package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.MessageCreateDto
import com.gameora.domain.model.Conversation
import com.gameora.domain.model.Message
import com.gameora.util.Paged
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.time.Instant

/**
 * Chat data layer.
 *
 * - القراءة اللحظية (Realtime): Firestore snapshot listeners (read-only).
 * - الكتابة (إرسال / Mark as read): دايمًا من خلال الـ Backend عبر [ApiService].
 *
 * كل دالة observe* بترجّع [ListenerRegistration] لازم المستدعي يعمل لها remove()
 * (ChatViewModel / ConversationsViewModel بيتحكموا في ده عشان مايحصلش تكرار Listeners).
 */
class ChatRepository(private val api: ApiService) {

    private val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val myUid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    // ------------------------------------------------------------ Realtime

    /** آخر [limit] رسالة في المحادثة، مرتبة من الأقدم للأحدث، وبتتحدث فورًا. */
    fun observeMessages(
        conversationId: String,
        limit: Int,
        onChange: (List<Message>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration =
        firestore.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                onChange(
                    snapshot.documents
                        .map { it.toMessage(conversationId) }
                        .reversed()
                )
            }

    /** قائمة محادثات المستخدم الحالي (آخر رسالة + Unread Count) بتتحدث فورًا. */
    fun observeConversations(
        onChange: (List<Conversation>) -> Unit,
        onError: (Throwable) -> Unit
    ): ListenerRegistration? {
        val uid = myUid ?: return null

        return firestore.collection("conversations")
            .whereArrayContains("participantIds", uid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                onChange(snapshot.documents.map { it.toConversation(uid) })
            }
    }

    // ------------------------------------------------------------- REST

    /** REST conversations (تحميل أول مرة / fallback لو الـ Realtime مش متاح). */
    suspend fun getConversations(): Result<List<Conversation>> =
        safeApi { api.getConversations().map { it.toDomain() } }

    suspend fun getMessages(conversationId: String, page: Int, limit: Int): Result<Paged<Message>> =
        safeApi {
            val f = mapOf("page" to page.toString(), "limit" to limit.toString())
            api.getMessages(conversationId, f).toDomain(page, limit) { it.toDomain() }
        }

    /** الإرسال من خلال الـ Backend. [clientMessageId] بيمنع تكرار الرسالة عند إعادة المحاولة. */
    suspend fun sendMessage(
        conversationId: String,
        text: String,
        clientMessageId: String? = null
    ): Result<Message> =
        safeApi {
            api.sendMessage(conversationId, MessageCreateDto(text, clientMessageId)).toDomain()
        }

    /** Mark as read: بيصفّر Unread Count ويحوّل رسائل الطرف الآخر إلى "read". */
    suspend fun markRead(conversationId: String): Result<Unit> =
        safeApi { api.markConversationRead(conversationId) }

    // ----------------------------------------------------------- Mapping

    private fun DocumentSnapshot.toMessage(conversationId: String) = Message(
        id = id,
        conversationId = getString("conversationId") ?: conversationId,
        senderId = getString("senderId"),
        text = getString("text"),
        createdAt = readTime("createdAt"),
        status = getString("status") ?: "sent"
    )

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toConversation(myId: String): Conversation {
        val participants = (get("participantIds") as? List<*>)
            ?.filterIsInstance<String>()
            .orEmpty()
        val otherId = participants.firstOrNull { it != myId }

        val names = get("otherUserNames") as? Map<String, Any?>
        val avatars = get("otherUserAvatars") as? Map<String, Any?>
        val unread = get("unreadCounts") as? Map<String, Any?>

        return Conversation(
            id = id,
            otherUserId = otherId,
            otherUserName = otherId?.let { names?.get(it) as? String },
            otherUserAvatarUrl = otherId?.let { avatars?.get(it) as? String },
            lastMessage = getString("lastMessage"),
            lastMessageAt = readTime("lastMessageAt"),
            unreadCount = (unread?.get(myId) as? Number)?.toInt() ?: 0,
            productId = getString("productId"),
            orderId = getString("orderId")
        )
    }

    /** الـ Backend بيخزن الوقت كـ ISO string؛ بنتعامل مع Timestamp كمان احتياطيًا. */
    private fun DocumentSnapshot.readTime(field: String): String? =
        when (val v = get(field)) {
            is String -> v
            is Timestamp -> Instant.ofEpochSecond(v.seconds, v.nanoseconds.toLong()).toString()
            else -> null
        }
}
