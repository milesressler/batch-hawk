package com.batchhawk.worker.client;

import java.util.UUID;

public record NextJobResponse(
        UUID runId,
        UUID roasterId,
        String websiteUrl,
        String emailListUrl,
        String urlHints,
        String integrationType
) {}
