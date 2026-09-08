package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OrderItemDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("productId") val productId: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("quantity") val quantity: Int = 1
)

data class OrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("items") val items: List<OrderItemDto> = emptyList(),
    @SerializedName("total") val total: Double? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("sellerId") val sellerId: String? = null,
    @SerializedName("sellerName") val sellerName: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class OrderCreateDto(
    @SerializedName("productId") val productId: String,
    @SerializedName("quantity") val quantity: Int = 1
)
