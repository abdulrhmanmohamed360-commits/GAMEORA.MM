package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WalletDepositResponseDto(

    @SerializedName("depositId")
    val depositId: String,

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("currency")
    val currency: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("intentionId")
    val intentionId: String?,

    @SerializedName("paymentUrl")
    val paymentUrl: String?
)
