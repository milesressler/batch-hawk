# Playwright Two-Stage Scraper

**Status:** Planning  
**Branch:** TBD  
**Module:** `worker/`

## Goal

Replace the current single-loop `ScraperAgentSession` with a two-stage pipeline that is smarter, cheaper, and learns across runs. The existing infrastructure (`BrowserTools`, `BrowserManager`, `PlaywrightConfig`) stays unchanged.

## Problem with Current Approach

The single agentic loop accumulates the full conversation history on every turn. By turn 10 the agent is paying to re-send 8,000+ tokens of page content it already processed. The agent also tries to do everything at once (discover structure, paginate, AND extract product details) which leads to wasted turns and confused strategies.

## New Architecture

```
PlaywrightAgentScraper
  │
  ├── Stage 1: DiscoverySession (one agentic loop)
  │     BrowserTools + Claude
  │     Goal: find all product pages, collect basic listing data
  │     Output: List<DiscoveredProduct> + updated SiteHints
  │
  └── Stage 2: ProductDetailSession (one call per product, no loop)
        BrowserTools + Claude (plain JSON response, no tool_use)
        Goal: extract full details for one product
        Input: product URL + partial data from Stage 1
        Output: ProductUpdateRequest
        → run sequentially (parallel later)
  
  Join all ProductUpdateRequests → CompleteRunRequest
```

## Stage 1 — Discovery

**Focused job:** navigate the site, find all product URLs, return basic listing data. Does NOT extract full product details.

`DiscoverySession` replaces `ScraperAgentSession`. Key differences:
- Smaller tool set — no `return_products` with full schema; instead a `return_product_list` terminal tool with a lightweight schema
- System prompt focused purely on navigation and URL discovery
- Knows to use `urlHints` to skip discovery on repeat runs
- Updates `SiteHints` with what worked and what failed

**Terminal tool: `return_product_list`**
```json
{
  "products": [
    { "url": "https://...", "name": "Alfred's Blend", "priceInCents": 1900 }
  ],
  "siteHints": {
    "productListingUrls": ["/product-category/coffee/"],
    "paginationType": "paged_param",
    "paginationParam": "paged",
    "requiresDetailPage": true,
    "platformType": "woocommerce",
    "failedStrategies": ["?page=N"]
  }
}
```

**SiteHints forward context** — persisted to `producers.url_hints` after each run. On repeat runs Stage 1 reads them to skip discovery entirely if listing URLs are already known. `failedStrategies` prevents retrying dead-end pagination patterns.

## Stage 2 — Product Detail Extraction

One API call per product. No tool_use, no loop — just navigate + extract.

`ProductDetailSession`:
1. Navigates to the product URL via `BrowserTools`
2. Gets preprocessed page text
3. Single `messages.create()` call with:
   - System prompt: "Extract structured coffee product data from this page text. Return JSON only."
   - User message: page text + whatever is already known from Stage 1 (name, price)
   - No tools — plain JSON response
4. Deserializes response to `ProductUpdateRequest`

Plain JSON avoids tool_use overhead (no schema validation, no multi-turn loop). Fixed ~800-token context regardless of how many products there are.

## SiteHints Schema

```json
{
  "productListingUrls": ["/product-category/coffee/"],
  "paginationType": "query_param | paged_param | load_more | infinite_scroll | none",
  "paginationParam": "paged",
  "requiresDetailPage": true,
  "platformType": "squarespace | woocommerce | shopify | custom",
  "shippingPolicyUrl": "/pages/shipping",
  "failedStrategies": ["?page=N", "?page_num=N"],
  "lastSuccessfulRun": "2026-05-06"
}
```

Stored in existing `producers.url_hints` JSONB column. No schema changes needed.

## New DTOs

**`DiscoveredProduct`** (worker-internal, not persisted):
```kotlin
data class DiscoveredProduct(
    val url: String,
    val name: String? = null,
    val priceInCents: Int? = null,
)
```

**`DiscoveryResult`** (Stage 1 output):
```kotlin
data class DiscoveryResult(
    val products: List<DiscoveredProduct>,
    val siteHintsJson: String?,
)
```

## Tasks

### New Tool Definition
- [ ] Add `return_product_list` tool to `ScrapingToolDefinitions` (lightweight schema: url, name?, priceInCents?)
- [ ] Remove `return_products` from tool set (moves to Stage 2 as plain JSON)

### DiscoverySession
- [ ] Create `DiscoverySession` — agentic loop focused on URL discovery
- [ ] System prompt: navigation + listing only, no detail extraction
- [ ] Terminal tool: `return_product_list`
- [ ] Include `urlHints` in prompt for repeat runs (skip discovery if listing URLs known)
- [ ] `DiscoveryResult` output
- [ ] Accumulate token counts

### ProductDetailSession
- [ ] Create `ProductDetailSession` — single API call, no loop
- [ ] Navigate product URL via `BrowserTools`
- [ ] Single `messages.create()` with plain JSON system prompt
- [ ] Deserialize response JSON → `ProductUpdateRequest`
- [ ] Pass partial data from `DiscoveredProduct` as context
- [ ] Accumulate token counts
- [ ] Handle parse failures gracefully (skip product, log warning)

### PlaywrightAgentScraper
- [ ] Orchestrate Stage 1 → Stage 2 → join
- [ ] Pass `DiscoveredProduct` list to Stage 2 sessions
- [ ] Merge token counts from all sessions into `CompleteRunRequest`
- [ ] Pass updated `siteHints` from Stage 1 into `CompleteRunRequest`

### Cleanup
- [ ] Remove `ScraperAgentSession` (replaced by `DiscoverySession`)
- [ ] Remove `return_products` / `report_failure` from `BrowserTools` (no longer terminal tools in Stage 1)
- [ ] Keep `BrowserTools` otherwise unchanged

### Tests
- [ ] Unit test `ProductDetailSession` — mock page text, assert correct `ProductUpdateRequest` fields
- [ ] Unit test `DiscoverySession` — mock `BrowserTools`, assert `return_product_list` parsing
- [ ] Unit test `SiteHints` JSON round-trip (serialize → store → deserialize)
- [ ] Integration test `PlaywrightAgentScraper` — mock Anthropic client + Playwright, assert end-to-end flow
- [ ] Test graceful handling of Stage 2 parse failure (one bad product doesn't fail whole run)

## Future Work

- Parallelize Stage 2 product detail calls (coroutines or virtual threads)
- Skip Stage 1 entirely when `urlHints` has all listing URLs (go straight to Stage 2)
- Skip Stage 2 for products where listing page data is sufficient (`requiresDetailPage: false` in hints)
- Bot detection mitigations (stealth plugin, proxy rotation)
- `add_to_cart` + `get_checkout_shipping` for shipping cost discovery
- HTML → Markdown conversion (flexmark) if token cost warrants it
