package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.Seller

class SellerRepository(private val api: ApiService) {

    suspend fun getSeller(id: String): Result<Seller> = safeApi { api.getSeller(id).toDomain() }
}
