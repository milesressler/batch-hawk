package com.batchhawk.common

enum class ScrapedField(val description: String) {
    PRODUCT_NAME("Product name or title as listed on the store"),
    PRODUCT_TYPE("Type of product, e.g. single origin, espresso, blend"),
    ROAST_LEVEL("Roast level, e.g. light, medium, dark"),
    ORIGIN_COUNTRY("Country of origin"),
    ORIGIN_REGION("Region or sub-region of origin"),
    PROCESS("Processing method, e.g. washed, natural, honey"),
    BREW_METHODS("Recommended brew methods"),
    FLAVOR_PROFILE("Tasting notes or flavor descriptors"),
    IS_DECAF("Whether the product is decaffeinated"),
    AVAILABILITY_TYPE("Whether the product is seasonal, limited release, or subscription"),
    DESCRIPTION("Full product description text"),
    PRICE("Current listed price in USD"),
    BAG_SIZE("Bag size in ounces"),
    IN_STOCK("Whether the product is currently in stock"),
}
