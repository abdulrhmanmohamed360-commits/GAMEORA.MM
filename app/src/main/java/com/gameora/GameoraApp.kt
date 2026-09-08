package com.gameora

import android.app.Application
import com.gameora.di.AppContainer

/**
 * Application entry point. Holds the single [AppContainer] (manual DI) that wires
 * the API client, repositories and secure session storage. No data is created here —
 * the app only ever reads data from the backend through the repositories.
 */
class GameoraApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        @Volatile
        private lateinit var instance: GameoraApp

        fun get(): GameoraApp = instance
    }
}
