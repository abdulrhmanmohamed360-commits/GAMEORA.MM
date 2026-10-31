package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.ReviewCreateDto
import com.gameora.domain.model.Review

class ReviewRepository(private val api: ApiService) {

    suspend fun getProductReviews(productId: String): Result<List<Review>> =
        safeApi { api.getProductReviews(productId).map { it.toDomain() } }

    suspend fun getSellerReviews(sellerId: String): Result<List<Review>> =
        safeApi { api.getSellerReviews(sellerId).map { it.toDomain() } }

    suspend fun createReview(
        rating: Int,
        comment: String,
        productId: String? = null,
        sellerId: String? = null
    ): Result<Review> = safeApi {
        api.createReview(ReviewCreateDto(productId, sellerId, rating, comment)).toDomain()
    }
}
