package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.MessageCreateDto
import com.gameora.domain.model.Conversation
import com.gameora.domain.model.Message
import com.gameora.util.Paged

class ChatRepository(private val api: ApiService) {

    /** REST conversations. For realtime, the backend may expose a WebSocket; this client
     *  polls/refreshes via the same endpoints so messages always originate from the server. */
    suspend fun getConversations(): Result<List<Conversation>> =
        safeApi { api.getConversations().map { it.toDomain() } }

    suspend fun getMessages(conversationId: String, page: Int, limit: Int): Result<Paged<Message>> =
        safeApi {
            val f = mapOf("page" to page.toString(), "limit" to limit.toString())
            api.getMessages(conversationId, f).toDomain(page, limit) { it.toDomain() }
        }

    suspend fun sendMessage(conversationId: String, text: String): Result<Message> =
        safeApi { api.sendMessage(conversationId, MessageCreateDto(text)).toDomain() }
}
