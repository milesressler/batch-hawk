package com.batchhawk.data.response;

import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.ModerationStatus;

import java.time.Instant;
import java.util.UUID;

public record RoasterResponse(
    UUID id,
    String name,
    String websiteUrl,
    String emailListUrl,
    boolean active,
    ModerationStatus moderationStatus,
    Instant createdAt,
    Instant updatedAt
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
