package com.batchhawk.worker.scraper.playwright

data class DiscoveredProduct(
    val url: String,
    val name: String? = null,
    val priceInCents: Int? = null,
)

data class DiscoveryResult(
    val products: List<DiscoveredProduct>,
    val siteHintsJson: String?,
)
