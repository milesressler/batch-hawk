package com.batchhawk.worker.scraper.playwright

import com.anthropic.core.JsonValue
import com.anthropic.models.messages.Tool

object ScrapingToolDefinitions {

    val TOOLS: List<Tool> by lazy { listOf(
        navigate,
        getPageContent,
        click,
        scrollToBottom,
        extractLinks,
        returnProductList,
        reportFailure,
    ) }

    private val navigate = tool(
        name = "navigate",
        description = """
            Navigate the browser to a URL and return the rendered page text.
            Use to move to a shop page, product listing, product detail, or shipping policy.
            Returns cleaned visible text after JavaScript has rendered.
        """.trimIndent(),
        required = listOf("url"),
        properties = mapOf(
            "url" to mapOf("type" to "string", "description" to "Absolute URL to navigate to")
        )
    )

    private val getPageContent = tool(
        name = "get_page_content",
        description = "Return the rendered text of the current page without navigating. Use after a click or scroll to see updated content.",
    )

    private val click = tool(
        name = "click",
        description = "Click an element on the current page by CSS selector and return the updated page text. Use to follow links, expand sections, or trigger load-more buttons.",
        required = listOf("selector"),
        properties = mapOf(
            "selector" to mapOf("type" to "string", "description" to "CSS selector of the element to click")
        )
    )

    private val scrollToBottom = tool(
        name = "scroll_to_bottom",
        description = "Scroll to the bottom of the current page and return updated text. Use on infinite-scroll product listings to reveal more items.",
    )

    private val extractLinks = tool(
        name = "extract_links",
        description = "Return all links on the current page as a JSON array of {text, href}. Optionally filter by a keyword. Use to find product pages, pagination links, or shipping policy pages.",
        properties = mapOf(
            "filter" to mapOf("type" to "string", "description" to "Optional keyword to filter links by text or href")
        )
    )

    private val returnProductList = tool(
        name = "return_product_list",
        description = """
            Signal that discovery is complete. Return all product page URLs found, with
            any name and price visible on the listing page. Also include site structure hints
            discovered during this run to speed up future scrapes.
            Call this once you have collected product URLs from all listing pages.
        """.trimIndent(),
        required = listOf("products"),
        properties = mapOf(
            "products" to mapOf(
                "type" to "array",
                "description" to "Product pages discovered on this site",
                "items" to mapOf(
                    "type" to "object",
                    "required" to listOf("url"),
                    "properties" to mapOf(
                        "url" to mapOf("type" to "string", "description" to "Absolute URL of the product page"),
                        "name" to mapOf("type" to "string", "description" to "Product name if visible on the listing page"),
                        "priceInCents" to mapOf("type" to "integer", "description" to "Price in cents if visible on the listing page, e.g. 1895 for \$18.95"),
                    )
                )
            ),
            "siteHints" to mapOf(
                "type" to "object",
                "description" to "Site structure discovered during this run",
                "properties" to mapOf(
                    "productListingUrls" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "description" to "URLs of product listing/category pages"),
                    "paginationType" to mapOf("type" to "string", "description" to "query_param, paged_param, load_more, infinite_scroll, or none"),
                    "paginationParam" to mapOf("type" to "string", "description" to "Query parameter name for pagination, e.g. 'paged'"),
                    "requiresDetailPage" to mapOf("type" to "boolean", "description" to "Whether individual product pages are needed for full data"),
                    "platformType" to mapOf("type" to "string", "description" to "woocommerce, squarespace, shopify, or custom"),
                    "failedStrategies" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "description" to "Pagination or navigation patterns that did not work"),
                )
            )
        )
    )

    private val reportFailure = tool(
        name = "report_failure",
        description = "Signal that you cannot extract product data and explain why. Use when the site blocks scraping, has no products, requires login, or you have exhausted reasonable navigation attempts.",
        required = listOf("reason"),
        properties = mapOf(
            "reason" to mapOf("type" to "string", "description" to "Explanation of why extraction failed")
        )
    )

    private fun tool(
        name: String,
        description: String,
        required: List<String> = emptyList(),
        properties: Map<String, Any> = emptyMap(),
    ): Tool = Tool.builder()
        .name(name)
        .description(description)
        .inputSchema(
            Tool.InputSchema.builder()
                .type(JsonValue.from("object"))
                .required(required)
                .putAdditionalProperty("properties", JsonValue.from(properties))
                .build()
        )
        .build()
}
