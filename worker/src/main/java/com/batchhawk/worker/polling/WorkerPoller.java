package com.batchhawk.worker.polling;

import com.batchhawk.worker.client.CompleteRunRequest;
import com.batchhawk.worker.client.NextJobResponse;
import com.batchhawk.worker.client.WorkerApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkerPoller {

    private final WorkerApiClient apiClient;

    @Scheduled(fixedDelayString = "${batchhawk.worker.poll-interval-minutes}",
               timeUnit = TimeUnit.MINUTES)
    public void poll() {
        final Optional<NextJobResponse> job = apiClient.claimNextJob();
        if (job.isEmpty()) {
            log.debug("No roasters due for refresh — sleeping until next poll");
            return;
        }

        final NextJobResponse next = job.get();
        log.info("Claimed job runId={} roasterId={} integrationType={}",
                next.runId(), next.roasterId(), next.integrationType());

        try {
            final CompleteRunRequest result = scrape(next);
            apiClient.completeRun(next.runId().toString(), result);
            log.info("Completed runId={} status={}", next.runId(), result.status());
        } catch (final Exception e) {
            log.error("Job failed runId={}", next.runId(), e);
            apiClient.completeRun(next.runId().toString(), new CompleteRunRequest(
                    "FAILED", List.of(), List.of(), e.getMessage(), null));
        }
    }

    private CompleteRunRequest scrape(final NextJobResponse job) {
        // TODO: implement AI scraping agent
        // Use job.integrationType() to select the appropriate scraping strategy:
        //   SHOPIFY  → /products.json endpoint
        //   WOO_COMMERCE → REST API
        //   SQUARE / CUSTOM / UNKNOWN → general browser/AI agent
        throw new UnsupportedOperationException("Scraping not yet implemented");
    }
}
