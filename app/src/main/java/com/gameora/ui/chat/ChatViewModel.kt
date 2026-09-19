package com.gameora.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Message
import com.gameora.util.UiState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * Realtime chat:
 *  - القراءة: Firestore snapshot listener واحد بس لكل محادثة (start/stop idempotent).
 *  - الإرسال: من خلال الـ Backend مع clientMessageId (بيمنع تكرار الرسالة).
 *  - الرسالة المُرسَلة بتظهر فورًا كـ "sending" وبعدين بتتطابق مع نسخة الـ Firestore بنفس الـ id.
 *  - Mark as read: مرة أول ما المحادثة تفتح، وبعدها كل ما توصل رسالة جديدة من الطرف الآخر.
 */
class ChatViewModel : ViewModel() {

    private val chatRepo = GameoraApp.get().container.chatRepository
    private val session = GameoraApp.get().container.sessionManager

    private val _messages = MutableLiveData<UiState<List<Message>>>(UiState.Loading)
    val messages: LiveData<UiState<List<Message>>> = _messages

    /** رسالة خطأ إرسال لمرة واحدة (الـ Activity بتعمل consumeSendError بعد ما تعرضها). */
    private val _sendError = MutableLiveData<String?>()
    val sendError: LiveData<String?> = _sendError

    /** false = الـ Listener مش شغال (مثلاً Rules لسه ما اتنشرتش) واحنا شغالين على REST. */
    private val _realtimeActive = MutableLiveData(true)
    val realtimeActive: LiveData<Boolean> = _realtimeActive

    val currentUserId: String?
        get() = session.currentUser.value?.id ?: FirebaseAuth.getInstance().currentUser?.uid

    private var conversationId: String? = null
    private var registration: ListenerRegistration? = null
    private var limit = PAGE_SIZE
    private var loadingMore = false
    private var hasMore = true
    private var firstSnapshotHandled = false
    private var markReadInFlight = false

    private var serverMessages: List<Message> = emptyList()

    /** رسائل لسه ما رجعتش من الـ Firestore (sending / failed) — المفتاح هو clientMessageId. */
    private val pending = LinkedHashMap<String, Message>()

    // ------------------------------------------------------------ Lifecycle

    /** آمن للاستدعاء أكتر من مرة: لو نفس المحادثة وفيه Listener شغال مابيعملش حاجة. */
    fun start(cid: String) {
        if (conversationId == cid && registration != null) return

        if (conversationId != cid) {
            resetForConversation(cid)
        }
        attach()
    }

