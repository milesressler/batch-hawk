package com.batchhawk.worker.scraper.shopify

import com.batchhawk.common.ProductUpdateRequest

data class ProductExtractionResult(val products: List<ProductUpdateRequest>)