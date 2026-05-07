# Scraping Strategy & IntegrationType

**Status:** Ready  
**Touches:** `worker/` scrapers, `IntegrationType` enum, `WorkerPoller`

Clarifies the relationship between platform detection (`IntegrationType`) and scraping mechanism (API-first vs. Playwright fallback). Playwright is a scraping tool, not an integration type.

---

## Mental Model

`IntegrationType` = what software platform the roaster's site runs on.  
Scraping mechanism = how we extract data from that platform (API, network intercept, or Playwright).

Most platforms have a preferred cheap path (API or known JSON endpoint). Playwright is the fallback when the cheap path fails or isn't available. CUSTOM sites go straight to Playwright.

```
IntegrationType →  Primary strategy              →  Fallback
─────────────────────────────────────────────────────────────
SHOPIFY         →  /products.json                →  Playwright
WOOCOMMERCE     →  WooCommerce REST API           →  Playwright  (future)
SQUARESPACE     →  (no useful API)                →  Playwright  (future)
CUSTOM          →  Playwright directly            →  —
UNKNOWN         →  detect platform, then route    →  Playwright
```

---

## Current State

- `IntegrationType`: SHOPIFY, WOO_COMMERCE, SQUARE, CUSTOM, UNKNOWN
- `WorkerPoller` dispatches by `integrationType` to registered `RoasterScraper` beans
- `ShopifyProductScraper` is `@Component("SHOPIFY")` — handles Shopify via `/products.json` + LLM extraction
- No fallback mechanism exists — if `/products.json` fails, the run fails

---

## Changes

### Rename `SQUARE` → `SQUARESPACE`

`SQUARE` is ambiguous (Square the payments company vs. Squarespace the website builder). Squarespace is the relevant platform here.

- [ ] Update `IntegrationType` enum: rename `SQUARE` → `SQUARESPACE`
- [ ] Flyway migration: update any existing rows with `integration_type = 'SQUARE'`

### Add Playwright fallback to `ShopifyProductScraper`

If `/products.json` returns empty, fails, or is disabled, fall back to Playwright agent scraping rather than returning a failed run. The `PlaywrightAgentScraper` (from the scraper plan) becomes an injectable fallback.

- [ ] Inject `PlaywrightAgentScraper` into `ShopifyProductScraper`
- [ ] If Shopify API path yields no products → delegate to `PlaywrightAgentScraper.scrape(job)`
- [ ] Log clearly when fallback is triggered (useful signal for updating `integrationType`)

### `CUSTOM` and `UNKNOWN` route to Playwright

- [ ] Register `PlaywrightAgentScraper` as `@Component("CUSTOM")` (or use a dispatcher in WorkerPoller)
- [ ] `UNKNOWN` type: attempt platform detection first (check for `cdn.shopify.com`, WooCommerce signals, etc.), update `integrationType` on the roaster, then scrape — OR just route directly to Playwright for now

### Platform detection (future)

On first run of an UNKNOWN roaster, the agent (or a lightweight pre-check) detects the platform and updates `roaster.integrationType`. This means future runs take the optimal path without re-detecting.

Detection signals:
- `cdn.shopify.com` in page source or `Powered by Shopify` → SHOPIFY
- `/wp-json/wc/` available → WOOCOMMERCE  
- Squarespace meta tags / `static1.squarespace.com` → SQUARESPACE
- None of the above → CUSTOM

- [ ] _(future)_ Add lightweight platform detection step before first scrape of UNKNOWN roaster
- [ ] _(future)_ `WorkerJobService` or worker updates `roaster.integrationType` after detection

---

## Impact on Scraper Plan

`PlaywrightAgentScraper` is no longer `@Component("PLAYWRIGHT")`. It should be:
- `@Component("CUSTOM")` for direct registration
- Also injected as a fallback into other scrapers

Update `agents/plans/worker-playwright-scraper.md` accordingly.

---

## Completed

_(nothing yet)_