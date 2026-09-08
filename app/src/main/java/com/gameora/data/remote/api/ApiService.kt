package com.gameora.data.remote.api

import com.gameora.data.remote.dto.AuthResponseDto
import com.gameora.data.remote.dto.CategoryDto
import com.gameora.data.remote.dto.ConversationDto
import com.gameora.data.remote.dto.GameDto
import com.gameora.data.remote.dto.LoginRequestDto
import com.gameora.data.remote.dto.MessageCreateDto
import com.gameora.data.remote.dto.MessageDto
import com.gameora.data.remote.dto.NotificationDto
import com.gameora.data.remote.dto.OrderCreateDto
import com.gameora.data.remote.dto.OrderDto
import com.gameora.data.remote.dto.PaginatedDto
import com.gameora.data.remote.dto.ProductCreateDto
import com.gameora.data.remote.dto.ProductDto
import com.gameora.data.remote.dto.ProductUpdateDto
import com.gameora.data.remote.dto.RegisterRequestDto
import com.gameora.data.remote.dto.ReviewCreateDto
import com.gameora.data.remote.dto.ReviewDto
import com.gameora.data.remote.dto.SellerDto
import com.gameora.data.remote.dto.TransactionDto
import com.gameora.data.remote.dto.UserDto
import com.gameora.data.remote.dto.WalletDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * The complete server contract. Every piece of content the user sees flows through
 * one of these endpoints. The app contains no local catalogue, no fallback lists,
 * and no hard-coded products — if the server returns nothing, the UI shows Empty/Error.
 *
 * Paths are relative to [com.gameora.config.ApiConfig.API_BASE_URL].
 */
interface ApiService {

    // ----------------------------------------------------------------- Auth
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("auth/logout")
    suspend fun logout()

    @GET("auth/me")
    suspend fun getMe(): UserDto

    // ------------------------------------------------------------- Current user
    @GET("users/me")
    suspend fun getCurrentUser(): UserDto

    // ----------------------------------------------------------------- Games
    @GET("games")
    suspend fun getGames(): List<GameDto>

    @GET("games/{id}")
    suspend fun getGame(@Path("id") id: String): GameDto

    // ------------------------------------------------------------ Categories
    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("categories/{id}")
    suspend fun getCategory(@Path("id") id: String): CategoryDto

    // ------------------------------------------------------------- Products
    @GET("products")
    suspend fun getProducts(@QueryMap filters: Map<String, String>): PaginatedDto<ProductDto>

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: String): ProductDto

    @GET("games/{gameId}/products")
    suspend fun getProductsByGame(
        @Path("gameId") gameId: String,
        @QueryMap filters: Map<String, String>
    ): PaginatedDto<ProductDto>

    @POST("products")
    suspend fun createProduct(@Body body: ProductCreateDto): ProductDto

    @PATCH("products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body body: ProductUpdateDto
    ): ProductDto

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: String)

    // -------------------------------------------------------------- Sellers
    @GET("sellers/{id}")
    suspend fun getSeller(@Path("id") id: String): SellerDto

    // --------------------------------------------------------------- Reviews
    @GET("products/{id}/reviews")
    suspend fun getProductReviews(@Path("id") id: String): List<ReviewDto>

    @GET("sellers/{id}/reviews")
    suspend fun getSellerReviews(@Path("id") id: String): List<ReviewDto>

    @POST("reviews")
    suspend fun createReview(@Body body: ReviewCreateDto): ReviewDto

    // ---------------------------------------------------------------- Orders
    @GET("orders")
    suspend fun getOrders(@QueryMap filters: Map<String, String>): PaginatedDto<OrderDto>

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: String): OrderDto

    @POST("orders")
    suspend fun createOrder(@Body body: OrderCreateDto): OrderDto

    @POST("orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String): OrderDto

    // ---------------------------------------------------------------- Wallet
    @GET("wallet")
    suspend fun getWallet(): WalletDto

    @GET("wallet/transactions")
    suspend fun getWalletTransactions(@QueryMap filters: Map<String, String>): PaginatedDto<TransactionDto>

    // ----------------------------------------------------------------- Chat
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") id: String,
        @QueryMap filters: Map<String, String>
    ): PaginatedDto<MessageDto>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: String,
        @Body body: MessageCreateDto
    ): MessageDto

    // --------------------------------------------------------- Notifications
    @GET("notifications")
    suspend fun getNotifications(@QueryMap filters: Map<String, String>): PaginatedDto<NotificationDto>
}
