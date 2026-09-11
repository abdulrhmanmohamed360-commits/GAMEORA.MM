package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OfferDto(
    val id: String,

    val title: String,

    val gameId: String,

    val oldPrice: Double,

    val discountPercent: Double,

    val finalPrice: Double,

    val currency: String,

    val imageUrl: String? = null,

    val description: String? = null,

    val startAt: String? = null,

    val endAt: String? = null,

    val status: String,

    val createdAt: String? = null,

    val updatedAt: String? = null
)
