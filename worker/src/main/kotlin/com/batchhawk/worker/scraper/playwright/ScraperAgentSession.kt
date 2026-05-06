package com.batchhawk.worker.scraper.playwright

import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.*
import com.batchhawk.common.CompleteRunRequest
import com.batchhawk.common.NextJobResponse
import com.batchhawk.worker.config.WorkerProperties.ScrapingProperties
import org.slf4j.LoggerFactory

class ScraperAgentSession(
    private val anthropicClient: AnthropicClient,
    private val browserTools: BrowserTools,
    private val config: ScrapingProperties,
    private val job: NextJobResponse,
) {
    private val log = LoggerFactory.getLogger(ScraperAgentSession::class.java)
    private var inputTokens = 0L
    private var outputTokens = 0L

    fun run(): CompleteRunRequest {
        val history = mutableListOf<MessageParam>()
        history.add(
            MessageParam.builder()
                .role(MessageParam.Role.USER)
                .content("Scrape all coffee products from: ${job.websiteUrl ?: "unknown URL"}")
                .build()
        )

        val systemPrompt = buildSystemPrompt()

        var turns = 0
        while (turns < config.maxTurns && !browserTools.isSessionComplete()) {
            val response = anthropicClient.messages().create(
                MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5)
                    .maxTokens(4096L)
                    .system(systemPrompt)
                    .tools(ScrapingToolDefinitions.TOOLS.map { ToolUnion.ofTool(it) })
                    .messages(history)
                    .build()
            )

            inputTokens += response.usage().inputTokens()
            outputTokens += response.usage().outputTokens()

            log.debug(
                "Turn {}: stopReason={} inputTokens={} outputTokens={}",
                turns,
                response.stopReason(),
                response.usage().inputTokens(),
                response.usage().outputTokens(),
            )

            history.add(toAssistantParam(response))

            if (browserTools.isSessionComplete()) break

            val isToolUse = response.stopReason()
                .map { it == StopReason.TOOL_USE }
                .orElse(false)
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

            val toolResultsParam = MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(toolResults)
                .build()
            history.add(toolResultsParam)

            turns++

            if (turns >= config.maxTurns - 1 && !browserTools.isSessionComplete()) {
                history.add(
                    MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content("This is your final turn. Call return_products now with whatever products you have collected, even if incomplete. Do not navigate further.")
                        .build()
                )
            }
        }

        if (!browserTools.isSessionComplete()) {
            log.warn("Session ended without terminal tool after {} turns (runId={})", turns, job.runId)
        }

        return buildCompleteRunRequest(turns)
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

    private fun executeTool(name: String, input: JsonValue): String {
        val obj = input.asObject().orElse(emptyMap())
        fun str(key: String) = obj[key]?.asString()?.orElse(null) ?: ""

        val argSummary = when (name) {
            "navigate" -> str("url")
            "click" -> str("selector")
            "extract_links" -> obj["filter"]?.asString()?.orElse("(no filter)") ?: "(no filter)"
            "report_failure" -> str("reason")
            "return_products" -> {
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
            "return_products" -> browserTools.returnProducts(input)
            "report_failure" -> browserTools.reportFailure(str("reason"))
            else -> {
                log.warn("Unknown tool called: {}", name)
                "Unknown tool: $name"
            }
        }

        when (name) {
            "extract_links" -> log.debug("Tool result (extract_links): {}", result)
            "return_products", "report_failure" -> {}
            else -> log.debug("Tool result ({}): {}", name, result.take(200).replace("\n", " "))
        }

        return result
    }

    private fun buildSystemPrompt(): String = buildString {
        appendLine(
            """
            You are an autonomous web scraper that extracts coffee product data from roaster websites.
            Your goal: find all coffee products for sale and return structured data via return_products.

            Strategy — follow this order:
            1. Navigate to the shop/product listing page.
            2. Use extract_links to find product page URLs (filter by "product" or "coffee" if helpful).
            3. Navigate to each individual product page and extract: name, price, bag size, roast level,
               origin, process, and tasting notes. The listing page often shows name and price — use that
               when a detail page visit is not needed.
            4. Once you have collected products, call return_products immediately — do NOT spend turns
               chasing pagination before you have any products. Collect what you can and return it.

            For validation purposes, stop after collecting 3 products and call return_products immediately.

            Include siteHints with structural patterns you discovered (listing URLs, pagination type).
            Call report_failure only if the site blocks access, requires login, or has no products
            after exhausting reasonable navigation attempts.

            Selector rules: use standard CSS selectors only. :contains() is not valid — use
            attribute selectors, class names, or nth-child instead.
            """.trimIndent()
        )
        job.urlHints?.let { hints ->
            appendLine("\nKnown site structure from previous runs — go directly to known pages, skip discovery:")
            appendLine(hints)
        }
    }

    private fun buildCompleteRunRequest(turns: Int): CompleteRunRequest =
        when (val result = browserTools.terminalResult) {
            is TerminalResult.Success -> CompleteRunRequest(
                status = "SUCCESS",
                products = result.products,
                notes = "Scraped ${result.products.size} products in $turns turns",
                siteHints = result.siteHintsJson,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
            )
            is TerminalResult.Failure -> CompleteRunRequest(
                status = "FAILED",
                products = emptyList(),
                notes = result.reason,
                inputTokens = inputTokens,
                outputTokens = outputTokens,
            )
            null -> CompleteRunRequest(
                status = "FAILED",
                products = emptyList(),
                notes = "Agent did not call return_products or report_failure within ${config.maxTurns} turns",
                inputTokens = inputTokens,
                outputTokens = outputTokens,
            )
        }
}