    fun stop() {
        registration?.remove()
        registration = null
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    /** بيحاول تاني يشغّل الـ Realtime بعد ما كان فشل. */
    fun retryRealtime() {
        if (conversationId == null) return
        _realtimeActive.value = true
        attach()
    }

    private fun resetForConversation(cid: String) {
        stop()
        conversationId = cid
        limit = PAGE_SIZE
        loadingMore = false
        hasMore = true
        firstSnapshotHandled = false
        markReadInFlight = false
        serverMessages = emptyList()
        pending.clear()
        _messages.value = UiState.Loading
    }

    private fun attach() {
        val cid = conversationId ?: return
        registration?.remove()
        registration = chatRepo.observeMessages(
            conversationId = cid,
            limit = limit,
            onChange = { list -> onServerMessages(list) },
            onError = { onRealtimeError(it) }
        )
    }

    // ------------------------------------------------------------- Incoming

    private fun onServerMessages(list: List<Message>) {
        _realtimeActive.value = true
        loadingMore = false
        hasMore = list.size >= limit
        serverMessages = list
        publish()
        markReadIfNeeded()
    }

    private fun onRealtimeError(error: Throwable) {
        // الـ Listener بيتقفل تلقائيًا بعد الخطأ.
        registration = null
        loadingMore = false
        _realtimeActive.value = false
        loadViaRest(error)
    }

    private fun loadViaRest(cause: Throwable? = null) {
        val cid = conversationId ?: return
        viewModelScope.launch {
            chatRepo.getMessages(cid, page = 1, limit = limit).fold(
                onSuccess = {
                    serverMessages = it.items
                    publish()
                    markReadIfNeeded()
                },
                onFailure = {
                    if (serverMessages.isEmpty()) {
                        _messages.value = UiState.Error(
                            it.message ?: cause?.message ?: "error"
                        )
                    }
                }
            )
        }
    }

    /** بيدمج رسائل السيرفر مع الرسائل المعلّقة (اللي لسه ما وصلتش) بدون تكرار. */
    private fun publish() {
        val serverIds = HashSet<String>(serverMessages.size)
        serverMessages.forEach { serverIds.add(it.id) }
        pending.keys.removeAll(serverIds)

        val merged = serverMessages + pending.values
        _messages.value = if (merged.isEmpty()) UiState.Empty else UiState.Success(merged)
    }

    // -------------------------------------------------------------- Sending

    fun send(text: String) {
        val cid = conversationId ?: return
        val body = text.trim()
        if (body.isEmpty()) return

        val clientId = UUID.randomUUID().toString()
        pending[clientId] = Message(
            id = clientId,
            conversationId = cid,
            senderId = currentUserId,
            text = body,
            createdAt = Instant.now().toString(),
            status = STATUS_SENDING
        )
        publish()
        dispatch(cid, clientId, body)
    }

    /** إعادة إرسال رسالة فشلت — بنفس الـ id فمستحيل تتكرر عند السيرفر. */
    fun retry(messageId: String) {
        val cid = conversationId ?: return
        val failed = pending[messageId] ?: return
        if (failed.status != STATUS_FAILED) return

        pending[messageId] = failed.copy(status = STATUS_SENDING)
        publish()
        dispatch(cid, messageId, failed.text.orEmpty())
    }

    private fun dispatch(cid: String, clientId: String, body: String) {
        viewModelScope.launch {
            chatRepo.sendMessage(cid, body, clientId).fold(
                onSuccess = { sent ->
                    pending[clientId]?.let {
                        pending[clientId] = it.copy(
                            status = sent.status ?: "sent",
                            createdAt = sent.createdAt ?: it.createdAt
                        )
                    }
                    publish()
                    // في وضع REST مفيش Listener يجيب الرسالة، فنحدّث القائمة بنفسنا.
                    if (registration == null) loadViaRest()
                },
                onFailure = {
                    pending[clientId]?.let { p -> pending[clientId] = p.copy(status = STATUS_FAILED) }
                    publish()
                    _sendError.value = it.message ?: "error"
                }
            )
        }
    }

    fun consumeSendError() {
        _sendError.value = null
    }

    // ------------------------------------------------------------ Pagination

    /** بيحمّل رسائل أقدم (سجل المحادثة) بإعادة تشغيل نفس الـ Listener بحدّ أكبر — من غير Listeners مكررة. */
    fun loadMore() {
        if (registration == null || loadingMore || !hasMore) return
        loadingMore = true
        limit += PAGE_SIZE
        attach()
    }

    // ------------------------------------------------------------ Mark read

    private fun markReadIfNeeded() {
        val cid = conversationId ?: return
        val me = currentUserId ?: return
        if (markReadInFlight) return

        val hasUnreadIncoming =
            serverMessages.any { it.senderId != me && it.status != STATUS_READ }

        // أول Snapshot: بنعمل markRead مرة (عشان الـ Unread Count يتصفّر حتى لو
        // الرسائل غير المقروءة برّه نافذة الرسائل المحمّلة)، وبعدها بس لو فيه جديد.
        if (firstSnapshotHandled && !hasUnreadIncoming) return
        firstSnapshotHandled = true

        markReadInFlight = true
        viewModelScope.launch {
            chatRepo.markRead(cid)
            markReadInFlight = false
        }
    }

    companion object {
        const val STATUS_SENDING = "sending"
        const val STATUS_FAILED = "failed"
        const val STATUS_READ = "read"
        private const val PAGE_SIZE = 50
    }
}
