package com.gameora.ui.support

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.SupportTicket
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class NewTicketViewModel : ViewModel() {

    private val supportRepo = GameoraApp.get().container.supportRepository

    private val _created = MutableLiveData<UiState<SupportTicket>>()
    val created: LiveData<UiState<SupportTicket>> = _created

    fun submit(subject: String, description: String, category: String?) {
        if (subject.isBlank() || description.isBlank()) {
            _created.value = UiState.Error("subject_and_description_required")
            return
        }

        _created.value = UiState.Loading
        viewModelScope.launch {
            supportRepo.createTicket(subject, description, category?.takeIf { it.isNotBlank() }).fold(
                onSuccess = { _created.value = UiState.Success(it) },
                onFailure = { _created.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
