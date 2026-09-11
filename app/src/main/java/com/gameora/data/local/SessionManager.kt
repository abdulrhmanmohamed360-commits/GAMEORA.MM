package com.gameora.data.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.gameora.domain.model.User

/**
 * Holds the current authentication session state.
 *
 * isLoggedIn:
 * null  = session is being restored
 * true  = user is logged in
 * false = user is logged out
 */
class SessionManager {

    private val _isLoggedIn = MutableLiveData<Boolean?>(null)
    val isLoggedIn: LiveData<Boolean?> get() = _isLoggedIn

    private val _currentUser = MutableLiveData<User?>(null)
    val currentUser: LiveData<User?> get() = _currentUser

    /**
     * Called after successful login or session restoration.
     */
    fun onLoggedIn(user: User?) {
        _currentUser.postValue(user)
        _isLoggedIn.postValue(user != null)
    }

    /**
     * Called when the user explicitly logs out.
     */
    fun onLoggedOut() {
        _currentUser.postValue(null)
        _isLoggedIn.postValue(false)
    }

    /**
     * Marks the session restoration process as finished
     * without an authenticated user.
     */
    fun onSessionRestoreFailed() {
        _currentUser.postValue(null)
        _isLoggedIn.postValue(false)
    }
}
