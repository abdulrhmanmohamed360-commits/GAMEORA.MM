package com.gameora.data.repository

import com.gameora.data.mapper.toDomain
import com.gameora.data.remote.api.ApiService
import com.gameora.domain.model.Game

class GameRepository(private val api: ApiService) {

    suspend fun getGames(): Result<List<Game>> = safeApi { api.getGames().map { it.toDomain() } }

    suspend fun getGame(id: String): Result<Game> = safeApi { api.getGame(id).toDomain() }
}
