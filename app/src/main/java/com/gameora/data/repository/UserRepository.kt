package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.User

class UserRepository(private val api: ApiService) {

    suspend fun getCurrentUser(): Result<User> = safeApi { api.getCurrentUser().toDomain() }
}
