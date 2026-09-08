package com.gameora.data.repository

import com.gameora.data.local.SessionManager
import com.gameora.data.local.TokenStore
import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.data.remote.dto.LoginRequestDto
import com.gameora.data.remote.dto.RegisterRequestDto
import com.gameora.domain.model.AuthSession
import com.gameora.domain.model.User

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager
) {

    suspend fun login(emailOrUsername: String, password: String): Result<AuthSession> = safeApi {
        val dto = api.login(LoginRequestDto(email = emailOrUsername, password = password))
        persist(dto)
        dto.toDomain()
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        displayName: String?
    ): Result<AuthSession> = safeApi {
        val dto = api.register(RegisterRequestDto(username, email, password, displayName))
        persist(dto)
        dto.toDomain()
    }

    suspend fun logout(): Result<Unit> {
        val result = safeApi {
            try { api.logout() } catch (_: Throwable) { /* best effort: clear locally anyway */ }
        }
        tokenStore.clear()
        sessionManager.onLoggedOut()
        return result
    }

    /** Refreshes the in-memory + cached current user from the server. */
    suspend fun fetchCurrentUser(): Result<User> = safeApi {
        api.getCurrentUser().toDomain().also { sessionManager.onLoggedIn(it) }
    }

    fun isLoggedIn(): Boolean = tokenStore.hasToken

    private fun persist(dto: com.gameora.data.remote.dto.AuthResponseDto) {
        tokenStore.saveTokens(dto.accessToken ?: dto.token, dto.refreshToken)
        dto.user?.let { sessionManager.onLoggedIn(it.toDomain()) }
    }
}
