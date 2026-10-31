package com.gameora.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Message
import com.gameora.util.UiState
import java.time.Instant
import kotlinx.coroutines.launch

/**
 * حالات إرسال محلية للرسالة (لا تُخزَّن على السيرفر) - تُستخدم فقط لعرض
 * "جارٍ الإرسال… / فشل الإرسال" في الوقت الذي يستغرقه طلب الشبكة، قبل
 * ما يستلم Firestore listener الرسالة الحقيقية المؤكدة من السيرفر.
 */
object LocalStatus {
    const val SENDING = "SENDING"
    const val FAILED = "FAILED"
}

class ChatViewModel : ViewModel() {

    private val chatRepo = GameoraApp.get().container.chatRepository
    private val session = GameoraApp.get().container.sessionManager

    private var serverMessages: List<Message> = emptyList()
    private val pendingMessages = mutableListOf<Message>()

    private val _messages = MutableLiveData<UiState<List<Message>>>(UiState.Loading)
    val messages: LiveData<UiState<List<Message>>> = _messages

    private val _sendError = MutableLiveData<String?>()
    val sendError: LiveData<String?> = _sendError

    val currentUserId: String? get() = session.currentUser.value?.id

    fun load(conversationId: String) {
        _messages.value = UiState.Loading

        viewModelScope.launch {
            chatRepo.markAsRead(conversationId)
        }

        viewModelScope.launch {
            chatRepo.observeMessages(conversationId).collect { list ->
                serverMessages = list

                /*
                 * فور ما رسالة معلّقة (SENDING/FAILED) تظهر مؤكدة من السيرفر
                 * (نفس المُرسِل ونفس النص)، نشيلها من القائمة المحلية عشان
                 * ما تتكررش الرسالة في الشاشة.
                 */
                pendingMessages.removeAll { pending ->
                    list.any { it.senderId == pending.senderId && it.text == pending.text }
                }

                publish()
            }
        }
    }

    private fun publish() {
        val combined = (serverMessages + pendingMessages)
            .sortedBy { it.createdAt ?: "" }

        _messages.value = if (combined.isEmpty()) UiState.Empty else UiState.Success(combined)
    }

    fun send(conversationId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val tempId = "pending_${System.currentTimeMillis()}"

        val pending = Message(
            id = tempId,
            conversationId = conversationId,
            senderId = currentUserId,
            text = trimmed,
            createdAt = Instant.now().toString(),
            status = LocalStatus.SENDING
        )

        pendingMessages.add(pending)
        publish()

        dispatch(conversationId, pending)
    }

    /** إعادة إرسال رسالة فشل إرسالها سابقًا، عند الضغط عليها. */
    fun retry(message: Message) {
        val conversationId = message.conversationId ?: return
        updatePending(message.id, LocalStatus.SENDING)
        dispatch(conversationId, message)
    }

    private fun dispatch(conversationId: String, message: Message) {
        viewModelScope.launch {
            chatRepo.sendMessage(conversationId, message.text.orEmpty()).fold(
                onSuccess = {
                    // الرسالة المؤكدة هتوصل عن طريق الـ listener تلقائيًا؛
                    // هنا بس بنشيل حالة "جارٍ الإرسال" لو ما اتشالتش لسه.
                    updatePending(message.id, "SENT")
                },
                onFailure = {
                    updatePending(message.id, LocalStatus.FAILED)
                    _sendError.value = it.message
                }
            )
        }
    }

    private fun updatePending(id: String, status: String) {
        val idx = pendingMessages.indexOfFirst { it.id == id }
        if (idx >= 0) {
            pendingMessages[idx] = pendingMessages[idx].copy(status = status)
            publish()
        }
    }
}
