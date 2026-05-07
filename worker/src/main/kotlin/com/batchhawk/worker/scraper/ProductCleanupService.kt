package com.batchhawk.worker.scraper

import com.batchhawk.common.ProductUpdateRequest
import com.batchhawk.common.VariantInfo
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ProductCleanupService {

    private val log = LoggerFactory.getLogger(ProductCleanupService::class.java)

    private val subscriptionKeywords = listOf("subscription", "subscribe", "coffee club")
    private val nonCoffeeKeywords = listOf("shirt", "hat", "tote", "sticker", "mug", "tumbler", "merchandise", "merch", "apparel")

    fun clean(products: List<ProductUpdateRequest>): List<ProductUpdateRequest> {
        val cleaned = products.mapNotNull { cleanOne(it) }
        log.info("Cleanup complete: retained {}/{} products", cleaned.size, products.size)
        return cleaned
    }

    private fun cleanOne(product: ProductUpdateRequest): ProductUpdateRequest? {
        val name = product.name

        if (name.isNullOrBlank()) {
            log.info("Dropping product: blank name (url={})", product.productUrl)
            return null
        }

        if (isSubscription(name, product.description, product.availabilityType)) {
            log.info("Dropping '{}': identified as subscription", name)
            return null
        }

        if (isNonCoffee(name)) {
            log.info("Dropping '{}': identified as non-coffee merchandise", name)
            return null
        }

        val cleanedVariants = deduplicateVariants(product.variants, name)

        val hasPrice = cleanedVariants?.any { it.priceInCents != null } == true || product.priceInCents != null
        if (!hasPrice) {
            log.info("Dropping '{}': no usable price data", name)
            return null
        }

        return product.copy(
            roastLevel = product.roastLevel?.blankToNull(),
            originCountry = product.originCountry?.blankToNull(),
            originRegion = product.originRegion?.blankToNull(),
            process = product.process?.blankToNull(),
            description = product.description?.blankToNull(),
            externalProductId = product.externalProductId?.blankToNull(),
            variants = cleanedVariants,
        )
    }

    private fun deduplicateVariants(variants: List<VariantInfo>?, productName: String): List<VariantInfo>? {
        if (variants.isNullOrEmpty()) return variants
        val deduped = variants.distinctBy { Triple(it.bagSize, it.bagUnit, it.priceInCents) }
        val dropped = variants.size - deduped.size
        if (dropped > 0) {
            log.info("  '{}': removed {} duplicate variant(s)", productName, dropped)
        }
        return deduped
    }

    private fun isSubscription(name: String, description: String?, availabilityType: String?): Boolean {
        if (availabilityType?.contains("subscription", ignoreCase = true) == true) return true
        return subscriptionKeywords.any { kw ->
            name.contains(kw, ignoreCase = true) || description?.contains(kw, ignoreCase = true) == true
        }
    }

    private fun isNonCoffee(name: String): Boolean =
        nonCoffeeKeywords.any { kw -> name.contains(kw, ignoreCase = true) }

    private fun String.blankToNull(): String? = takeIf { it.isNotBlank() }
}