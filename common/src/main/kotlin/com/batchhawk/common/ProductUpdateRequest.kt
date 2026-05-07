package com.batchhawk.common

data class VariantInfo(
    val bagSize: Int? = null,
    val bagUnit: String? = null,
    val priceInCents: Int? = null,
    val inStock: Boolean? = null,
)

data class ProductUpdateRequest(
    val name: String?,
    val roastLevel: String? = null,
    val productType: String? = null,
    val originCountry: String? = null,
    val originRegion: String? = null,
    val process: String? = null,
    val brewMethods: List<String>? = null,
    val flavorProfile: List<String>? = null,
    val isDecaf: Boolean? = null,
    val availabilityType: String? = null,
    val description: String? = null,
    val productUrl: String? = null,
    val externalProductId: String? = null,
    val offersGrinding: Boolean? = null,
    /** All retail bag-size pricing tiers for this product. Preferred over the flat fields below. */
    val variants: List<VariantInfo>? = null,
    /** Flat fields — used by Playwright scraper for single-variant products. */
    val priceInCents: Int? = null,
    val bagSize: Int? = null,
    val bagUnit: String? = null,
    val inStock: Boolean? = null,
)