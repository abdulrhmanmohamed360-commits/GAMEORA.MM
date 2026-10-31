package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.AppNotification
import com.gameora.util.Paged

class NotificationRepository(private val api: ApiService) {

    suspend fun getNotifications(page: Int, limit: Int): Result<Paged<AppNotification>> =
        safeApi {
            val f = mapOf("page" to page.toString(), "limit" to limit.toString())
            api.getNotifications(f).toDomain(page, limit) { it.toDomain() }
        }
}
