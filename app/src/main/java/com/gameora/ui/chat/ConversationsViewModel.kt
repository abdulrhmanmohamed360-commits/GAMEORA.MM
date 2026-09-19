package com.gameora.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Conversation
import com.gameora.util.UiState
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

/**
 * قائمة المحادثات Realtime (آخر رسالة + Unread Count) عن طريق Firestore listener واحد.
 * لو الـ Listener فشل بنرجع لـ REST كـ fallback.
 */
class ConversationsViewModel : ViewModel() {

    private val chatRepo = GameoraApp.get().container.chatRepository

    private val _conversations = MutableLiveData<UiState<List<Conversation>>>(UiState.Loading)
    val conversations: LiveData<UiState<List<Conversation>>> = _conversations

    private var registration: ListenerRegistration? = null

    /** آمن للاستدعاء المتكرر: لو فيه Listener شغال مابيعملش حاجة. */
    fun start() {
        if (registration != null) return

        // مانرجعش لـ Loading لو فيه بيانات معروضة (بيمنع الوميض عند الرجوع للشاشة).
        if (_conversations.value !is UiState.Success) {
            _conversations.value = UiState.Loading
        }

        val reg = chatRepo.observeConversations(
            onChange = { list ->
                _conversations.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            },
            onError = {
                registration = null
                loadViaRest()
            }
        )

        if (reg == null) {
            loadViaRest()
        } else {
            registration = reg
        }
    }

    fun stop() {
        registration?.remove()
        registration = null
    }

    /** زرار Retry في الـ StateView. */
    fun load() {
        stop()
        _conversations.value = UiState.Loading
        start()
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    private fun loadViaRest() {
        viewModelScope.launch {
            chatRepo.getConversations().fold(
                onSuccess = {
                    _conversations.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it)
                },
                onFailure = { _conversations.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
