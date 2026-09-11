package com.gameora

import android.app.Application
import com.gameora.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Creates the single AppContainer and restores the Firebase session
 * when the application starts.
 */
class GameoraApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        instance = this
        container = AppContainer(this)

        restoreSession()
    }

    /**
     * If Firebase still has an authenticated user,
     * AuthRepository synchronizes that user with the backend
     * and updates SessionManager.
     */
    private fun restoreSession() {
        applicationScope.launch {
            container.authRepository.fetchCurrentUser()
        }
    }

    companion object {
        @Volatile
        private lateinit var instance: GameoraApp

        fun get(): GameoraApp = instance
    }
}
