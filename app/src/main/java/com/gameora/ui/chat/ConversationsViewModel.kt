package com.gameora.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Conversation
import com.gameora.util.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ConversationsViewModel : ViewModel() {

    private val chatRepo = GameoraApp.get().container.chatRepository

    private val _conversations = MutableLiveData<UiState<List<Conversation>>>(UiState.Loading)
    val conversations: LiveData<UiState<List<Conversation>>> = _conversations

    private var loadJob: Job? = null

    /** يبدأ بجلب أولي عبر REST، ثم يتحول تلقائيًا لتحديث مباشر (Firestore listener). */
    fun load() {
        _conversations.value = UiState.Loading

        // نلغي الـ listener القديم قبل ما نبدأ جديد (إعادة المحاولة)، عشان ما يتكررش.
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            chatRepo.observeConversations()
                .catch { e ->
                    _conversations.value =
                        UiState.Error(e.message ?: "تعذّر تحميل المحادثات")
                }
                .collect { list ->
                    _conversations.value =
                        if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                }
        }
    }
}
