package com.batchhawk.worker.client

data class CompleteRunRequest(
    val status: String,
    val fieldsAttempted: List<String>,
    val fieldsFound: List<String>,
    val feedbackNotes: String?,
    val checkoutNotes: String?,
)
