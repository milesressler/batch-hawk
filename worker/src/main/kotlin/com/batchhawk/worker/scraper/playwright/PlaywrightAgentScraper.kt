package com.batchhawk.worker.scraper.playwright

import com.anthropic.client.AnthropicClient
import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.config.WorkerProperties
import com.batchhawk.worker.scraper.RoasterScraper
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI

@Component("CUSTOM")
class PlaywrightAgentScraper(
    private val browserManager: BrowserManager,
    private val anthropicClient: AnthropicClient,
    private val workerProperties: WorkerProperties,
    private val objectMapper: ObjectMapper,
) : RoasterScraper {

    private val log = LoggerFactory.getLogger(PlaywrightAgentScraper::class.java)

    override fun scrape(job: NextJobResponse): CompleteRunRequest {
        val websiteUrl = job.websiteUrl?.trimEnd('/')
            ?: return CompleteRunRequest("FAILED", emptyList(), "No websiteUrl on job")

        val allowedDomain = runCatching { URI(websiteUrl).host }.getOrElse { websiteUrl }
        log.info("Starting playwright scrape for runId={} url={}", job.runId, websiteUrl)

        return browserManager.withContext { context ->
            val page = context.newPage()
            val browserTools = BrowserTools(
                page = page,
                allowedDomain = allowedDomain,
                config = workerProperties.scraping,
                objectMapper = objectMapper,
            )
            ScraperAgentSession(
                anthropicClient = anthropicClient,
                browserTools = browserTools,
                config = workerProperties.scraping,
                job = job,
            ).run()
        }
    }
}
