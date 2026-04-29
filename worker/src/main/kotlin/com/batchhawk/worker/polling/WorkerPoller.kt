package com.batchhawk.worker.polling

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.client.WorkerApiClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WorkerPoller(private val apiClient: WorkerApiClient) {

    private val log = LoggerFactory.getLogger(WorkerPoller::class.java)

    @Scheduled(
        fixedDelayString = "\${batchhawk.worker.poll-interval-minutes}",
        timeUnit = TimeUnit.MINUTES,
    )
    fun poll() {
        val job = apiClient.claimNextJob() ?: run {
            log.debug("No roasters due for refresh — sleeping until next poll")
            return
        }

        log.info("Claimed job runId={} roasterId={} integrationType={}", job.runId, job.roasterId, job.integrationType)

        try {
            val result = scrape(job)
            apiClient.completeRun(job.runId.toString(), result)
            log.info("Completed runId={} status={}", job.runId, result.status)
        } catch (e: Exception) {
            log.error("Job failed runId={}", job.runId, e)
            apiClient.completeRun(
                job.runId.toString(),
                CompleteRunRequest("FAILED", emptyList(), emptyList(), e.message, null),
            )
        }
    }

    private fun scrape(job: NextJobResponse): CompleteRunRequest {
        // TODO: implement AI scraping agent
        // Use job.integrationType to select the appropriate strategy:
        //   SHOPIFY      → /products.json endpoint
        //   WOO_COMMERCE → REST API
        //   SQUARE / CUSTOM / UNKNOWN → general browser/AI agent
        throw UnsupportedOperationException("Scraping not yet implemented")
    }
}
