package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SupportTicketDto(
    @SerializedName("ticketId") val ticketId: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("userName") val userName: String? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("status") val status: String = "OPEN",
    @SerializedName("priority") val priority: String = "NORMAL",
    @SerializedName("conversationId") val conversationId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class SupportTicketCreateDto(
    @SerializedName("subject") val subject: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String? = null
)
