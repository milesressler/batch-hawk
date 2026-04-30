package com.batchhawk.service;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.repository.RoasterRepository;
import com.batchhawk.data.response.RoasterResponse;
import com.batchhawk.data.specification.RoasterSpec;
import com.batchhawk.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RoasterService {

    private final RoasterRepository roasterRepository;

    @Transactional(readOnly = true)
    public Page<RoasterResponse> list(final String name, final boolean activeOnly, final Pageable pageable) {
        final var spec = Stream.<Optional<Specification<Roaster>>>of(
            Optional.ofNullable(name).filter(Predicate.not(String::isBlank)).map(RoasterSpec::nameContains),
            Optional.of(activeOnly).filter(Boolean::booleanValue).map(ignored -> RoasterSpec.isActive(true))
        )
        .flatMap(Optional::stream)
        .reduce(Specification::and)
        .orElse(null);

        return roasterRepository.findAll(spec, pageable).map(RoasterResponse::from);
    }

    @Transactional(readOnly = true)
    public RoasterResponse getById(final UUID uuid) {
        return roasterRepository.findByUuid(uuid)
            .map(RoasterResponse::from)
            .orElseThrow(() -> new EntityNotFoundException("Roaster", uuid));
    }
}
