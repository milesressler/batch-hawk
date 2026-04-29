package com.batchhawk.common

data class ProductUpdateRequest(
    val name: String?,
    val priceInCents: Int?,
    val bagSize: Int?,
    val bagUnit: String?,
    val roastLevel: String?,
    val productType: String?,
    val originCountry: String?,
    val originRegion: String?,
    val process: String?,
    val brewMethods: List<String>?,
    val flavorProfile: List<String>?,
    val isDecaf: Boolean?,
    val availabilityType: String?,
    val description: String?,
    val inStock: Boolean?,
    val productUrl: String?,
)