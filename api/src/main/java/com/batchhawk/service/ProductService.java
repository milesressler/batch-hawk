package com.batchhawk.service;

import com.batchhawk.data.entity.observation.ProductObservation;
import com.batchhawk.data.entity.product.Product;
import com.batchhawk.data.repository.ProductObservationRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductObservationRepository productObservationRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(
        final UUID roasterId,
        final String keyword,
        final boolean activeOnly,
        final List<String> roastLevel,
        final List<String> process,
        final List<String> productType,
        final List<String> availabilityType,
        final boolean decafOnly,
        final Pageable pageable
    ) {
        final var spec = Stream.<Optional<Specification<Product>>>of(
            Optional.ofNullable(roasterId).map(ProductSpec::forRoaster),
            Optional.ofNullable(keyword).filter(Predicate.not(String::isBlank)).map(ProductSpec::keywordSearch),
            Optional.of(activeOnly).filter(Boolean::booleanValue).map(ignored -> ProductSpec.isActive(true)),
            Optional.ofNullable(roastLevel).filter(Predicate.not(List::isEmpty)).map(ProductSpec::roastLevelIn),
            Optional.ofNullable(process).filter(Predicate.not(List::isEmpty)).map(ProductSpec::processIn),
            Optional.ofNullable(productType).filter(Predicate.not(List::isEmpty)).map(ProductSpec::productTypeIn),
            Optional.ofNullable(availabilityType).filter(Predicate.not(List::isEmpty)).map(ProductSpec::availabilityTypeIn),
            Optional.of(decafOnly).filter(Boolean::booleanValue).map(ignored -> ProductSpec.isDecaf())
        )
        .flatMap(Optional::stream)
        .reduce(Specification::and)
        .orElse(null);

        final var page = productRepository.findAll(spec, pageable);
        final List<Long> productIds = page.getContent().stream().map(Product::getId).toList();
        final Map<Long, List<ProductObservation>> variantMap = productObservationRepository
            .findAllLatestVariantsByProductIds(productIds)
            .stream()
            .collect(Collectors.groupingBy(obs -> obs.getProduct().getId()));

        return page.map(p -> ProductResponse.from(p, variantMap.getOrDefault(p.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(final UUID uuid) {
        final var product = productRepository.findByUuid(uuid)
            .orElseThrow(() -> new EntityNotFoundException("Product", uuid));
        final var variants = productObservationRepository.findLatestVariantsByProductId(product.getId());
        return ProductResponse.from(product, variants);
    }
}
