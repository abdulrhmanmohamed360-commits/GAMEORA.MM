package com.gameora.data.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gameora.domain.model.User

/**
 * Lightweight in-memory session state. The [User] itself always originates from the
 * backend ([com.gameora.data.remote.api.ApiService.getCurrentUser]); this class only
 * caches it for the UI to react to login/logout.
 */
class SessionManager {

    private val _isLoggedIn = MutableLiveData(false)
    val isLoggedIn: LiveData<Boolean> get() = _isLoggedIn

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> get() = _currentUser

    fun onLoggedIn(user: User?) {
        _currentUser.postValue(user)
        _isLoggedIn.postValue(user != null)
    }

    fun onLoggedOut() {
        _currentUser.postValue(null)
        _isLoggedIn.postValue(false)
    }
}
