package com.batchhawk.data.response;

import com.batchhawk.data.entity.agent.AgentRun;
import com.batchhawk.data.entity.roaster.Roaster;
import com.batchhawk.data.enums.AgentRunStatus;
import com.batchhawk.data.enums.IntegrationType;
import com.batchhawk.data.enums.ModerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record AdminRoasterResponse(
    @Schema(requiredMode = REQUIRED) UUID id,
    @Schema(requiredMode = REQUIRED) String name,
    String websiteUrl,
    @Schema(requiredMode = REQUIRED) boolean active,
    @Schema(requiredMode = REQUIRED) ModerationStatus moderationStatus,
    @Schema(requiredMode = REQUIRED) IntegrationType integrationType,
    @Schema(requiredMode = REQUIRED) boolean pendingRefresh,
    Instant lastRunStartedAt,
    Instant lastRunCompletedAt,
    AgentRunStatus lastRunStatus
) {
    public static AdminRoasterResponse from(final Roaster r, final AgentRun lastRun) {
        return new AdminRoasterResponse(
            r.getUuid(),
            r.getName(),
            r.getWebsiteUrl(),
            r.isActive(),
            r.getModerationStatus(),
            r.getIntegrationType(),
            r.isPendingRefresh(),
            lastRun != null ? lastRun.getStartedAt() : null,
            lastRun != null ? lastRun.getCompletedAt() : null,
            lastRun != null ? lastRun.getStatus() : null
        );
    }
}