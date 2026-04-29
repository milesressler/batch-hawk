package com.batchhawk.worker.client

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class WorkerApiClient(private val workerWebClient: WebClient) {

    fun claimNextJob(): NextJobResponse? = workerWebClient.post()
        .uri("/internal/worker/jobs")
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError) { Mono.empty() }
        .bodyToMono(NextJobResponse::class.java)
        .block()

    fun completeRun(runId: String, request: CompleteRunRequest) {
        workerWebClient.post()
            .uri("/internal/worker/runs/{runId}/complete", runId)
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block()
    }
}