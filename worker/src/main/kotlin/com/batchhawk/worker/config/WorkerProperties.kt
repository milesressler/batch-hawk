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
    val scraping: ScrapingProperties = ScrapingProperties(),
) {
    data class ScrapingProperties(
        @field:Positive val maxTurns: Int = 30,
        @field:Positive val navigationTimeoutMs: Double = 15_000.0,
        @field:Positive val pageTextMaxChars: Int = 12_000,
        @field:Positive val maxLinks: Int = 50,
    )
}
