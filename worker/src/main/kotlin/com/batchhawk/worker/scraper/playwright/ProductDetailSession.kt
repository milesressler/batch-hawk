package com.batchhawk.worker.scraper.playwright

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.batchhawk.common.ProductUpdateRequest
import com.batchhawk.common.VariantInfo
import com.batchhawk.worker.config.WorkerProperties.ScrapingProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory

class ProductDetailSession(
    private val anthropicClient: AnthropicClient,
    private val browserTools: BrowserTools,
    private val config: ScrapingProperties,
    private val objectMapper: ObjectMapper,
    private val integrationType: String? = null,
) {
    private val log = LoggerFactory.getLogger(ProductDetailSession::class.java)
    var inputTokens = 0L
        private set
    var outputTokens = 0L
        private set

    fun extract(product: DiscoveredProduct): ProductUpdateRequest? {
        val pageText = browserTools.navigate(product.url)
        if (pageText.startsWith("Navigation blocked") || pageText.startsWith("Navigation failed") || pageText.startsWith("Already visited")) {
            log.warn("Could not navigate to product page {}: {}", product.url, pageText)
            return product.toPartialUpdateRequest()
        }

        val selectsJson = browserTools.findAllSelectOptions()
        val selectsNode = runCatching { objectMapper.readTree(selectsJson) }.getOrNull()
        val hasSelects = selectsNode != null && selectsNode.size() > 0

        val userMessage = buildString {
            product.name?.let { appendLine("Name (from listing): $it") }
            product.priceInCents?.let { appendLine("Price (from listing): \$${"%.2f".format(it / 100.0)}") }
            appendLine()
            appendLine("Page content:")
            appendLine(pageText)
            if (hasSelects) {
                appendLine()
                appendLine("Dropdowns on this page (index, name, options):")
                appendLine(selectsJson)
            }
        }

        val response = anthropicClient.messages().create(
            MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5)
                .maxTokens(1024L)
                .system(buildSystemPrompt())
                .messages(listOf(MessageParam.builder().role(MessageParam.Role.USER).content(userMessage).build()))
                .build()
        )

        inputTokens += response.usage().inputTokens()
        outputTokens += response.usage().outputTokens()
        log.debug("Product detail for {}: inputTokens={} outputTokens={}", product.url, response.usage().inputTokens(), response.usage().outputTokens())

        val rawText = response.content().firstOrNull { it.isText() }?.asText()?.text()
            ?: return product.toPartialUpdateRequest()

        return runCatching {
            val json = extractJson(rawText)
            val node = objectMapper.readTree(json)

            val name = node["name"]?.asText() ?: product.name ?: return null
            val isDecaf = node["isDecaf"]?.asBoolean() == true || name.contains("decaf", ignoreCase = true)

            // LLM extracted variants directly from option labels — most common case
            var variants: List<VariantInfo>? = node["variants"]?.takeIf { it.isArray && it.size() > 0 }
                ?.map { parseVariantNode(it) }

            val sizeSelectIndex = node["sizeSelectIndex"]?.takeIf { !it.isNull && it.isInt }?.asInt()
            val preselectOptions = node["preselectOptions"]
                ?.filter { it["selectIndex"] != null && it["optionValue"] != null }
                ?.map { it["selectIndex"].asInt() to it["optionValue"].asText() }
                ?: emptyList()
            log.debug("Variant extraction path for {}: variants={} sizeSelectIndex={} preselects={}", product.url, variants?.size, sizeSelectIndex, preselectOptions.size)

            // LLM identified a size select that needs click-through for dynamic prices
            if (variants == null) {
                if (sizeSelectIndex != null && selectsNode != null) {
                    variants = extractVariantsByClickThrough(sizeSelectIndex, preselectOptions, selectsNode)
                }
            } else if (variants.any { it.priceInCents == null }) {
                log.warn("LLM returned variants with null prices for {} — should have used sizeSelectIndex instead", product.url)
            }

            ProductUpdateRequest(
                name = name,
                roastLevel = node["roastLevel"]?.asText(),
                productType = node["productType"]?.asText(),
                originCountry = node["originCountry"]?.asText(),
                originRegion = node["originRegion"]?.asText(),
                process = node["process"]?.asText(),
                brewMethods = node["brewMethods"]?.map { it.asText() },
                flavorProfile = node["flavorProfile"]?.map { it.asText() },
                isDecaf = isDecaf,
                offersGrinding = node["offersGrinding"]?.asBoolean(),
                availabilityType = node["availabilityType"]?.asText(),
                description = node["description"]?.asText(),
                inStock = if (variants != null) null else node["inStock"]?.asBoolean(),
                productUrl = product.url,
                externalProductId = node["externalProductId"]?.asText(),
                variants = variants,
                priceInCents = if (variants != null) null else node["priceInCents"]?.asInt() ?: product.priceInCents,
                bagSize = if (variants != null) null else node["bagSize"]?.asInt(),
                bagUnit = if (variants != null) null else node["bagUnit"]?.asText(),
            )
        }.getOrElse { e ->
            log.warn("Failed to parse product detail for {}: {}", product.url, e.message)
            product.toPartialUpdateRequest()
        }
    }

    private fun extractVariantsByClickThrough(
        sizeSelectIndex: Int,
        preselectOptions: List<Pair<Int, String>>,
        selectsNode: JsonNode,
    ): List<VariantInfo>? {
        val options = selectsNode.get(sizeSelectIndex)?.get("options") ?: return null
        log.debug("Click-through variant extraction: {} options in select[{}], preselects={}", options.size(), sizeSelectIndex, preselectOptions.size)

        preselectOptions.forEach { (selectIdx, value) ->
            log.debug("Applying preselect: select[{}] = '{}'", selectIdx, value)
            browserTools.chooseSelectOption(selectIdx, value)
        }

        val variants = options.mapNotNull { opt ->
            val value = opt["value"]?.asText() ?: return@mapNotNull null
            val label = opt["text"]?.asText() ?: value

            val updatedPage = browserTools.chooseSelectOption(sizeSelectIndex, value)
            if (updatedPage.startsWith("Select failed")) {
                log.warn("chooseSelectOption failed for value={}: {}", value, updatedPage)
                return@mapNotNull null
            }
            log.debug("Page text after selecting '{}' (first 400 chars): {}", label, updatedPage.take(400).replace("\n", " "))

            val priceResponse = anthropicClient.messages().create(
                MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5)
                    .maxTokens(128L)
                    .system(PRICE_EXTRACTION_PROMPT)
                    .messages(listOf(MessageParam.builder().role(MessageParam.Role.USER)
                        .content("Option selected: $label\n\n$updatedPage")
                        .build()))
                    .build()
            )

            inputTokens += priceResponse.usage().inputTokens()
            outputTokens += priceResponse.usage().outputTokens()

            val priceText = priceResponse.content().firstOrNull { it.isText() }?.asText()?.text() ?: return@mapNotNull null
            val priceNode = runCatching { objectMapper.readTree(extractJson(priceText)) }.getOrNull() ?: return@mapNotNull null

            parseVariantNode(priceNode, labelOverride = label)
        }

        return variants.takeIf { it.isNotEmpty() }
    }

    private fun parseVariantNode(node: JsonNode, labelOverride: String? = null): VariantInfo {
        var bagSize = node["bagSize"]?.takeIf { !it.isNull }?.asInt()
        var bagUnit = node["bagUnit"]?.takeIf { !it.isNull }?.asText()

        // Fall back to parsing size from the option label text (e.g. "12 oz", "5 lb")
        if (bagSize == null && labelOverride != null) {
            SIZE_PATTERN.find(labelOverride)?.let { m ->
                bagSize = m.groupValues[1].toIntOrNull()
                bagUnit = m.groupValues[2].lowercase()
            }
        }

        return VariantInfo(
            bagSize = bagSize,
            bagUnit = bagUnit,
            priceInCents = node["priceInCents"]?.asInt(),
            inStock = node["inStock"]?.asBoolean(),
        )
    }

    private fun buildSystemPrompt(): String = buildString {
        appendLine(BASE_SYSTEM_PROMPT)
        if (integrationType == "SQUARESPACE") {
            appendLine()
            appendLine("""
                Platform note: this is a Squarespace site.
                Squarespace uses dynamic pricing — the price shown at the top of the page updates when a size option is selected.
                Size dropdown options contain ONLY size labels (e.g. "4 oz", "12 oz", "5 lb") with NO prices embedded in the option text.
                Therefore: when you see a size dropdown, ALWAYS set "sizeSelectIndex" — never populate "variants" from option labels on this platform.
            """.trimIndent())
        }
    }

    private fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private fun DiscoveredProduct.toPartialUpdateRequest() = name?.let {
        ProductUpdateRequest(name = it, priceInCents = priceInCents, productUrl = url)
    }

    companion object {
        private val SIZE_PATTERN = Regex("""(\d+)\s*(oz|lb|g|kg)\b""", RegexOption.IGNORE_CASE)

        private val BASE_SYSTEM_PROMPT = """
            You are extracting structured data from a coffee product page.
            Return ONLY a valid JSON object — no explanation, no markdown, no code fences.

            If the page has dropdowns (provided as "Dropdowns on this page"), handle size pricing one of two ways:
            A) If size option labels include prices (e.g. "2 oz - ${'$'}6.59"), extract all sizes directly as "variants".
            B) If size option labels have no prices, set "sizeSelectIndex" to that dropdown's index for click-through.
               If other dropdowns (e.g. grind type) must be set first for prices to appear, list them in "preselectOptions"
               with the "Whole Bean" (or equivalent) value so the page is in a valid state before size iteration.
            Never set both "variants" and "sizeSelectIndex".

            JSON schema:
            {
              "name": "string (required)",
              "roastLevel": "light | medium | medium-dark | dark",
              "productType": "single origin | blend | espresso",
              "originCountry": "string",
              "originRegion": "string",
              "process": "washed | natural | honey | anaerobic | other",
              "brewMethods": ["string"],
              "flavorProfile": ["lowercase tasting notes"],
              "isDecaf": boolean,
              "offersGrinding": boolean (true if any grind option is present),
              "availabilityType": "standard | limited | seasonal",
              "description": "1-2 sentence paraphrase, max 200 chars, do not copy verbatim",
              "inStock": boolean,
              "externalProductId": "string",
              "sizeSelectIndex": integer (index of the size/weight dropdown — for dynamic pricing),
              "preselectOptions": [{"selectIndex": integer, "optionValue": string}] (set these before iterating sizes),
              "variants": [
                { "bagSize": integer, "bagUnit": "oz|g|lb|kg", "priceInCents": integer, "inStock": boolean }
              ],
              "priceInCents": integer (only when no variants — flat single-price product),
              "bagSize": integer (only when no variants),
              "bagUnit": "oz | g | lb | kg" (only when no variants)
            }

            Omit fields you cannot determine. Do not guess. Do not set both "variants" and "sizeSelectIndex".
        """.trimIndent()

        private val PRICE_EXTRACTION_PROMPT = """
            A size option was just selected on a coffee product page. Extract the current price and stock status.
            Return ONLY a valid JSON object — no explanation, no markdown.

            JSON schema:
            {
              "bagSize": integer,
              "bagUnit": "oz | g | lb | kg",
              "priceInCents": integer (e.g. 1895 for ${'$'}18.95),
              "inStock": boolean
            }

            Omit fields you cannot determine.
        """.trimIndent()
    }
}
