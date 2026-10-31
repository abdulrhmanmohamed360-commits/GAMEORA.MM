package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.Category

class CategoryRepository(private val api: ApiService) {

    suspend fun getCategories(): Result<List<Category>> =
        safeApi { api.getCategories().map { it.toDomain() } }

    suspend fun getCategory(id: String): Result<Category> = safeApi { api.getCategory(id).toDomain() }
}
