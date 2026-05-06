package com.batchhawk.common

data class CompleteRunRequest(
    val status: String,
    val products: List<ProductUpdateRequest>,
    val notes: String?,
    val roasterUpdate: RoasterUpdateRequest? = null,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val siteHints: String? = null,
)
