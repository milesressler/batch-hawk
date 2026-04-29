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
    @Schema(requiredMode = REQUIRED) boolean active,
    @Schema(requiredMode = REQUIRED) ModerationStatus moderationStatus,
    @Schema(requiredMode = REQUIRED) Instant createdAt,
    @Schema(requiredMode = REQUIRED) Instant updatedAt
) {
    public static RoasterResponse from(final Roaster r) {
        return new RoasterResponse(
            r.getId(),
            r.getName(),
            r.getWebsiteUrl(),
            r.getEmailListUrl(),
            r.isActive(),
            r.getModerationStatus(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}
