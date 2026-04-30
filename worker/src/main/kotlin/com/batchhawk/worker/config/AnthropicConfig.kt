package com.batchhawk.worker.config

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AnthropicConfig(private val props: AnthropicProperties) {

    @Bean
    fun anthropicClient(): AnthropicClient = AnthropicOkHttpClient.builder()
        .apiKey(props.apiKey)
        .build()
}