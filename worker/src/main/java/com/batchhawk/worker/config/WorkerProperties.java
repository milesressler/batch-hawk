package com.batchhawk.worker.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "batchhawk.worker")
public record WorkerProperties(
        @NotBlank String apiBaseUrl,
        @NotBlank String apiSecret,
        @Positive int pollIntervalMinutes,
        @Positive int refreshIntervalHours,
        @Positive int maxRunMinutes
) {}
