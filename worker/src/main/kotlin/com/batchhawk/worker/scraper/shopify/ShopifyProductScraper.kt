package com.batchhawk.worker.scraper.shopify

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.scraper.ProductCleanupService
import com.batchhawk.worker.scraper.RoasterScraper
import com.batchhawk.worker.scraper.playwright.PlaywrightAgentScraper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("SHOPIFY")
class ShopifyProductScraper(
    private val externalWebClient: WebClient,
    private val llmExtractor: ShopifyLlmExtractor,
    private val roasterInfoExtractor: RoasterInfoExtractor,
    private val playwrightScraper: PlaywrightAgentScraper,
    private val cleanupService: ProductCleanupService,
) : RoasterScraper {

    private val log = LoggerFactory.getLogger(ShopifyProductScraper::class.java)

    override fun scrape(job: NextJobResponse): CompleteRunRequest {
        val baseUrl = job.websiteUrl?.trimEnd('/')
            ?: return CompleteRunRequest("FAILED", emptyList(), "No websiteUrl on job")

        val response = try {
            externalWebClient.get()
                .uri("$baseUrl/products.json?limit=250")
                .retrieve()
                .bodyToMono(ShopifyProductsResponse::class.java)
                .block()
                ?: run {
                    log.warn("Empty response from {}/products.json — falling back to Playwright", baseUrl)
                    return playwrightScraper.scrape(job)
                }
        } catch (e: Exception) {
            log.warn("products.json unavailable for {} ({}) — falling back to Playwright", baseUrl, e.message)
            return playwrightScraper.scrape(job)
        }

        val slim = response.products
        log.info("Fetched {} raw products from {}, sending to LLM extractor", slim.size, baseUrl)

        val extracted = llmExtractor.extract(slim, baseUrl)
        log.info("LLM extracted {} coffee products from {}", extracted.size, baseUrl)

        val cleaned = cleanupService.clean(extracted)

//        val roasterUpdate = roasterInfoExtractor.extract(baseUrl)

        return CompleteRunRequest(
            status = "SUCCESS",
            products = cleaned,
            notes = "Extracted ${cleaned.size} coffee products from ${slim.size} raw Shopify products (${extracted.size} before cleanup)",
            roasterUpdate = null,
        )
    }
}
