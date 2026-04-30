package com.batchhawk.worker.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "batchhawk.worker")
data class WorkerProperties(
    @field:NotBlank val apiBaseUrl: String,
    @field:Positive val pollIntervalSeconds: Int,
    @field:Positive val refreshIntervalHours: Int,
    @field:Positive val maxRunMinutes: Int,
)
