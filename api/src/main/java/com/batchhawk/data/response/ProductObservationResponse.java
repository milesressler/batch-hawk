package com.batchhawk.data.response;

import com.batchhawk.data.entity.observation.ProductObservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record ProductObservationResponse(
    @Schema(requiredMode = REQUIRED) UUID id,
    Integer bagSize,
    String bagSizeUnit,
    BigDecimal bagSizeOz,
    BigDecimal priceUsd,
    BigDecimal pricePerOz,
    Boolean inStock,
    @Schema(requiredMode = REQUIRED) Instant observedAt
) {
    public static ProductObservationResponse from(final ProductObservation obs) {
        return new ProductObservationResponse(
            obs.getUuid(),
            obs.getBagSize(),
            obs.getBagSizeUnit(),
            obs.getBagSizeOz(),
            obs.getPriceUsd(),
            obs.getPricePerOz(),
            obs.getInStock(),
            obs.getObservedAt()
        );
    }
}
