package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.OrderCreateDto
import com.gameora.domain.model.Order
import com.gameora.util.Paged

class OrderRepository(private val api: ApiService) {

    suspend fun getOrders(page: Int, limit: Int, status: String? = null): Result<Paged<Order>> =
        safeApi {
            val f = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
            if (!status.isNullOrBlank()) f["status"] = status
            api.getOrders(f).toDomain(page, limit) { it.toDomain() }
        }

    suspend fun getOrder(id: String): Result<Order> = safeApi { api.getOrder(id).toDomain() }

    /** Server is the source of truth for the order — it validates product availability,
     *  re-prices, checks the buyer/balance and returns the created order. */
    suspend fun createOrder(productId: String, quantity: Int = 1): Result<Order> =
        safeApi { api.createOrder(OrderCreateDto(productId, quantity)).toDomain() }

    suspend fun cancelOrder(id: String): Result<Order> = safeApi { api.cancelOrder(id).toDomain() }
}
