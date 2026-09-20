package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.MessageCreateDto
import com.gameora.data.remote.dto.StartConversationDto
import com.gameora.data.remote.realtime.FirestoreChatSource
import com.gameora.domain.model.Conversation
import com.gameora.domain.model.Message
import com.gameora.util.Paged
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val api: ApiService) {

    /** جلب أولي واحد عبر REST (يُستخدم كحالة بداية قبل ما الـ listener يشتغل). */
    suspend fun getConversations(): Result<List<Conversation>> =
        safeApi { api.getConversations().map { it.toDomain() } }

    /** قائمة المحادثات، تتحدث تلقائيًا (Firestore realtime listener) - بدون polling. */
    fun observeConversations(): Flow<List<Conversation>> =
        FirestoreChatSource.conversationsFlow()

    suspend fun getMessages(conversationId: String, page: Int, limit: Int): Result<Paged<Message>> =
        safeApi {
            val f = mapOf("page" to page.toString(), "limit" to limit.toString())
            api.getMessages(conversationId, f).toDomain(page, limit) { it.toDomain() }
        }

    /** رسائل محادثة واحدة، تتحدث تلقائيًا (Firestore realtime listener) - بدون polling. */
    fun observeMessages(conversationId: String): Flow<List<Message>> =
        FirestoreChatSource.messagesFlow(conversationId)

    suspend fun sendMessage(conversationId: String, text: String): Result<Message> =
        safeApi { api.sendMessage(conversationId, MessageCreateDto(text)).toDomain() }

    /** يبدأ محادثة مباشرة مع مستخدم آخر (مثلاً البائع) أو يرجّع الموجودة مسبقًا. */
    suspend fun startConversation(otherUserId: String, productId: String? = null): Result<Conversation> =
        safeApi { api.startConversation(StartConversationDto(otherUserId, productId)).toDomain() }

    suspend fun markAsRead(conversationId: String): Result<Unit> =
        safeApi { api.markConversationRead(conversationId) }
}
