package com.gameora.data.repository

import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.OfferDto

class OfferRepository(
    private val apiService: ApiService
) {

    suspend fun getOffers(): Result<List<OfferDto>> {
        return safeApi {
            val response = apiService.getOffers()

            if (response.ok) {
                response.offers
            } else {
                throw IllegalStateException("فشل تحميل العروض")
            }
        }
    }
}
