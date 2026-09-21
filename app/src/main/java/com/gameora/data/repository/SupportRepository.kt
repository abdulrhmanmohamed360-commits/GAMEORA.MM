package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.SupportTicketCreateDto
import com.gameora.domain.model.SupportTicket

class SupportRepository(private val api: ApiService) {

    suspend fun getTickets(): Result<List<SupportTicket>> =
        safeApi { api.getSupportTickets().map { it.toDomain() } }

    suspend fun getTicket(id: String): Result<SupportTicket> =
        safeApi { api.getSupportTicket(id).toDomain() }

    suspend fun createTicket(
        subject: String,
        description: String,
        category: String? = null
    ): Result<SupportTicket> =
        safeApi {
            api.createSupportTicket(
                SupportTicketCreateDto(subject, description, category)
            ).toDomain()
        }
}
