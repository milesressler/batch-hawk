package com.batchhawk.data.response;

import com.batchhawk.data.entity.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ProductResponse(
    @Schema(requiredMode = REQUIRED) UUID id,
    @Schema(requiredMode = REQUIRED) RoasterResponse roaster,
    @Schema(requiredMode = REQUIRED) String name,
    String roastLevel,
    String productType,
    String originCountry,
    String originRegion,
    String process,
    List<String> brewMethods,
    List<String> flavorProfile,
    @Schema(requiredMode = REQUIRED) boolean decaf,
    String availabilityType,
    String description,
    @Schema(requiredMode = REQUIRED) boolean active,
    @Schema(requiredMode = REQUIRED) Instant createdAt,
    @Schema(requiredMode = REQUIRED) Instant updatedAt
) {
    public static ProductResponse from(final Product p) {
        return new ProductResponse(
            p.getUuid(),
            RoasterResponse.from(p.getRoaster()),
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
