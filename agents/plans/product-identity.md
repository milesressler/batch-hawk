# Product Identity, Deduplication & Freshness

**Status:** Ready  
**Touches:** `api/` entities, migrations, `WorkerJobService`, common DTOs

Products need a stable identity across scrape runs so we can: (1) upsert without creating duplicates, (2) mark products inactive when they disappear from the catalog, (3) track when each product was last seen.

---

## Problems with current state

- **Dedup key is `name` (case-insensitive)** — fragile. Roasters rename products, vary punctuation, add seasonal suffixes. Wrong matches will corrupt observations.
- **`updatedAt` (from `BaseEntity`) doesn't mean "last scraped"** — it also fires when we hide a product in our system, change a field, etc. Ambiguous.
- **No removal logic** — products that disappear from the catalog stay `active = true` forever.
- **`productUrl` captured in `ProductUpdateRequest` but never persisted** — throwing away a reliable stable identifier every run.

---

## Changes

### Add `productUrl` to `Product`

The product URL is the most reliable stable identifier we have — it's consistent across runs, works for Playwright-scraped sites and Shopify (handle-derived), and is useful for "buy now" affiliate links.

- [ ] Flyway migration: add `product_url VARCHAR(512)` to `products`, add unique index `(roaster_id, product_url)` where product_url is not null
- [ ] Add `productUrl: String?` field to `Product.java`
- [ ] Persist `ProductUpdateRequest.productUrl` in `WorkerJobService`

### Add `lastRefreshedAt` to `Product`

Dedicated scrape-freshness timestamp, updated only when a new `ProductObservation` is written. `updatedAt` stays for entity-level changes (hiding a product, correcting a field manually, etc.).

- [ ] Flyway migration: add `last_refreshed_at TIMESTAMPTZ` to `products`
- [ ] Add `lastRefreshedAt: Instant?` to `Product.java`
- [ ] Set `lastRefreshedAt = now()` in `WorkerJobService` whenever a `ProductObservation` is written

### Upsert strategy — prioritize `productUrl`

Change the upsert logic in `WorkerJobService.completeRun()`:

1. If `productUrl` is non-null → look up by `(roasterId, productUrl)` 
2. Else → fall back to `(roasterId, name)` case-insensitive (current behavior)

Add `ProductRepository.findByRoasterIdAndProductUrl()`.

- [ ] Add `findByRoasterIdAndProductUrl(roasterId, productUrl)` to `ProductRepository`
- [ ] Update `WorkerJobService` upsert to try productUrl first

### Mark missing products inactive

If the run returned **at least one product**, any previously active product for this roaster not seen in the run is marked `active = false` immediately. If the run returned zero products, nothing is deactivated — the agent likely failed to navigate the catalog, and penalizing the product list would be wrong.

This avoids the "N consecutive misses" complexity while still protecting against a total scrape failure wiping out the catalog.

Implementation: after writing all products from the run, if `productsWritten > 0`, collect the set of persisted product IDs and flip any other active products for this roaster to inactive.

- [ ] Add `ProductRepository.findActiveByRoasterId(roasterId)`
- [ ] In `WorkerJobService.completeRun()`: if products written > 0, deactivate active products not in this run's result set

---

## Open Questions

- For Shopify stores, the product handle is a more stable ID than the URL (URL can change, handle rarely does). Worth storing the handle separately for Shopify products? Defer until Shopify integration is more mature.

---

## Completed

_(nothing yet)_