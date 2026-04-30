package com.batchhawk.worker.scraper.shopify

import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.batchhawk.common.RoasterUpdateRequest
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class RoasterInfoExtractor(
    private val externalWebClient: WebClient,
    private val anthropicClient: AnthropicClient,
) {

    private val log = LoggerFactory.getLogger(RoasterInfoExtractor::class.java)

    fun extract(baseUrl: String): RoasterUpdateRequest? {
        val pageText = fetchAboutPageText(baseUrl) ?: return null

        val params = MessageCreateParams.builder()
            .model(Model.CLAUDE_HAIKU_4_5)
            .maxTokens(256L)
            .system(SYSTEM_PROMPT)
            .addUserMessage(pageText.take(MAX_TEXT_CHARS))
            .outputConfig(RoasterInfoResult::class.java)
            .build()

        val message = anthropicClient.messages().create(params)

        val result = message.content()
            .stream()
            .flatMap { cb -> cb.text().stream() }
            .map { typed -> typed.text() }
            .findFirst()
            .orElse(null)

        if (result == null) {
            log.warn("LLM returned no roaster info for baseUrl={}", baseUrl)
            return null
        }

        log.info("Extracted roaster info for {}: city={} state={}", baseUrl, result.city, result.state)
        return RoasterUpdateRequest(city = result.city, state = result.state, logoUrl = null)
    }

    private fun fetchAboutPageText(baseUrl: String): String? {
        val pages = try {
            externalWebClient.get()
                .uri("$baseUrl/pages.json")
                .retrieve()
                .bodyToMono(ShopifyPagesResponse::class.java)
                .block()
                ?.pages ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch pages.json for {}: {}", baseUrl, e.message)
            return null
        }

        val aboutPage = pages.firstOrNull { page ->
            ABOUT_KEYWORDS.any { kw ->
                page.handle?.contains(kw, ignoreCase = true) == true ||
                page.title?.contains(kw, ignoreCase = true) == true
            }
        }

        if (aboutPage == null) {
            log.debug("No about-style page found for {}", baseUrl)
            return null
        }

        return aboutPage.bodyHtml?.let { Jsoup.parse(it).text() }
    }

    companion object {
        private const val MAX_TEXT_CHARS = 3_000
        private val ABOUT_KEYWORDS = listOf("about", "story", "who-we-are", "who we are")

        private val SYSTEM_PROMPT = """
            You are a data extractor. Given text from a coffee roaster's About page, extract their physical location.

            Rules:
            - For city: the city where the roaster is physically located. Use null if not found.
            - For state: the US state (or Canadian province) abbreviation (e.g. "CA", "NY", "TX", "BC"). Use null if not found or not applicable.
            - Return null for any field you cannot confidently determine.
        """.trimIndent()
    }
}

data class RoasterInfoResult(
    val city: String?,
    val state: String?,
)