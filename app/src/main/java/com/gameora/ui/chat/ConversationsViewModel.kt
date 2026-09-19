package com.gameora.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Conversation
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class ConversationsViewModel : ViewModel() {

    private val chatRepo = GameoraApp.get().container.chatRepository

    private val _conversations = MutableLiveData<UiState<List<Conversation>>>(UiState.Loading)
    val conversations: LiveData<UiState<List<Conversation>>> = _conversations

    fun load() {
        _conversations.value = UiState.Loading
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
