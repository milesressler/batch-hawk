package com.batchhawk.service;

import com.batchhawk.common.CompleteRunRequest;
import com.batchhawk.common.NextJobResponse;
import com.batchhawk.common.ProductUpdateRequest;
import com.batchhawk.data.entity.agent.AgentRun;
import com.batchhawk.data.entity.observation.ProductObservation;
import com.batchhawk.data.entity.product.Product;
import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.AgentRunStatus;
import com.batchhawk.data.repository.AgentRunRepository;
import com.batchhawk.data.repository.ProductObservationRepository;
import com.batchhawk.data.repository.ProductRepository;
import com.batchhawk.data.repository.RoasterRepository;
import com.batchhawk.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkerJobService {

    private static final Logger log = LoggerFactory.getLogger(WorkerJobService.class);

    private final RoasterRepository roasterRepository;
    private final AgentRunRepository agentRunRepository;
    private final ProductRepository productRepository;
    private final ProductObservationRepository productObservationRepository;

    @Value("${batchhawk.refresh-interval-hours:24}")
    private int refreshIntervalHours;

    @Value("${batchhawk.max-run-minutes:10}")
    private int maxRunMinutes;

    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public Optional<NextJobResponse> claimNextJob() {
        try {
            final var cutoff = Instant.now().minus(refreshIntervalHours, ChronoUnit.HOURS);
            return roasterRepository.findNextDueForRefresh(cutoff).map(this::createRunForRoaster);
        } catch (DataIntegrityViolationException e) {
            // Another worker claimed this roaster concurrently — nothing available
            return Optional.empty();
        }
    }

    @Transactional
    public void completeRun(final UUID runId, final CompleteRunRequest request) {
        final var run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("AgentRun", runId));

        final var now = Instant.now();
        run.setStatus(AgentRunStatus.valueOf(request.getStatus()));
        run.setCompletedAt(now);
        run.setFeedbackNotes(request.getNotes());
        agentRunRepository.save(run);

        request.getProducts().forEach(product -> upsertProduct(run, product, now));
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void expireStaleRuns() {
        final var cutoff = Instant.now().minus(maxRunMinutes, ChronoUnit.MINUTES);
        final var stale = agentRunRepository.findByStatusAndStartedAtBefore(AgentRunStatus.IN_PROGRESS, cutoff);
        if (stale.isEmpty()) return;

        log.warn("Expiring {} stale agent run(s) older than {} minutes", stale.size(), maxRunMinutes);
        stale.forEach(run -> {
            run.setStatus(AgentRunStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run.setFeedbackNotes("Expired: exceeded max run time of " + maxRunMinutes + " minutes");
        });
        agentRunRepository.saveAll(stale);
    }

    private NextJobResponse createRunForRoaster(final Roaster roaster) {
        final var run = new AgentRun();
        run.setRoaster(roaster);
        run.setStartedAt(Instant.now());
        run.setStatus(AgentRunStatus.IN_PROGRESS);
        final var saved = agentRunRepository.save(run);
        return new NextJobResponse(
                saved.getId(),
                roaster.getId(),
                roaster.getWebsiteUrl(),
                roaster.getEmailListUrl(),
                roaster.getUrlHints(),
                roaster.getIntegrationType().name()
        );
    }

    private void upsertProduct(final AgentRun run, final ProductUpdateRequest update, final Instant now) {
        if (update.getName() == null) return;

        final var product = productRepository
                .findByRoasterIdAndNameIgnoreCase(run.getRoaster().getId(), update.getName())
                .orElseGet(() -> {
                    final var p = new Product();
                    p.setRoaster(run.getRoaster());
                    p.setName(update.getName());
                    return p;
                });

        Optional.ofNullable(update.getRoastLevel()).ifPresent(product::setRoastLevel);
        Optional.ofNullable(update.getProductType()).ifPresent(product::setProductType);
        Optional.ofNullable(update.getOriginCountry()).ifPresent(product::setOriginCountry);
        Optional.ofNullable(update.getOriginRegion()).ifPresent(product::setOriginRegion);
        Optional.ofNullable(update.getProcess()).ifPresent(product::setProcess);
        Optional.ofNullable(update.getBrewMethods()).ifPresent(product::setBrewMethods);
        Optional.ofNullable(update.getFlavorProfile()).ifPresent(product::setFlavorProfile);
        Optional.ofNullable(update.isDecaf()).ifPresent(product::setDecaf);
        Optional.ofNullable(update.getAvailabilityType()).ifPresent(product::setAvailabilityType);
        Optional.ofNullable(update.getDescription()).ifPresent(product::setDescription);

        final var saved = productRepository.save(product);

        if (update.getPriceInCents() != null || update.getInStock() != null || update.getBagSize() != null) {
            writeObservation(run, saved, update, now);
        }
    }

    private void writeObservation(final AgentRun run, final Product product,
                                   final ProductUpdateRequest update, final Instant now) {
        final var obs = new ProductObservation();
        obs.setProduct(product);
        obs.setObservedAt(now);
        obs.setAgentRunId(run.getId());

        Optional.ofNullable(update.getPriceInCents())
                .map(cents -> BigDecimal.valueOf(cents).movePointLeft(2))
                .ifPresent(obs::setPriceUsd);

        Optional.ofNullable(update.getBagSize()).ifPresent(size -> {
            final var bagSizeOz = toOz(size, update.getBagUnit());
            obs.setBagSizeOz(bagSizeOz);
            if (obs.getPriceUsd() != null && bagSizeOz.compareTo(BigDecimal.ZERO) > 0) {
                obs.setPricePerOz(obs.getPriceUsd().divide(bagSizeOz, 4, RoundingMode.HALF_UP));
            }
        });

        Optional.ofNullable(update.getInStock()).ifPresent(obs::setInStock);
        productObservationRepository.save(obs);
    }

    private static BigDecimal toOz(final int size, final String unit) {
        if ("g".equalsIgnoreCase(unit) || "grams".equalsIgnoreCase(unit)) {
            return BigDecimal.valueOf(size).divide(BigDecimal.valueOf(28.3495), 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(size);
    }
}
