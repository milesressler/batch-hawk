package com.batchhawk.data.request;

import com.batchhawk.data.enums.IntegrationType;
import com.batchhawk.data.enums.ModerationStatus;

public record AdminRoasterRequest(
    String name,
    String websiteUrl,
    String emailListUrl,
    IntegrationType integrationType,
    ModerationStatus moderationStatus,
    Boolean active
) {}
