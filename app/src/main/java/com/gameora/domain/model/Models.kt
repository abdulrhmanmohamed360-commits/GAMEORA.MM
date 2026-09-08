package com.gameora.domain.model

/**
 * Domain models used by the UI. These are produced exclusively by mapping server DTOs
 * (see com.gameora.data.mapper) — the app never instantiates them with hard-coded content.
 */

data class User(
    val id: String,
    val username: String?,
    val displayName: String?,
    val email: String?,
    val avatarUrl: String?,
    val rating: Double,
    val reviewsCount: Int,
    val verified: Boolean,
    val isSeller: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

data class Game(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val iconUrl: String?,
    val bannerUrl: String?,
    val status: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class Category(
    val id: String,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val imageUrl: String?,
    val status: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class Product(
    val id: String,
    val gameId: String?,
    val title: String,
    val description: String?,
    val price: Double,
    val currency: String?,
    val images: List<String>,
    val level: String?,
    val rank: String?,
    val server: String?,
    val categoryId: String?,
    val sellerId: String?,
    val status: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class Seller(
    val id: String,
    val username: String?,
    val displayName: String?,
    val avatarUrl: String?,
    val rating: Double,
    val reviewsCount: Int,
    val verified: Boolean,
    val createdAt: String?,
    val updatedAt: String?
)

data class Review(
    val id: String,
    val productId: String?,
    val sellerId: String?,
    val authorName: String?,
    val authorAvatarUrl: String?,
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class OrderItem(
    val id: String?,
    val productId: String?,
    val title: String?,
    val imageUrl: String?,
    val price: Double?,
    val currency: String?,
    val quantity: Int
)

enum class OrderStatus(val raw: String) {
    PENDING_SELLER_APPROVAL("PENDING_SELLER_APPROVAL"),
    PROCESSING("PROCESSING"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    REFUNDED("REFUNDED"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromRaw(value: String?): OrderStatus =
            values().firstOrNull { it.raw.equals(value, ignoreCase = true) } ?: UNKNOWN
    }
}

data class Order(
    val id: String,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val total: Double?,
    val currency: String?,
    val sellerId: String?,
    val sellerName: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class Wallet(
    val balance: Double,
    val currency: String?,
    val pendingBalance: Double
)

data class Transaction(
    val id: String,
    val type: String?,
    val amount: Double,
    val currency: String?,
    val description: String?,
    val status: String?,
    val createdAt: String?
)

data class Conversation(
    val id: String,
    val otherUserId: String?,
    val otherUserName: String?,
    val otherUserAvatarUrl: String?,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val unreadCount: Int,
    val productId: String?
)

data class Message(
    val id: String,
    val conversationId: String?,
    val senderId: String?,
    val text: String?,
    val createdAt: String?,
    val status: String?
)

data class AppNotification(
    val id: String,
    val type: String?,
    val title: String?,
    val body: String?,
    val read: Boolean,
    val createdAt: String?
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String?,
    val user: User?
)
