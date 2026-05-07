package com.batchhawk.worker.scraper.playwright

import com.anthropic.core.JsonValue
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
    private val integrationType: String? = null,
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
            val waitUntil = if (integrationType == "SQUARE") WaitUntilState.LOAD else WaitUntilState.DOMCONTENTLOADED
            page.navigate(normalized, Page.NavigateOptions().setTimeout(config.navigationTimeoutMs).setWaitUntil(waitUntil))
            if (integrationType == "SQUARE") {
                runCatching { page.waitForLoadState(LoadState.NETWORKIDLE, Page.WaitForLoadStateOptions().setTimeout(3000.0)) }
                val rawHtml = page.evaluate("() => document.documentElement?.outerHTML ?? 'NO_DOCUMENT'") as? String ?: ""
                log.debug("SQUARE raw HTML after load ({}): {}", normalized, rawHtml.take(2000).replace("\n", " "))
            }
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

    fun findAllSelectOptions(): String = runCatching {
        val result = page.evaluate("""
            () => Array.from(document.querySelectorAll('select'))
                .filter(sel => sel.options.length > 1)
                .map((sel, idx) => ({
                    index: idx,
                    id: sel.id || null,
                    name: sel.name || null,
                    options: Array.from(sel.options).map(o => ({
                        value: o.value,
                        text: o.text.trim()
                    }))
                }))
        """.trimIndent())
        objectMapper.writeValueAsString(result)
    }.getOrElse { "[]" }

    fun chooseSelectOption(selectIndex: Int, optionValue: String): String = runCatching {
        page.evaluate("""
            ([idx, value]) => {
                const sel = Array.from(document.querySelectorAll('select'))
                    .filter(s => s.options.length > 1)[idx];
                if (!sel) return;
                sel.value = value;
                ['input', 'change'].forEach(evt =>
                    sel.dispatchEvent(new Event(evt, { bubbles: true })));
            }
        """.trimIndent(), listOf(selectIndex, optionValue))
        page.waitForLoadState(LoadState.NETWORKIDLE, Page.WaitForLoadStateOptions().setTimeout(3000.0))
        preprocessPage()
    }.getOrElse { e ->
        log.warn("chooseSelectOption failed index={} value={}: {}", selectIndex, optionValue, e.message)
        "Select failed: ${e.message}"
    }

    fun scrollToBottom(): String = runCatching {
        page.evaluate("() => window.scrollTo(0, (document.body ?? document.documentElement)?.scrollHeight ?? 0)")
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

    fun returnProductList(input: JsonValue): String = runCatching {
        val rootNode = input.convert(com.fasterxml.jackson.databind.JsonNode::class.java)!!
        log.debug("return_product_list raw input: {}", rootNode.toString().take(500))

        val products: List<DiscoveredProduct> = rootNode["products"]?.map { node ->
            DiscoveredProduct(
                url = node["url"]?.asText() ?: return@map null,
                name = node["name"]?.asText(),
                priceInCents = node["priceInCents"]?.asInt(),
            )
        }?.filterNotNull() ?: emptyList()

        val siteHintsJson = rootNode.get("siteHints")
            ?.takeIf { !it.isNull }
            ?.let { objectMapper.writeValueAsString(it) }

        terminalResult = TerminalResult.Success(DiscoveryResult(products, siteHintsJson))
        log.info("Discovery complete: {} products found", products.size)
        "Product list received. Discovery complete."
    }.getOrElse { e ->
        log.error("Failed to parse return_product_list input", e)
        terminalResult = TerminalResult.Failure("Failed to parse product list: ${e.message}")
        "Parse error: ${e.message}"
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
    data class Success(val discoveryResult: DiscoveryResult) : TerminalResult()
    data class Failure(val reason: String) : TerminalResult()
}
