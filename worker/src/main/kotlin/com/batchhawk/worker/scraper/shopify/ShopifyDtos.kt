package com.batchhawk.worker.scraper.shopify

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShopifyProductsResponse(
    val products: List<ShopifySlimProduct> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShopifySlimProduct(
    val id: Long?,
    val title: String?,
    val handle: String?,
    val tags: List<String> = emptyList(),
    @JsonProperty("product_type") val productType: String?,
    @JsonProperty("body_html") val bodyHtml: String?,
    val variants: List<ShopifySlimVariant> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShopifySlimVariant(
    val title: String?,
    val option1: String?,
    val option2: String?,
    val option3: String?,
    val price: String?,
    val available: Boolean?,
    val grams: Int?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShopifyPagesResponse(
    val pages: List<ShopifyPage> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShopifyPage(
    val title: String?,
    val handle: String?,
    @JsonProperty("body_html") val bodyHtml: String?,
)