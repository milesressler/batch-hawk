package com.batchhawk.service;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.repository.AgentRunRepository;
import com.batchhawk.data.repository.ProductRepository;
import com.batchhawk.data.repository.RoasterRepository;
import com.batchhawk.data.request.AdminRoasterRequest;
import com.batchhawk.data.response.AdminRoasterResponse;
import com.batchhawk.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRoasterService {

    private final RoasterRepository roasterRepository;
    private final AgentRunRepository agentRunRepository;
    private final ProductRepository productRepository;

    public List<AdminRoasterResponse> listAll() {
        return roasterRepository.findAll(Sort.by("name")).stream()
            .map(r -> AdminRoasterResponse.from(r, agentRunRepository.findLatestCompletedOrActiveRun(r).orElse(null)))
            .toList();
    }

    @Transactional
    public void triggerRefresh(final UUID roasterId) {
        final var roaster = roasterRepository.findByUuid(roasterId)
            .orElseThrow(() -> new EntityNotFoundException("Roaster", roasterId));
        roaster.setPendingRefresh(true);
        roasterRepository.save(roaster);
    }

    @Transactional
    public AdminRoasterResponse create(final AdminRoasterRequest request) {
        final var roaster = new Roaster();
        applyRequest(roaster, request);
        final var saved = roasterRepository.save(roaster);
        return AdminRoasterResponse.from(saved, null);
    }

    @Transactional
    public AdminRoasterResponse update(final UUID roasterId, final AdminRoasterRequest request) {
        final var roaster = roasterRepository.findByUuid(roasterId)
            .orElseThrow(() -> new EntityNotFoundException("Roaster", roasterId));
        applyRequest(roaster, request);
        final var saved = roasterRepository.save(roaster);
        return AdminRoasterResponse.from(saved, agentRunRepository.findLatestCompletedOrActiveRun(saved).orElse(null));
    }

    @Transactional
    public int deactivateProducts(final UUID roasterId) {
        final var roaster = roasterRepository.findByUuid(roasterId)
            .orElseThrow(() -> new EntityNotFoundException("Roaster", roasterId));
        return productRepository.deactivateAllByRoasterId(roaster.getId());
    }

    private void applyRequest(final Roaster roaster, final AdminRoasterRequest request) {
        Optional.ofNullable(request.name()).ifPresent(roaster::setName);
        Optional.ofNullable(request.websiteUrl()).ifPresent(roaster::setWebsiteUrl);
        Optional.ofNullable(request.emailListUrl()).ifPresent(roaster::setEmailListUrl);
        Optional.ofNullable(request.integrationType()).ifPresent(roaster::setIntegrationType);
        Optional.ofNullable(request.moderationStatus()).ifPresent(roaster::setModerationStatus);
        Optional.ofNullable(request.active()).ifPresent(roaster::setActive);
    }
}