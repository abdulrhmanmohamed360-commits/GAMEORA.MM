package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProductDto(
    @SerializedName("id") val id: String,
    @SerializedName("gameId") val gameId: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("level") val level: String? = null,
    @SerializedName("rank") val rank: String? = null,
    @SerializedName("server") val server: String? = null,
    @SerializedName("categoryId") val categoryId: String? = null,
    @SerializedName("sellerId") val sellerId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class ProductCreateDto(
    @SerializedName("gameId") val gameId: String,
    @SerializedName("categoryId") val categoryId: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double,
    @SerializedName("currency") val currency: String,
    @SerializedName("images") val images: List<String> = emptyList(),
    @SerializedName("level") val level: String? = null,
    @SerializedName("rank") val rank: String? = null,
    @SerializedName("server") val server: String? = null
)

data class ProductUpdateDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("images") val images: List<String>? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("rank") val rank: String? = null,
    @SerializedName("server") val server: String? = null,
    @SerializedName("status") val status: String? = null
)
