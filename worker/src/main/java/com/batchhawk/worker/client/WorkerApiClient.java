package com.batchhawk.worker.client;

import com.batchhawk.worker.config.WorkerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class WorkerApiClient {

    private final WebClient.Builder webClientBuilder;
    private final WorkerProperties props;

    private WebClient client() {
        return webClientBuilder
                .baseUrl(props.apiBaseUrl())
                .defaultHeader("X-Worker-Secret", props.apiSecret())
                .build();
    }

    public Optional<NextJobResponse> claimNextJob() {
        return client().get()
                .uri("/internal/worker/next-job")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, resp -> Mono.empty())
                .bodyToMono(NextJobResponse.class)
                .blockOptional();
    }

    public void completeRun(final String runId, final CompleteRunRequest request) {
        client().post()
                .uri("/internal/worker/runs/{runId}/complete", runId)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
