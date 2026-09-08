package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SellerDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("reviewsCount") val reviewsCount: Int = 0,
    @SerializedName("verified") val verified: Boolean = false,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)
