package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WalletDto(
    @SerializedName("balance") val balance: Double = 0.0,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("pendingBalance") val pendingBalance: Double = 0.0
)

data class TransactionDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String? = null,
    @SerializedName("amount") val amount: Double = 0.0,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null
)
