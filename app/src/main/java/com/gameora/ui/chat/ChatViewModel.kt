package com.gameora.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Message
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepo = GameoraApp.get().container.chatRepository
    private val session = GameoraApp.get().container.sessionManager

    private val _messages = MutableLiveData<UiState<List<Message>>>(UiState.Loading)
    val messages: LiveData<UiState<List<Message>>> = _messages

    private val _sent = MutableLiveData<UiState<Message>>()
    val sent: LiveData<UiState<Message>> = _sent

    val currentUserId: String? get() = session.currentUser.value?.id

    fun load(conversationId: String) {
        _messages.value = UiState.Loading
        viewModelScope.launch {
            chatRepo.getMessages(conversationId, page = 1, limit = 50).fold(
                onSuccess = {
                    _messages.value = if (it.items.isEmpty()) UiState.Empty else UiState.Success(it.items)
                },
                onFailure = { _messages.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    fun send(conversationId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            chatRepo.sendMessage(conversationId, text).fold(
                onSuccess = { msg ->
                    val current = (_messages.value as? UiState.Success)?.data.orEmpty()
                    _messages.value = UiState.Success(current + msg)
                    _sent.value = UiState.Success(msg)
                },
                onFailure = { _sent.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
