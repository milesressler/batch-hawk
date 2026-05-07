package com.batchhawk.service;

import com.batchhawk.data.repository.AgentRunRepository;
import com.batchhawk.data.repository.ProductRepository;
import com.batchhawk.data.repository.RoasterRepository;
import com.batchhawk.data.response.AdminRoasterResponse;
import com.batchhawk.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public int deactivateProducts(final UUID roasterId) {
        final var roaster = roasterRepository.findByUuid(roasterId)
            .orElseThrow(() -> new EntityNotFoundException("Roaster", roasterId));
        return productRepository.deactivateAllByRoasterId(roaster.getId());
    }
}