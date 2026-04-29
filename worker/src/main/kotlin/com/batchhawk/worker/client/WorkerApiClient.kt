package com.batchhawk.worker.client

import com.batchhawk.worker.config.WorkerProperties
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class WorkerApiClient(
    private val webClientBuilder: WebClient.Builder,
    private val props: WorkerProperties,
) {
    private fun client(): WebClient = webClientBuilder
        .baseUrl(props.apiBaseUrl)
        .defaultHeader("X-Worker-Secret", props.apiSecret)
        .build()

    fun claimNextJob(): NextJobResponse? = client().get()
        .uri("/internal/worker/next-job")
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError) { Mono.empty() }
        .bodyToMono(NextJobResponse::class.java)
        .block()

    fun completeRun(runId: String, request: CompleteRunRequest) {
        client().post()
            .uri("/internal/worker/runs/{runId}/complete", runId)
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block()
    }
}
