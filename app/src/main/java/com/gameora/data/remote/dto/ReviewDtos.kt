package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReviewDto(
    @SerializedName("id") val id: String,
    @SerializedName("productId") val productId: String? = null,
    @SerializedName("sellerId") val sellerId: String? = null,
    @SerializedName("authorName") val authorName: String? = null,
    @SerializedName("authorAvatarUrl") val authorAvatarUrl: String? = null,
    @SerializedName("rating") val rating: Int = 0,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class ReviewCreateDto(
    @SerializedName("productId") val productId: String? = null,
    @SerializedName("sellerId") val sellerId: String? = null,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String
)
