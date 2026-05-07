package com.batchhawk.data.response;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.ModerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record RoasterResponse(
    @Schema(requiredMode = REQUIRED) UUID id,
    @Schema(requiredMode = REQUIRED) String name,
    String websiteUrl,
    String emailListUrl,
    String city,
    String state,
    String logoUrl,
    @Schema(requiredMode = REQUIRED) boolean active,
    @Schema(requiredMode = REQUIRED) ModerationStatus moderationStatus,
    @Schema(requiredMode = REQUIRED) Instant createdAt,
    @Schema(requiredMode = REQUIRED) Instant updatedAt
) {
    public static RoasterResponse from(final Roaster r) {
        return new RoasterResponse(
            r.getUuid(),
            r.getName(),
            r.getWebsiteUrl(),
            r.getEmailListUrl(),
            r.getCity(),
            r.getState(),
            r.getLogoUrl(),
            r.isActive(),
            r.getModerationStatus(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}
