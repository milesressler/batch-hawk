package com.batchhawk.common

data class CompleteRunRequest(
    val status: String,
    val fieldsAttempted: List<ScrapedField>,
    val fieldsFound: List<ScrapedField>,
    val feedbackNotes: String?,
    val checkoutNotes: String?,
)
