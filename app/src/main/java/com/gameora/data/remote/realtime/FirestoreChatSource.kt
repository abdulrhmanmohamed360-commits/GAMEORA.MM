package com.gameora.data.remote.realtime

import com.gameora.domain.model.Conversation
import com.gameora.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * مصدر بيانات Firestore Realtime للمحادثات والرسائل فقط.
 *
 * هذا المصدر للقراءة فقط (read-only): كل الكتابة (إرسال رسالة، إنشاء
 * محادثة، تعليم كمقروء...) لسه بتمر حصريًا عبر [com.gameora.data.remote.api.ApiService]
 * (REST -> Backend -> Admin SDK)، وده بيحقق النقطة المهمة في تعليمات
 * الأمان: قرارات الصلاحيات كلها Server-side، مش معتمدة على التطبيق.
 *
 * الـ Firestore هنا بيُستخدم فقط عشان أي رسالة يكتبها السيرفر تظهر فورًا
 * عند الطرف التاني بدون أي Refresh أو polling - وده معتمد فقط على هوية
 * Firebase Auth الموثّقة (نفس المستخدم المسجّل دخوله بالفعل لكل باقي
 * التطبيق)، وقواعد firestore.rules بتقفل القراءة على المشاركين في
 * المحادثة فقط (participantIds array-contains request.auth.uid).
 */
object FirestoreChatSource {

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    private val currentUid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * قائمة محادثات المستخدم الحالي، تتحدث تلقائيًا فور إنشاء محادثة
     * جديدة أو وصول رسالة تغيّر lastMessage/lastMessageAt/unreadCount.
     */
    fun conversationsFlow(): Flow<List<Conversation>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        /*
         * مهم: مفيش orderBy هنا عمدًا.
         * 1) whereArrayContains + orderBy بيحتاج Composite Index في Firestore؛ لو مش
         *    متعمل، الـ listener بيفشل وكانت الشاشة بتفضل "تحميل" للأبد.
         * 2) orderBy بيستبعد أي محادثة مالهاش حقل lastMessageAt (مثلاً محادثة لسه
         *    اتفتحت ومفيهاش رسايل) - وده كان سبب اختفاء محادثات.
         * الترتيب بيتم هنا في التطبيق بعد الجلب.
         */
        val query = db.collection("conversations")
            .whereArrayContains("participantIds", uid)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // نقفل الـ flow بالخطأ عشان الـ Repository يقدر يرجع لجلب REST بدل ما الشاشة تعلّق.
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                return@addSnapshotListener
            }

            val conversations = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                @Suppress("UNCHECKED_CAST")
                val participantIds = data["participantIds"] as? List<String> ?: emptyList()
                val otherId = participantIds.firstOrNull { it != uid }

                @Suppress("UNCHECKED_CAST")
                val otherNames = data["otherUserNames"] as? Map<String, String?>
                @Suppress("UNCHECKED_CAST")
                val otherAvatars = data["otherUserAvatars"] as? Map<String, String?>
                @Suppress("UNCHECKED_CAST")
                val unreadCounts = data["unreadCounts"] as? Map<String, Number>

                Conversation(
                    id = doc.id,
                    type = data["type"] as? String,
                    otherUserId = otherId,
                    otherUserName = otherId?.let { otherNames?.get(it) },
                    otherUserAvatarUrl = otherId?.let { otherAvatars?.get(it) },
                    lastMessage = data["lastMessage"] as? String,
                    lastMessageAt = toIso(data["lastMessageAt"]),
                    unreadCount = (unreadCounts?.get(uid))?.toInt() ?: 0,
                    productId = data["productId"] as? String,
                    orderId = data["orderId"] as? String,
                    ticketId = data["ticketId"] as? String
                )
            }

            trySend(conversations.sortedByDescending { it.lastMessageAt ?: "" })
        }

        awaitClose { registration.remove() }
    }

    /**
     * رسائل محادثة واحدة، مرتّبة من الأقدم للأحدث، تتحدث تلقائيًا فور
     * إضافة رسالة جديدة من أي طرف (المستخدم نفسه، الطرف الآخر، أو ردّ
     * الإدارة في حالة تذكرة دعم فني).
     */
    fun messagesFlow(conversationId: String): Flow<List<Message>> = callbackFlow {
        val query = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            // بنجيب "أحدث" 500 رسالة (تنازلي) ونعكسها بعد كده؛ الترتيب التصاعدي مع limit
            // كان بيجيب أقدم 500 بس، فأي رسائل جديدة بعد الـ 500 كانت بتختفي.
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(500)

        val registration: ListenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                return@addSnapshotListener
            }

            val messages = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null

                Message(
                    id = doc.id,
                    conversationId = data["conversationId"] as? String,
                    senderId = data["senderId"] as? String,
                    text = data["text"] as? String,
                    createdAt = toIso(data["createdAt"]),
                    status = data["status"] as? String ?: "sent"
                )
            }

            trySend(messages.reversed())
        }

        awaitClose { registration.remove() }
    }

    /** بيقبل النص (ISO) أو Firestore Timestamp، عشان أي حقل تاريخ ما يتحولش لـ null ويختفي. */
    private fun toIso(value: Any?): String? = when (value) {
        is String -> value
        is com.google.firebase.Timestamp -> value.toDate().toInstant().toString()
        is java.util.Date -> value.toInstant().toString()
        else -> null
    }
}
