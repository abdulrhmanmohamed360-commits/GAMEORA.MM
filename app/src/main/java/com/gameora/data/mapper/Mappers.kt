package com.gameora.data.mapper

import com.gameora.data.remote.dto.AuthResponseDto
import com.gameora.data.remote.dto.CategoryDto
import com.gameora.data.remote.dto.ConversationDto
import com.gameora.data.remote.dto.GameDto
import com.gameora.data.remote.dto.MessageDto
import com.gameora.data.remote.dto.NotificationDto
import com.gameora.data.remote.dto.OrderDto
import com.gameora.data.remote.dto.OrderItemDto
import com.gameora.data.remote.dto.PaginatedDto
import com.gameora.data.remote.dto.ProductDto
import com.gameora.data.remote.dto.ReviewDto
import com.gameora.data.remote.dto.SellerDto
import com.gameora.data.remote.dto.TransactionDto
import com.gameora.data.remote.dto.UserDto
import com.gameora.data.remote.dto.WalletDto
import com.gameora.domain.model.AppNotification
import com.gameora.domain.model.AuthSession
import com.gameora.domain.model.Category
import com.gameora.domain.model.Conversation
import com.gameora.domain.model.Game
import com.gameora.domain.model.Message
import com.gameora.domain.model.Order
import com.gameora.domain.model.OrderItem
import com.gameora.domain.model.OrderStatus
import com.gameora.domain.model.Product
import com.gameora.domain.model.Review
import com.gameora.domain.model.Seller
import com.gameora.domain.model.Transaction
import com.gameora.domain.model.User
import com.gameora.domain.model.Wallet
import com.gameora.util.Paged
import kotlin.math.ceil

/** DTO -> domain mappers. The only place where server types are translated to UI types. */

fun UserDto.toDomain() = User(
    id, username, displayName, email, avatarUrl,
    rating, reviewsCount, verified, isSeller, createdAt, updatedAt
)

fun GameDto.toDomain() = Game(
    id, name, description, imageUrl, iconUrl, bannerUrl, status, createdAt, updatedAt
)

fun CategoryDto.toDomain() = Category(
    id, name, description, iconUrl, imageUrl, status, createdAt, updatedAt
)

fun ProductDto.toDomain() = Product(
    id, gameId, title, description, price, currency, images,
    level, rank, server, categoryId, sellerId, status, createdAt, updatedAt
)

fun SellerDto.toDomain() = Seller(
    id, username, displayName, avatarUrl, rating, reviewsCount, verified, createdAt, updatedAt
)

fun ReviewDto.toDomain() = Review(
    id, productId, sellerId, authorName, authorAvatarUrl, rating, comment, createdAt, updatedAt
)

fun OrderItemDto.toDomain() = OrderItem(id, productId, title, imageUrl, price, currency, quantity)

fun OrderDto.toDomain() = Order(
    id,
    OrderStatus.fromRaw(status),
    items.map { it.toDomain() },
    total, currency, sellerId, sellerName, createdAt, updatedAt
)

fun WalletDto.toDomain() = Wallet(balance, currency, pendingBalance)

fun TransactionDto.toDomain() = Transaction(
    id, type, amount, currency, description, status, createdAt
)

fun ConversationDto.toDomain() = Conversation(
    id, otherUserId, otherUserName, otherUserAvatarUrl,
    lastMessage, lastMessageAt, unreadCount, productId
)

fun MessageDto.toDomain() = Message(id, conversationId, senderId, text, createdAt, status)

fun NotificationDto.toDomain() = AppNotification(id, type, title, body, read, createdAt)

fun AuthResponseDto.toDomain(): AuthSession {
    val token = accessToken ?: token ?: ""
    return AuthSession(token, refreshToken, user?.toDomain())
}

/**
 * Maps a generic paginated envelope to a [Paged] of domain items, tolerating servers
 * that use `items` or `data` and that may omit page metadata.
 */
fun <T, R> PaginatedDto<T>.toDomain(currentPage: Int, limit: Int, mapper: (T) -> R): Paged<R> {
    val rawItems = if (items.isNotEmpty()) items else (data ?: emptyList())
    val mapped = rawItems.map(mapper)

    val totalPages: Int = when {
        totalPages != null -> totalPages
        total != null -> ceil(total.toDouble() / limit.coerceAtLeast(1)).toInt()
        else -> currentPage
    }

    val hasMore: Boolean = when {
        hasMore != null -> hasMore
        nextPage != null -> true
        totalPages > currentPage -> true
        else -> rawItems.size >= limit
    }

    return Paged(mapped, currentPage, totalPages, hasMore)
}
