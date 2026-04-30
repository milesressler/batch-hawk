package com.batchhawk.worker.polling

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.client.WorkerApiClient
import com.batchhawk.worker.scraper.RoasterScraper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class WorkerPoller(
    private val apiClient: WorkerApiClient,
    private val scrapers: Map<String, RoasterScraper>,
) {

    private val log = LoggerFactory.getLogger(WorkerPoller::class.java)

    @Scheduled(
        fixedDelayString = "\${batchhawk.worker.poll-interval-seconds}",
        timeUnit = TimeUnit.SECONDS,
    )
    fun poll() {
        val job = apiClient.claimNextJob() ?: run {
            log.debug("No roasters due for refresh — sleeping until next poll")
            return
        }

        log.info("Claimed job runId={} roasterId={} integrationType={}", job.runId, job.roasterId, job.integrationType)

        val result = runCatching { dispatch(job) }
            .getOrElse { e ->
                log.error("Job failed runId={}", job.runId, e)
                CompleteRunRequest("FAILED", emptyList(), e.message)
            }

        apiClient.completeRun(job.runId.toString(), result)
        log.info("Completed runId={} status={}", job.runId, result.status)
    }

    private fun dispatch(job: NextJobResponse): CompleteRunRequest {
        val scraper = scrapers[job.integrationType]
            ?: return CompleteRunRequest(
                "FAILED",
                emptyList(),
                "No scraper registered for integrationType=${job.integrationType}",
            )
        return scraper.scrape(job)
    }
}
