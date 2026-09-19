package com.gameora.di

import android.content.Context
import com.gameora.data.local.SessionManager
import com.gameora.data.local.TokenStore
import com.gameora.data.remote.api.ApiClient
import com.gameora.data.remote.api.ApiService
import com.gameora.data.repository.AuthRepository
import com.gameora.data.repository.CategoryRepository
import com.gameora.data.repository.ChatRepository
import com.gameora.data.repository.GameRepository
import com.gameora.data.repository.NotificationRepository
import com.gameora.data.repository.OfferRepository
import com.gameora.data.repository.OrderRepository
import com.gameora.data.repository.ProductRepository
import com.gameora.data.repository.ReviewRepository
import com.gameora.data.repository.SellerRepository
import com.gameora.data.repository.UserRepository
import com.gameora.data.repository.WalletRepository

/**
 * Manual dependency container created once in [com.gameora.GameoraApp].
 * Provides the API service and a single instance of every repository.
 */
class AppContainer(context: Context) {

    val tokenStore: TokenStore = TokenStore(context)
    val sessionManager: SessionManager = SessionManager()

    val apiService: ApiService = ApiClient.create(tokenStore)

    val authRepository: AuthRepository =
        AuthRepository(apiService, tokenStore, sessionManager)

    val userRepository: UserRepository =
        UserRepository(apiService)

    val gameRepository: GameRepository =
        GameRepository(apiService)

    val categoryRepository: CategoryRepository =
        CategoryRepository(apiService)

    val productRepository: ProductRepository =
        ProductRepository(apiService)

    val sellerRepository: SellerRepository =
        SellerRepository(apiService)

    val reviewRepository: ReviewRepository =
        ReviewRepository(apiService)

    val orderRepository: OrderRepository =
        OrderRepository(apiService)

    val walletRepository: WalletRepository =
        WalletRepository(apiService)

    val chatRepository: ChatRepository =
        ChatRepository(apiService)

    val notificationRepository: NotificationRepository =
        NotificationRepository(apiService)

    // ---------------------------------------------------------------- Offers

    val offerRepository: OfferRepository =
        OfferRepository(apiService)
}
