package com.gameora.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Category
import com.gameora.domain.model.Game
import com.gameora.domain.model.Product
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val gamesRepo = GameoraApp.get().container.gameRepository
    private val categoryRepo = GameoraApp.get().container.categoryRepository
    private val productRepo = GameoraApp.get().container.productRepository

    private val _games = MutableLiveData<UiState<List<Game>>>()
    val games: LiveData<UiState<List<Game>>> = _games

    private val _categories = MutableLiveData<UiState<List<Category>>>()
    val categories: LiveData<UiState<List<Category>>> = _categories

    private val _recent = MutableLiveData<UiState<List<Product>>>()
    val recent: LiveData<UiState<List<Product>>> = _recent

    fun load() {
        loadGames()
        loadCategories()
        loadRecent()
    }

    fun loadGames() {
        _games.value = UiState.Loading
        viewModelScope.launch {
            gamesRepo.getGames().fold(
                onSuccess = { _games.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _games.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            categoryRepo.getCategories().fold(
                onSuccess = { _categories.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _categories.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    fun loadRecent() {
        viewModelScope.launch {
            productRepo.getProducts(emptyMap(), page = 1, limit = 6).fold(
                onSuccess = { _recent.value = if (it.items.isEmpty()) UiState.Empty else UiState.Success(it.items) },
                onFailure = { _recent.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
