package com.batchhawk.common

data class NextJobResponse(
    val runId: Long,
    val roasterId: Long,
    val websiteUrl: String?,
    val emailListUrl: String?,
    val urlHints: String?,
    val integrationType: String,
)
