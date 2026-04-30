package com.batchhawk.worker.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "anthropic")
class AnthropicProperties {
    var apiKey: String = ""
}