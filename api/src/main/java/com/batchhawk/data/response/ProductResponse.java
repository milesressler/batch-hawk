package com.batchhawk.data.response;

import com.batchhawk.data.entity.product.Product;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID roasterId,
    String roasterName,
    String name,
    String roastLevel,
    String productType,
    String originCountry,
    String originRegion,
    String process,
    List<String> brewMethods,
    List<String> flavorProfile,
    boolean decaf,
    String availabilityType,
    String description,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProductResponse from(final Product p) {
        return new ProductResponse(
            p.getId(),
            p.getRoaster().getId(),
            p.getRoaster().getName(),
            p.getName(),
            p.getRoastLevel(),
            p.getProductType(),
            p.getOriginCountry(),
            p.getOriginRegion(),
            p.getProcess(),
            p.getBrewMethods(),
            p.getFlavorProfile(),
            p.isDecaf(),
            p.getAvailabilityType(),
            p.getDescription(),
            p.isActive(),
            p.getCreatedAt(),
            p.getUpdatedAt()
        );
    }
}
