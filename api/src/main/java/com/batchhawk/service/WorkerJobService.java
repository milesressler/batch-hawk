package com.batchhawk.service;

import com.batchhawk.common.CompleteRunRequest;
import com.batchhawk.common.NextJobResponse;
import com.batchhawk.common.ProductUpdateRequest;
import com.batchhawk.common.RoasterUpdateRequest;
import com.batchhawk.common.VariantInfo;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

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

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    @Transactional
    public void scheduleJobs() {
        final var cutoff = Instant.now().minus(refreshIntervalHours, ChronoUnit.HOURS);
        final var due = roasterRepository.findAllDueForScheduling(cutoff);
        if (due.isEmpty()) return;

        log.info("Enqueueing {} roaster(s) for refresh", due.size());
        due.forEach(roaster -> {
            final var run = new AgentRun();
            run.setRoaster(roaster);
            run.setStatus(AgentRunStatus.PENDING);
            agentRunRepository.save(run);
            if (roaster.isPendingRefresh()) {
                roaster.setPendingRefresh(false);
                roasterRepository.save(roaster);
            }
        });
    }

    @Transactional
    public Optional<NextJobResponse> claimNextJob() {
        return agentRunRepository.findFirstByStatusOrderByCreatedAtAsc(AgentRunStatus.PENDING)
                .map(run -> {
                    run.setStatus(AgentRunStatus.IN_PROGRESS);
                    run.setStartedAt(Instant.now());
                    agentRunRepository.save(run);
                    final var roaster = run.getRoaster();
                    return new NextJobResponse(
                            run.getId(),
                            roaster.getId(),
                            roaster.getWebsiteUrl(),
                            roaster.getEmailListUrl(),
                            roaster.getUrlHints(),
                            roaster.getIntegrationType().name()
                    );
                });
    }

    @Transactional
    public void completeRun(final Long runId, final CompleteRunRequest request) {
        final var run = agentRunRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("AgentRun", runId));

        final var now = Instant.now();
        run.setStatus(AgentRunStatus.valueOf(request.getStatus()));
        run.setCompletedAt(now);
        run.setFeedbackNotes(request.getNotes());
        Optional.ofNullable(request.getInputTokens()).ifPresent(run::setInputTokens);
        Optional.ofNullable(request.getOutputTokens()).ifPresent(run::setOutputTokens);
        agentRunRepository.save(run);

        Optional.ofNullable(request.getSiteHints()).ifPresent(hints -> {
            run.getRoaster().setUrlHints(hints);
            roasterRepository.save(run.getRoaster());
        });
        Optional.ofNullable(request.getRoasterUpdate())
                .ifPresent(update -> applyRoasterUpdate(run.getRoaster(), update));

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

    private void applyRoasterUpdate(final Roaster roaster, final RoasterUpdateRequest update) {
        Optional.ofNullable(update.getCity()).ifPresent(roaster::setCity);
        Optional.ofNullable(update.getState()).ifPresent(roaster::setState);
        Optional.ofNullable(update.getLogoUrl()).ifPresent(roaster::setLogoUrl);
        roasterRepository.save(roaster);
    }

    private void upsertProduct(final AgentRun run, final ProductUpdateRequest update, final Instant now) {
        if (update.getName() == null) return;

        final var product = Optional.ofNullable(update.getExternalProductId())
                .flatMap(extId -> productRepository.findByRoasterIdAndExternalProductId(run.getRoaster().getId(), extId))
                .or(() -> productRepository.findByRoasterIdAndNameIgnoreCase(run.getRoaster().getId(), update.getName()))
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
        Optional.ofNullable(update.getProductUrl()).ifPresent(product::setProductUrl);
        Optional.ofNullable(update.getExternalProductId()).ifPresent(product::setExternalProductId);
        Optional.ofNullable(update.getOffersGrinding()).ifPresent(product::setOffersGrinding);

        final var saved = productRepository.save(product);

        final var variants = update.getVariants();
        if (variants != null && !variants.isEmpty()) {
            variants.forEach(variant -> writeObservationFromVariant(run, saved, variant, now));
        } else if (update.getPriceInCents() != null || update.getInStock() != null || update.getBagSize() != null) {
            writeObservation(run, saved, update, now);
        }
    }

    private void writeObservation(final AgentRun run, final Product product,
                                   final ProductUpdateRequest update, final Instant now) {
        final var obs = new ProductObservation();
        obs.setProduct(product);
        obs.setObservedAt(now);
        obs.setAgentRun(run);

        Optional.ofNullable(update.getPriceInCents())
                .map(cents -> BigDecimal.valueOf(cents).movePointLeft(2))
                .ifPresent(obs::setPriceUsd);

        Optional.ofNullable(update.getBagSize()).ifPresent(size -> {
            obs.setBagSize(size);
            obs.setBagSizeUnit(update.getBagUnit());
            final var bagSizeOz = toOz(size, update.getBagUnit());
            obs.setBagSizeOz(bagSizeOz);
            if (obs.getPriceUsd() != null && bagSizeOz.compareTo(BigDecimal.ZERO) > 0) {
                obs.setPricePerOz(obs.getPriceUsd().divide(bagSizeOz, 4, RoundingMode.HALF_UP));
            }
        });

        Optional.ofNullable(update.getInStock()).ifPresent(obs::setInStock);
        productObservationRepository.save(obs);
    }

    private void writeObservationFromVariant(final AgentRun run, final Product product,
                                              final VariantInfo variant, final Instant now) {
        if (variant.getPriceInCents() == null && variant.getInStock() == null && variant.getBagSize() == null) return;

        final var obs = new ProductObservation();
        obs.setProduct(product);
        obs.setObservedAt(now);
        obs.setAgentRun(run);

        Optional.ofNullable(variant.getPriceInCents())
                .map(cents -> BigDecimal.valueOf(cents).movePointLeft(2))
                .ifPresent(obs::setPriceUsd);

        Optional.ofNullable(variant.getBagSize()).ifPresent(size -> {
            obs.setBagSize(size);
            obs.setBagSizeUnit(variant.getBagUnit());
            final var bagSizeOz = toOz(size, variant.getBagUnit());
            obs.setBagSizeOz(bagSizeOz);
            if (obs.getPriceUsd() != null && bagSizeOz.compareTo(BigDecimal.ZERO) > 0) {
                obs.setPricePerOz(obs.getPriceUsd().divide(bagSizeOz, 4, RoundingMode.HALF_UP));
            }
        });

        Optional.ofNullable(variant.getInStock()).ifPresent(obs::setInStock);
        productObservationRepository.save(obs);
    }

    private static BigDecimal toOz(final int size, final String unit) {
        if ("g".equalsIgnoreCase(unit) || "grams".equalsIgnoreCase(unit)) {
            return BigDecimal.valueOf(size).divide(BigDecimal.valueOf(28.3495), 4, RoundingMode.HALF_UP);
        }
        if ("lb".equalsIgnoreCase(unit) || "lbs".equalsIgnoreCase(unit) || "pounds".equalsIgnoreCase(unit)) {
            return BigDecimal.valueOf(size).multiply(BigDecimal.valueOf(16));
        }
        if ("kg".equalsIgnoreCase(unit) || "kilograms".equalsIgnoreCase(unit)) {
            return BigDecimal.valueOf(size).multiply(BigDecimal.valueOf(35.274));
        }
        return BigDecimal.valueOf(size);
    }
}
