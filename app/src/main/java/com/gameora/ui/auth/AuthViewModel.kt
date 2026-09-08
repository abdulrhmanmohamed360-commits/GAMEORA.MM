package com.gameora.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.util.UiState
import com.gameora.domain.model.AuthSession
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val repo = GameoraApp.get().container.authRepository

    private val _state = MutableLiveData<UiState<AuthSession>>()
    val state: LiveData<UiState<AuthSession>> = _state

    fun login(emailOrUsername: String, password: String) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.login(emailOrUsername, password).fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it.message ?: "login_failed") }
            )
        }
    }

    fun register(username: String, email: String, password: String, displayName: String?) {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repo.register(username, email, password, displayName).fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it.message ?: "register_failed") }
            )
        }
    }
}
