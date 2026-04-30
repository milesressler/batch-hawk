package com.batchhawk.worker.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(private val props: WorkerProperties) {

    @Bean
    fun reactiveClientRegistrationRepository(
        clientRegistrationRepository: InMemoryClientRegistrationRepository,
    ): ReactiveClientRegistrationRepository =
        InMemoryReactiveClientRegistrationRepository(clientRegistrationRepository.toList())

    @Bean
    fun reactiveAuthorizedClientManager(
        reactiveClientRegistrationRepository: ReactiveClientRegistrationRepository,
    ): ReactiveOAuth2AuthorizedClientManager {
        val service = InMemoryReactiveOAuth2AuthorizedClientService(reactiveClientRegistrationRepository)
        return AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
            reactiveClientRegistrationRepository, service,
        )
    }

    @Bean
    fun externalWebClient(builder: WebClient.Builder): WebClient =
        builder.codecs { it.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }.build()

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerKotlinModule()

    @Bean
    fun workerWebClient(
        builder: WebClient.Builder,
        objectMapper: ObjectMapper,
        reactiveAuthorizedClientManager: ReactiveOAuth2AuthorizedClientManager,
    ): WebClient {
        val oauth2 = ServerOAuth2AuthorizedClientExchangeFilterFunction(reactiveAuthorizedClientManager)
        oauth2.setDefaultClientRegistrationId("keycloak")
        return builder
            .baseUrl(props.apiBaseUrl)
            .filter(oauth2)
            .codecs { it.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper)) }
            .codecs { it.defaultCodecs().jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper)) }
            .build()
    }
}
