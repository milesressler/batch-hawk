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
            log.debug("  Extracted: name={} variants={} offersGrinding={}", p.name, p.variants?.size, p.offersGrinding)
        }

        return result.products.map { product ->
            if (product.name?.contains("decaf", ignoreCase = true) == true) product.copy(isDecaf = true)
            else product
        }
    }

    companion object {
        private const val BATCH_SIZE = 30

        private val SYSTEM_PROMPT = """
            You are a coffee product data extractor. Given a JSON array of Shopify product objects, extract structured data for each coffee product and return a JSON object with a "products" array.

            Rules:
            - ONLY include products that are roasted coffee (whole bean or ground). Skip accessories, merchandise, gift cards, subscriptions, cold brew, RTD drinks, and non-coffee items.
            - Use null for any field you cannot confidently determine.
            - For variants: extract ALL retail bag-size pricing tiers as a list. Each item has:
                - bagSize (integer) and bagUnit (string — use "oz" for bags under 2 lbs, "lb" for 2 lbs and above, "g" only if no oz/lb equivalent is available)
                - priceInCents (integer cents, e.g. "${'$'}19.00" → 1900)
                - inStock (boolean — true if that variant's available=true)
              Parse bag size from the variant title or the grams field (227g≈8oz, 340g≈12oz, 454g≈16oz/1lb, 1000g≈35oz/2.2lb).
              Include each distinct retail size (e.g. 4oz, 8oz, 12oz, 1lb, 2lb, 5lb) as a separate variant entry.
              OMIT grind-only variants — if a variant differs from another only in grind (whole bean vs. ground) and has the same size and price, omit it.
              OMIT bulk and wholesale variants (e.g. "Case of 12", "Wholesale").
            - For offersGrinding: true if ANY variant or option allows a pre-ground selection, even if the price matches whole bean.
            - Do NOT populate the top-level priceInCents, bagSize, bagUnit, or inStock fields — use variants instead.
            - For productUrl: construct as "{baseUrl}/products/{handle}" using the handle field from the product.
            - For externalProductId: use the numeric id field from the product object as a string (e.g. "123456789").
            - For isDecaf: true if title, tags, or description mentions decaf/decaffeinated.
            - For roastLevel: infer from tags or description — "light", "medium", "medium-dark", or "dark".
            - For productType: "single origin", "blend", or "espresso".
            - For flavorProfile: extract tasting notes as an array of lowercase strings.
            - For process: the processing method found in the description (e.g. "washed", "natural", "honey", "anaerobic").
            - For brewMethods: extract any mentioned brew methods (e.g. "pour over", "espresso", "french press").
            - For availabilityType: "seasonal" if the product name/description implies a limited season, "year-round" otherwise.
            - For description: paraphrase the body_html content in your own words (strip HTML tags). Keep it to 1-2 sentences, max 200 chars. Do not copy the original text verbatim.
        """.trimIndent()
    }
}