package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WalletDepositRequestDto(

    @SerializedName("amount")
    val amount: Double,

    @SerializedName("currency")
    val currency: String = "EGP"
)
