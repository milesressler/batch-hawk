package com.batchhawk.worker.scraper.shopify

import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.scraper.RoasterScraper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component("SHOPIFY")
class ShopifyProductScraper(
    private val externalWebClient: WebClient,
    private val llmExtractor: ShopifyLlmExtractor,
    private val roasterInfoExtractor: RoasterInfoExtractor,
) : RoasterScraper {

    private val log = LoggerFactory.getLogger(ShopifyProductScraper::class.java)

    override fun scrape(job: NextJobResponse): CompleteRunRequest {
        val baseUrl = job.websiteUrl?.trimEnd('/')
            ?: return CompleteRunRequest("FAILED", emptyList(), "No websiteUrl on job")

        val response = try {
            externalWebClient.get()
                .uri("$baseUrl/products.json?limit=3")
                .retrieve()
                .bodyToMono(ShopifyProductsResponse::class.java)
                .block()
                ?: return CompleteRunRequest("FAILED", emptyList(), "Empty response from $baseUrl/products.json")
        } catch (e: Exception) {
            log.warn("Failed to fetch products.json for {}: {}", baseUrl, e.message)
            return CompleteRunRequest("FAILED", emptyList(), "Could not fetch products.json: ${e.message}")
        }

        val slim = response.products
        log.info("Fetched {} raw products from {}, sending to LLM extractor", slim.size, baseUrl)

        val extracted = llmExtractor.extract(slim, baseUrl)
        log.info("LLM extracted {} coffee products from {}", extracted.size, baseUrl)

//        val roasterUpdate = roasterInfoExtractor.extract(baseUrl)

        return CompleteRunRequest(
            status = "SUCCESS",
            products = extracted,
            notes = "Extracted ${extracted.size} coffee products from ${slim.size} raw Shopify products",
            roasterUpdate = null,
        )
    }
}
