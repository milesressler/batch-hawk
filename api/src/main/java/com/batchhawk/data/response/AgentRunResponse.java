package com.batchhawk.data.response;

import com.batchhawk.common.ScrapedField;
import com.batchhawk.data.entity.agent.AgentRun;
import com.batchhawk.data.enums.AgentRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

public record AgentRunResponse(
    @Schema(requiredMode = REQUIRED) Long id,
    @Schema(requiredMode = REQUIRED) UUID roasterId,
    @Schema(requiredMode = REQUIRED) String roasterName,
    @Schema(requiredMode = REQUIRED) AgentRunStatus status,
    Instant startedAt,
    Instant completedAt,
    Long inputTokens,
    Long outputTokens,
    String feedbackNotes,
    String checkoutNotes,
    List<ScrapedField> fieldsFound
) {
    public static AgentRunResponse from(final AgentRun r) {
        return new AgentRunResponse(
            r.getId(),
            r.getRoaster().getUuid(),
            r.getRoaster().getName(),
            r.getStatus(),
            r.getStartedAt(),
            r.getCompletedAt(),
            r.getInputTokens(),
            r.getOutputTokens(),
            r.getFeedbackNotes(),
            r.getCheckoutNotes(),
            r.getFieldsFound()
        );
    }
}
