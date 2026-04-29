package com.batchhawk.worker.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(private val props: WorkerProperties) {

    @Bean
    fun authorizedClientManager(
        clientRegistrationRepository: ClientRegistrationRepository,
        authorizedClientService: OAuth2AuthorizedClientService,
    ): OAuth2AuthorizedClientManager {
        val provider = OAuth2AuthorizedClientProviderBuilder.builder()
            .clientCredentials()
            .build()
        return AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientService,
        ).also { it.setAuthorizedClientProvider(provider) }
    }

    @Bean
    fun workerWebClient(
        builder: WebClient.Builder,
        authorizedClientManager: OAuth2AuthorizedClientManager,
    ): WebClient {
        val oauth2 = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
        oauth2.setDefaultClientRegistrationId("keycloak")
        return builder
            .baseUrl(props.apiBaseUrl)
            .apply(oauth2.oauth2Configuration())
            .build()
    }
}