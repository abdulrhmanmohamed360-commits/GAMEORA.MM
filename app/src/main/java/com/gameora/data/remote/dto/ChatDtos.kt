package com.gameora.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ConversationDto(
    @SerializedName("id") val id: String,
    @SerializedName("otherUserId") val otherUserId: String? = null,
    @SerializedName("otherUserName") val otherUserName: String? = null,
    @SerializedName("otherUserAvatarUrl") val otherUserAvatarUrl: String? = null,
    @SerializedName("lastMessage") val lastMessage: String? = null,
    @SerializedName("lastMessageAt") val lastMessageAt: String? = null,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("productId") val productId: String? = null
)

data class MessageDto(
    @SerializedName("id") val id: String,
    @SerializedName("conversationId") val conversationId: String? = null,
    @SerializedName("senderId") val senderId: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("status") val status: String? = null
)

data class MessageCreateDto(
    @SerializedName("text") val text: String
)
