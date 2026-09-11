package com.gameora.data.repository

import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.OfferDto

class OfferRepository(
    private val apiService: ApiService
) {

    suspend fun getOffers(): ApiResult<List<OfferDto>> {
        return try {
            val response = apiService.getOffers()

            if (response.ok) {
                ApiResult.Success(response.offers)
            } else {
                ApiResult.Error("فشل تحميل العروض")
            }
        } catch (e: Exception) {
            ApiResult.Error(
                e.message ?: "حدث خطأ أثناء تحميل العروض"
            )
        }
    }
}
