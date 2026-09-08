package com.gameora.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.User
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val userRepo = GameoraApp.get().container.userRepository
    private val authRepo = GameoraApp.get().container.authRepository

    private val _user = MutableLiveData<UiState<User>>()
    val user: LiveData<UiState<User>> = _user

    private val _loggedOut = MutableLiveData(false)
    val loggedOut: LiveData<Boolean> = _loggedOut

    fun load() {
        _user.value = UiState.Loading
        viewModelScope.launch {
            userRepo.getCurrentUser().fold(
                onSuccess = { _user.value = UiState.Success(it) },
                onFailure = { _user.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _loggedOut.postValue(true)
        }
    }
}
