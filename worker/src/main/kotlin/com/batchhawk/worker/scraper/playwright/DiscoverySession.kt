package com.batchhawk.worker.scraper.playwright

import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUnion
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.config.WorkerProperties.ScrapingProperties
import org.slf4j.LoggerFactory

class DiscoverySession(
    private val anthropicClient: AnthropicClient,
    private val browserTools: BrowserTools,
    private val config: ScrapingProperties,
    private val job: NextJobResponse,
) {
    private val log = LoggerFactory.getLogger(DiscoverySession::class.java)
    var inputTokens = 0L
        private set
    var outputTokens = 0L
        private set

    fun run(): DiscoveryResult {
        val history = mutableListOf<MessageParam>()
        history.add(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content("Find all coffee products at: ${job.websiteUrl ?: "unknown URL"}")
                .build()
        )

        var turns = 0
        while (turns < config.maxTurns && !browserTools.isSessionComplete()) {
            val remaining = config.maxTurns - turns
            val response = anthropicClient.messages().create(
                MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5)
                    .maxTokens(8192L)
                    .system(buildSystemPrompt(remaining))
                    .tools(ScrapingToolDefinitions.TOOLS.map { ToolUnion.ofTool(it) })
                    .messages(history)
                    .build()
            )

            inputTokens += response.usage().inputTokens()
            outputTokens += response.usage().outputTokens()
            log.debug("Turn {}/{}: stopReason={} inputTokens={} outputTokens={}", turns + 1, config.maxTurns, response.stopReason(), response.usage().inputTokens(), response.usage().outputTokens())

            history.add(toAssistantParam(response))
            if (browserTools.isSessionComplete()) break

            val isToolUse = response.stopReason().map { it == StopReason.TOOL_USE }.orElse(false)
            if (!isToolUse) break

            val toolResults = response.content()
                .filter { it.isToolUse() }
                .map { block ->
                    val tu = block.asToolUse()
                    ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder()
                            .toolUseId(tu.id())
                            .content(executeTool(tu.name(), tu._input()))
                            .build()
                    )
                }

            if (toolResults.isEmpty()) break

            turns++
            val turnsLeft = config.maxTurns - turns

            val userBlocks = buildList {
                addAll(toolResults)
                if (turnsLeft <= 3 && !browserTools.isSessionComplete()) {
                    add(ContentBlockParam.ofText(
                        TextBlockParam.builder()
                            .text("[$turnsLeft turn${if (turnsLeft == 1) "" else "s"} remaining — call return_product_list now with all URLs collected so far]")
                            .build()
                    ))
                }
            }

            history.add(
                MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(userBlocks)
                    .build()
            )
        }

        if (!browserTools.isSessionComplete()) {
            log.warn("Discovery ended without terminal tool after {} turns (runId={})", turns, job.runId)
        }

        return when (val result = browserTools.terminalResult) {
            is TerminalResult.Success -> result.discoveryResult
            is TerminalResult.Failure -> {
                log.warn("Discovery failed: {}", result.reason)
                DiscoveryResult(emptyList(), null)
            }
            null -> DiscoveryResult(emptyList(), null)
        }
    }

    private fun executeTool(name: String, input: JsonValue): String {
        val obj = input.asObject().orElse(emptyMap())
        fun str(key: String) = obj[key]?.asString()?.orElse(null) ?: ""

        val argSummary = when (name) {
            "navigate" -> str("url")
            "click" -> str("selector")
            "extract_links" -> obj["filter"]?.asString()?.orElse("(no filter)") ?: "(no filter)"
            "report_failure" -> str("reason")
            "return_product_list" -> {
                val products = obj["products"]?.asArray()?.orElse(emptyList()) ?: emptyList()
                "${products.size} products"
            }
            else -> ""
        }
        log.info("Tool call: {} {}", name, argSummary)

        val result = when (name) {
            "navigate" -> browserTools.navigate(str("url"))
            "get_page_content" -> browserTools.getPageContent()
            "click" -> browserTools.click(str("selector"))
            "scroll_to_bottom" -> browserTools.scrollToBottom()
            "extract_links" -> browserTools.extractLinks(obj["filter"]?.asString()?.orElse(null))
            "return_product_list" -> browserTools.returnProductList(input)
            "report_failure" -> browserTools.reportFailure(str("reason"))
            else -> {
                log.warn("Unknown tool called: {}", name)
                "Unknown tool: $name"
            }
        }

        when (name) {
            "extract_links" -> log.debug("Tool result (extract_links): {}", result)
            "return_product_list", "report_failure" -> {}
            else -> log.debug("Tool result ({}): {}", name, result.take(200).replace("\n", " "))
        }

        return result
    }

    private fun toAssistantParam(message: Message): MessageParam {
        val blocks = message.content().mapNotNull { block ->
            when {
                block.isText() -> ContentBlockParam.ofText(
                    TextBlockParam.builder().text(block.asText().text()).build()
                )
                block.isToolUse() -> ContentBlockParam.ofToolUse(block.asToolUse().toParam())
                else -> null
            }
        }
        return MessageParam.builder()
            .role(MessageParam.Role.ASSISTANT)
            .contentOfBlockParams(blocks)
            .build()
    }

    private fun buildSystemPrompt(turnsRemaining: Int): String = buildString {
        appendLine("""
            You are a web scraper discovering coffee products on a roaster's website.
            Your only job: find all individual coffee/tea product page URLs and return them via return_product_list.
            Do NOT extract detailed product data — just URLs, and name/price if visible on listing pages.
            Only include coffee or tea products. Skip gift cards, subscriptions, chicory, merchandise, apparel, accessories, K-Cups, sampler packs, or any non-beverage items.

            You have $turnsRemaining turn${if (turnsRemaining == 1) "" else "s"} remaining (including this one).
            Budget turns carefully — call return_product_list as soon as you have collected all product URLs.

            Per-page workflow — follow this EXACTLY for each listing/category page:
            1. Navigate to the page.
            2. IMMEDIATELY call extract_links (no filter, or filtered by the category name) to get all links on that page.
            3. Collect any links that look like individual product pages (path typically contains category + product slug, e.g. /single-origin/colombia-supremo).
            4. Only then navigate to the next listing page and repeat.

            Never navigate away from a listing page before calling extract_links on it.
            Never call navigate more than once per turn.

            Category efficiency rule:
            If the shop has an "All" (or equivalent catch-all) category that shows every product, use ONLY that page.
            Do NOT visit individual filter pages (Light Roast, Medium Roast, Single Origin, etc.) — they are subsets
            of "All" and visiting them wastes turns on duplicate URLs.

            Overall strategy:
            1. Navigate to the starting URL to find the shop section.
            2. Use extract_links to find category/listing page URLs.
            3. If an "All products" page exists, navigate to it and extract links — skip per-filter pages.
            4. Otherwise visit each category page: navigate → extract_links → collect product URLs.
            5. Follow pagination if present and collect from each page.
            6. Call return_product_list with all discovered product URLs and siteHints.

            Include in siteHints: productListingUrls, paginationType, paginationParam, platformType,
            requiresDetailPage, and any failedStrategies (patterns you tried that did not work).

            Call report_failure only if the site blocks access or has no products after exhausting navigation.

            Selector rules: use standard CSS selectors only — :contains() is not valid.
        """.trimIndent())

        if (job.integrationType == "SQUARE") {
            appendLine()
            appendLine("""
                Platform note: this is a Square Online store (powered by Weebly/editmysite.com).
                The page is a JavaScript SPA — content loads after the initial HTML, so extract_links may return empty on the first call.
                If extract_links returns [] on a page that should have products, try scroll_to_bottom followed by another extract_links call.
                Product URLs on Square Online typically contain /l/ (e.g. /l/product-name) or are under a /store/ path.
            """.trimIndent())
        }

        job.urlHints?.let { hints ->
            appendLine("\nKnown site structure — go directly to known pages, skip discovery:")
            appendLine(hints)
        }
    }
}
