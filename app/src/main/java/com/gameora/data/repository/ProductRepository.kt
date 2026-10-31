package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.ProductCreateDto
import com.gameora.data.remote.dto.ProductUpdateDto
import com.gameora.domain.model.Product
import com.gameora.util.Paged

class ProductRepository(private val api: ApiService) {

    /**
     * Server-side product list with search + filters + pagination.
     * [filters] may contain: search, gameId, categoryId, priceMin, priceMax,
     * rank, level, server, sellerId, status, sort. The server interprets them.
     */
    suspend fun getProducts(
        filters: Map<String, String>,
        page: Int,
        limit: Int
    ): Result<Paged<Product>> = safeApi {
        val f = filters.toMutableMap().apply {
            put("page", page.toString())
            put("limit", limit.toString())
        }
        api.getProducts(f).toDomain(page, limit) { it.toDomain() }
    }

    suspend fun getProductsByGame(
        gameId: String,
        filters: Map<String, String>,
        page: Int,
        limit: Int
    ): Result<Paged<Product>> = safeApi {
        val f = filters.toMutableMap().apply {
            put("page", page.toString())
            put("limit", limit.toString())
        }
        api.getProductsByGame(gameId, f).toDomain(page, limit) { it.toDomain() }
    }

    suspend fun getProduct(id: String): Result<Product> = safeApi { api.getProduct(id).toDomain() }

    suspend fun createProduct(dto: ProductCreateDto): Result<Product> =
        safeApi { api.createProduct(dto).toDomain() }

    suspend fun updateProduct(id: String, dto: ProductUpdateDto): Result<Product> =
        safeApi { api.updateProduct(id, dto).toDomain() }

    suspend fun deleteProduct(id: String): Result<Unit> = safeApi { api.deleteProduct(id) }
}
