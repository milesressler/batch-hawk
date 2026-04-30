package com.batchhawk.worker.scraper.shopify

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.batchhawk.common.ProductUpdateRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ShopifyLlmExtractor(
    private val anthropicClient: AnthropicClient,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(ShopifyLlmExtractor::class.java)

    fun extract(products: List<ShopifySlimProduct>, baseUrl: String): List<ProductUpdateRequest> {
        if (products.isEmpty()) return emptyList()

        return products.chunked(BATCH_SIZE).flatMapIndexed { batchIndex, batch ->
            extractBatch(batch, baseUrl, batchIndex)
        }
    }

    private fun extractBatch(
        batch: List<ShopifySlimProduct>,
        baseUrl: String,
        batchIndex: Int,
    ): List<ProductUpdateRequest> {
        log.debug("Sending batch {} ({} products) to LLM for extraction, baseUrl={}", batchIndex, batch.size, baseUrl)

        val productsJson = objectMapper.writeValueAsString(batch)

        val params = MessageCreateParams.builder()
            .model(Model.CLAUDE_HAIKU_4_5)
            .maxTokens(8192L)
            .system(SYSTEM_PROMPT)
            .addUserMessage("Base URL: $baseUrl\n\nProducts JSON:\n$productsJson")
            .outputConfig(ProductExtractionResult::class.java)
            .build()

        val message = anthropicClient.messages().create(params)

        log.debug(
            "LLM batch {} response: stopReason={} inputTokens={} outputTokens={}",
            batchIndex,
            message.stopReason(),
            message.usage().inputTokens(),
            message.usage().outputTokens(),
        )

        val result = message.content()
            .stream()
            .flatMap { cb -> cb.text().stream() }
            .map { typed -> typed.text() }
            .findFirst()
            .orElse(null)

        if (result == null) {
            log.warn("LLM returned no structured content for batch {} baseUrl={}", batchIndex, baseUrl)
            return emptyList()
        }

        log.info(
            "LLM extracted {} coffee products from {} raw products in batch {} for baseUrl={}",
            result.products.size,
            batch.size,
            batchIndex,
            baseUrl,
        )
        result.products.forEach { p ->
            log.debug("  Extracted: name={} inStock={} priceInCents={}", p.name, p.inStock, p.priceInCents)
        }

        return result.products
    }

    companion object {
        private const val BATCH_SIZE = 30

        private val SYSTEM_PROMPT = """
            You are a coffee product data extractor. Given a JSON array of Shopify product objects, extract structured data for each coffee product and return a JSON object with a "products" array.

            Rules:
            - ONLY include products that are roasted coffee (whole bean or ground). Skip accessories, merchandise, gift cards, subscriptions, cold brew, RTD drinks, and non-coffee items.
            - Use null for any field you cannot confidently determine.
            - For priceInCents: use the smallest retail bag variant price, converted to integer cents (e.g. "${'$'}19.00" → 1900). Ignore bulk/wholesale variants.
            - For bagSize + bagUnit: parse from variant title or the grams field (227g ≈ 8oz, 340g ≈ 12oz, 454g ≈ 1lb). Prefer oz for small bags.
            - For inStock: true if ANY variant has available=true.
            - For productUrl: construct as "{baseUrl}/products/{handle}" using the handle field from the product.
            - For isDecaf: true if title, tags, or description mentions decaf/decaffeinated.
            - For roastLevel: infer from tags or description — "light", "medium", "medium-dark", or "dark".
            - For productType: "single origin", "blend", or "espresso".
            - For flavorProfile: extract tasting notes as an array of lowercase strings.
            - For process: the processing method found in the description (e.g. "washed", "natural", "honey", "anaerobic").
            - For brewMethods: extract any mentioned brew methods (e.g. "pour over", "espresso", "french press").
            - For availabilityType: "seasonal" if the product name/description implies a limited season, "year-round" otherwise.
            - For description: plain-text version of body_html (strip HTML tags), truncated to 500 chars.
        """.trimIndent()
    }
}