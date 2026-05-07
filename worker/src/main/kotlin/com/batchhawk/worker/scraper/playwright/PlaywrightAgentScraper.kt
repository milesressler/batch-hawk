package com.batchhawk.worker.scraper.playwright

import com.anthropic.client.AnthropicClient
import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.config.WorkerProperties
import com.batchhawk.worker.scraper.ProductCleanupService
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
    private val cleanupService: ProductCleanupService,
) : RoasterScraper {

    private val log = LoggerFactory.getLogger(PlaywrightAgentScraper::class.java)

    override fun scrape(job: NextJobResponse): CompleteRunRequest {
        val websiteUrl = job.websiteUrl?.trimEnd('/')
            ?: return CompleteRunRequest("FAILED", emptyList(), "No websiteUrl on job")

        val allowedDomain = runCatching { URI(websiteUrl).host }.getOrElse { websiteUrl }
        log.info("Starting playwright scrape for runId={} url={}", job.runId, websiteUrl)

        return browserManager.withContext { context ->
            // Stage 1: discover product URLs
            val discoveryPage = context.newPage()
            val discoveryTools = BrowserTools(discoveryPage, allowedDomain, workerProperties.scraping, objectMapper, job.integrationType)
            val discoverySession = DiscoverySession(anthropicClient, discoveryTools, workerProperties.scraping, job)
            val discovery = discoverySession.run()
            discoveryPage.close()

            log.info("Discovery complete: {} products, siteHints={}", discovery.products.size, discovery.siteHintsJson != null)

            if (discovery.products.isEmpty()) {
                return@withContext CompleteRunRequest(
                    status = "FAILED",
                    products = emptyList(),
                    notes = "Discovery found no product URLs",
                    siteHints = discovery.siteHintsJson,
                    inputTokens = discoverySession.inputTokens,
                    outputTokens = discoverySession.outputTokens,
                )
            }

            // Stage 2: extract details for each product
            var totalInputTokens = discoverySession.inputTokens
            var totalOutputTokens = discoverySession.outputTokens

            val products = discovery.products.mapNotNull { discovered ->
                val detailPage = context.newPage()
                val detailTools = BrowserTools(detailPage, allowedDomain, workerProperties.scraping, objectMapper, job.integrationType)
                val detailSession = ProductDetailSession(anthropicClient, detailTools, workerProperties.scraping, objectMapper, job.integrationType)
                val result = runCatching { detailSession.extract(discovered) }.getOrElse { e ->
                    log.warn("Detail extraction failed for {}: {}", discovered.url, e.message)
                    null
                }
                detailPage.close()
                totalInputTokens += detailSession.inputTokens
                totalOutputTokens += detailSession.outputTokens
                result
            }

            log.info("Extraction complete: {}/{} products succeeded", products.size, discovery.products.size)

            val cleanedProducts = cleanupService.clean(products)

            CompleteRunRequest(
                status = "SUCCESS",
                products = cleanedProducts,
                notes = "Scraped ${cleanedProducts.size} products (${discovery.products.size} discovered, ${products.size} extracted)",
                siteHints = discovery.siteHintsJson,
                inputTokens = totalInputTokens,
                outputTokens = totalOutputTokens,
            )
        }
    }
}
