package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.WalletDepositRequestDto
import com.gameora.domain.model.Transaction
import com.gameora.domain.model.Wallet
import com.gameora.util.Paged

class WalletRepository(
    private val api: ApiService
) {

    /** Real balance — never hard-coded. Always re-fetched from the server. */
    suspend fun getWallet(): Result<Wallet> =
        safeApi {
            api.getWallet().toDomain()
        }

    suspend fun getTransactions(
        page: Int,
        limit: Int
    ): Result<Paged<Transaction>> =
        safeApi {
            val filters = mapOf(
                "page" to page.toString(),
                "limit" to limit.toString()
            )

            api.getWalletTransactions(filters)
                .toDomain(page, limit) {
                    it.toDomain()
                }
        }

    /**
     * Creates a pending wallet deposit on the backend.
     *
     * The backend creates the Paymob intention and returns
     * the real checkout URL.
     *
     * The wallet is NOT credited here.
     * It is credited only after Paymob confirms payment
     * through the backend webhook.
     */
    suspend fun createDeposit(
        amount: Double
    ): Result<com.gameora.data.remote.dto.WalletDepositResponseDto> =
        safeApi {
            api.createWalletDeposit(
                WalletDepositRequestDto(
                    amount = amount,
                    currency = "EGP"
                )
            )
        }
}
