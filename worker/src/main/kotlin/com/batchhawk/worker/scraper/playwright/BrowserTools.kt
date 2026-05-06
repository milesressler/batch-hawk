package com.batchhawk.worker.scraper.playwright

import com.anthropic.core.JsonValue
import com.batchhawk.common.ProductUpdateRequest
import com.batchhawk.worker.config.WorkerProperties.ScrapingProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.LoadState
import com.microsoft.playwright.options.WaitUntilState
import org.slf4j.LoggerFactory
import java.net.URI

class BrowserTools(
    private val page: Page,
    private val allowedDomain: String,
    private val config: ScrapingProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(BrowserTools::class.java)
    private val visitedUrls = mutableSetOf<String>()

    var terminalResult: TerminalResult? = null
        private set

    fun isSessionComplete() = terminalResult != null

    fun navigate(url: String): String {
        val resolved = resolveUrl(url)

        val domain = runCatching { URI(resolved).host }.getOrNull()
        if (domain == null || baseDomain(domain) != baseDomain(allowedDomain)) {
            log.warn("Blocked cross-domain navigation: {} (allowed: {})", url, allowedDomain)
            return "Navigation blocked: only URLs within $allowedDomain are allowed."
        }

        val normalized = normalizeUrl(resolved)
        if (normalized in visitedUrls) {
            return "Already visited $normalized — choose a different page."
        }
        visitedUrls += normalized

        return runCatching {
            page.navigate(normalized, Page.NavigateOptions().setTimeout(config.navigationTimeoutMs).setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
            preprocessPage()
        }.getOrElse { e ->
            log.warn("Navigation failed for {}: {}", normalized, e.message)
            "Navigation failed: ${e.message}"
        }
    }

    fun getPageContent(): String = runCatching {
        preprocessPage()
    }.getOrElse { e ->
        "Could not get page content: ${e.message}"
    }

    fun click(selector: String): String = runCatching {
        page.locator(selector).first().click(Locator.ClickOptions().setTimeout(config.navigationTimeoutMs.toDouble()))
        page.waitForLoadState(LoadState.DOMCONTENTLOADED)
        preprocessPage()
    }.getOrElse { e ->
        log.warn("Click failed for '{}': {}", selector, e.message)
        "Click failed — element not found or not clickable. Selector: $selector"
    }

    fun scrollToBottom(): String = runCatching {
        page.evaluate("() => window.scrollTo(0, document.body.scrollHeight)")
        page.waitForTimeout(1500.0)
        preprocessPage()
    }.getOrElse { e ->
        "Scroll failed: ${e.message}"
    }

    fun extractLinks(filter: String?): String = runCatching {
        val links = page.locator("a[href]").all()
            .mapNotNull { el ->
                val text = runCatching { el.innerText().trim() }.getOrNull() ?: return@mapNotNull null
                val href = runCatching { el.getAttribute("href") }.getOrNull() ?: return@mapNotNull null
                mapOf("text" to text, "href" to href)
            }
            .filter { link ->
                filter == null ||
                    link["text"]!!.contains(filter, ignoreCase = true) ||
                    link["href"]!!.contains(filter, ignoreCase = true)
            }
            .take(config.maxLinks)

        objectMapper.writeValueAsString(links)
    }.getOrElse { e ->
        "Could not extract links: ${e.message}"
    }

    fun returnProducts(input: JsonValue): String = runCatching {
        val rootNode = input.convert(com.fasterxml.jackson.databind.JsonNode::class.java)!!
        log.debug("return_products raw input: {}", rootNode.toString().take(500))

        val products: List<ProductUpdateRequest> = objectMapper.treeToValue(
            rootNode["products"],
            objectMapper.typeFactory.constructCollectionType(List::class.java, ProductUpdateRequest::class.java)
        )

        val siteHintsJson = rootNode.get("siteHints")
            ?.takeIf { !it.isNull }
            ?.let { objectMapper.writeValueAsString(it) }

        terminalResult = TerminalResult.Success(products, siteHintsJson)
        log.info("Agent returned {} products", products.size)
        "Products received. Session complete."
    }.getOrElse { e ->
        log.error("Failed to parse return_products input", e)
        terminalResult = TerminalResult.Failure("Failed to parse products: ${e.message}")
        "Parse error — could not deserialize products: ${e.message}"
    }

    fun reportFailure(reason: String): String {
        log.warn("Agent reported failure: {}", reason)
        terminalResult = TerminalResult.Failure(reason)
        return "Failure recorded."
    }

    private fun preprocessPage(): String {
        val text = page.evaluate("""
            () => {
                document.querySelectorAll(
                    'script, style, nav, header, footer, [role="navigation"],' +
                    '[id*="cookie"], [class*="cookie"],' +
                    '[id*="chat"], [class*="chat"],' +
                    '[id*="popup"], [class*="popup"]'
                ).forEach(el => el.remove());
                return document.body?.innerText ?? '';
            }
        """.trimIndent()) as? String ?: ""

        return text.trim().take(config.pageTextMaxChars)
    }

    private fun resolveUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val current = runCatching { URI(page.url()) }.getOrNull() ?: return url
        return current.resolve(url).toString()
    }

    private fun normalizeUrl(url: String): String =
        url.trimEnd('/')
            .replace(Regex("[?&](utm_source|utm_medium|utm_campaign|utm_content|utm_term)=[^&]*"), "")
            .trimEnd('?', '&')

    private fun baseDomain(host: String): String = host.removePrefix("www.")
}

sealed class TerminalResult {
    data class Success(
        val products: List<ProductUpdateRequest>,
        val siteHintsJson: String?,
    ) : TerminalResult()

    data class Failure(val reason: String) : TerminalResult()
}
