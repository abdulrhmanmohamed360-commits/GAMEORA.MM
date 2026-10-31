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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ChatRepository(private val api: ApiService) {

    /** جلب أولي واحد عبر REST (يُستخدم كحالة بداية قبل ما الـ listener يشتغل). */
    suspend fun getConversations(): Result<List<Conversation>> =
        safeApi { api.getConversations().map { it.toDomain() } }

    /**
     * قائمة المحادثات، تتحدث تلقائيًا (Firestore realtime listener) - بدون polling.
     *
     * 1) بنبدأ بجلب REST فورًا، فالشاشة ما بتفضلش على "تحميل" لو Firestore اتأخر أو فشل.
     * 2) بعد كده بنسمع Firestore ونضم النتيجتين (بالـ id)، فأي محادثة موجودة في السيرفر
     *    بس مش ظاهرة في Firestore ما بتختفيش.
     * 3) لو Firestore فشل (index / صلاحيات) وكان عندنا نتيجة REST، بنكمل بيها بدل ما نطلع خطأ.
     */
    fun observeConversations(): Flow<List<Conversation>> = flow {
        var restList: List<Conversation> = emptyList()
        var restOk = false

        getConversations().onSuccess {
            restList = it
            restOk = true
            emit(it.sortedByDescending { c -> c.lastMessageAt ?: "" })
        }

        emitAll(
            FirestoreChatSource.conversationsFlow()
                .map { live ->
                    val liveIds = live.map { it.id }.toSet()
                    (live + restList.filter { it.id !in liveIds })
                        .sortedByDescending { it.lastMessageAt ?: "" }
                }
                .catch { e -> if (!restOk) throw e }
        )
    }

    suspend fun getMessages(conversationId: String, page: Int, limit: Int): Result<Paged<Message>> =
        safeApi {
            val f = mapOf("page" to page.toString(), "limit" to limit.toString())
            api.getMessages(conversationId, f).toDomain(page, limit) { it.toDomain() }
        }

    /**
     * رسائل محادثة واحدة، تتحدث تلقائيًا (Firestore realtime listener) - بدون polling.
     * لو الـ listener فشل (index / صلاحيات)، بنعرض آخر رسائل من REST بدل شاشة فاضية.
     */
    fun observeMessages(conversationId: String): Flow<List<Message>> =
        FirestoreChatSource.messagesFlow(conversationId)
            .catch {
                getMessages(conversationId, page = 1, limit = 100)
                    .onSuccess { emit(it.items) }
                    .onFailure { e -> throw e }
            }

    suspend fun sendMessage(conversationId: String, text: String): Result<Message> =
        safeApi { api.sendMessage(conversationId, MessageCreateDto(text)).toDomain() }

    /** يبدأ محادثة مباشرة مع مستخدم آخر (مثلاً البائع) أو يرجّع الموجودة مسبقًا. */
    suspend fun startConversation(otherUserId: String, productId: String? = null): Result<Conversation> =
        safeApi { api.startConversation(StartConversationDto(otherUserId, productId)).toDomain() }

    suspend fun markAsRead(conversationId: String): Result<Unit> =
        safeApi { api.markConversationRead(conversationId) }
}
