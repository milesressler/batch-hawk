package com.batchhawk.worker.client

import java.util.UUID

data class NextJobResponse(
    val runId: UUID,
    val roasterId: UUID,
    val websiteUrl: String?,
    val emailListUrl: String?,
    val urlHints: String?,
    val integrationType: String,
)
