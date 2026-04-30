package com.batchhawk.service;

import com.batchhawk.data.entity.product.Product;
import com.batchhawk.data.repository.ProductRepository;
import com.batchhawk.data.response.ProductResponse;
import com.batchhawk.data.specification.ProductSpec;
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
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(final UUID roasterId, final String name, final boolean activeOnly, final Pageable pageable) {
        final var spec = Stream.<Optional<Specification<Product>>>of(
            Optional.ofNullable(roasterId).map(ProductSpec::forRoaster),
            Optional.ofNullable(name).filter(Predicate.not(String::isBlank)).map(ProductSpec::nameContains),
            Optional.of(activeOnly).filter(Boolean::booleanValue).map(ignored -> ProductSpec.isActive(true))
        )
        .flatMap(Optional::stream)
        .reduce(Specification::and)
        .orElse(null);

        return productRepository.findAll(spec, pageable).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(final UUID uuid) {
        return productRepository.findByUuid(uuid)
            .map(ProductResponse::from)
            .orElseThrow(() -> new EntityNotFoundException("Product", uuid));
    }
}
