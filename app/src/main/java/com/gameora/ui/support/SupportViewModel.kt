package com.gameora.ui.support

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.SupportTicket
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class SupportViewModel : ViewModel() {

    private val supportRepo = GameoraApp.get().container.supportRepository

    private val _tickets = MutableLiveData<UiState<List<SupportTicket>>>(UiState.Loading)
    val tickets: LiveData<UiState<List<SupportTicket>>> = _tickets

    fun load() {
        _tickets.value = UiState.Loading
        viewModelScope.launch {
            supportRepo.getTickets().fold(
                onSuccess = { list ->
                    _tickets.value = if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                },
                onFailure = { _tickets.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
