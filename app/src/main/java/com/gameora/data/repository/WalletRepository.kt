package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.Transaction
import com.gameora.domain.model.Wallet
import com.gameora.util.Paged

class WalletRepository(private val api: ApiService) {

    /** Real balance — never hard-coded. Always re-fetched from the server. */
    suspend fun getWallet(): Result<Wallet> = safeApi { api.getWallet().toDomain() }

    suspend fun getTransactions(page: Int, limit: Int): Result<Paged<Transaction>> =
        safeApi {
            val f = mapOf("page" to page.toString(), "limit" to limit.toString())
            api.getWalletTransactions(f).toDomain(page, limit) { it.toDomain() }
        }
}
