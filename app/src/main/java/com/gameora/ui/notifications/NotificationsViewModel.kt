package com.gameora.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.AppNotification
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class NotificationsViewModel : ViewModel() {

    private val notifRepo = GameoraApp.get().container.notificationRepository

    private val _notifications = MutableLiveData<UiState<List<AppNotification>>>(UiState.Loading)
    val notifications: LiveData<UiState<List<AppNotification>>> = _notifications

    fun load() {
        _notifications.value = UiState.Loading
        viewModelScope.launch {
            notifRepo.getNotifications(page = 1, limit = 30).fold(
                onSuccess = {
                    _notifications.value = if (it.items.isEmpty()) UiState.Empty else UiState.Success(it.items)
                },
                onFailure = { _notifications.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
