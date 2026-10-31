package com.gameora.ui.sell

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.data.remote.dto.ProductCreateDto
import com.gameora.data.remote.dto.ProductUpdateDto
import com.gameora.domain.model.Category
import com.gameora.domain.model.Game
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class SellViewModel : ViewModel() {

    private val productRepo = GameoraApp.get().container.productRepository
    private val gameRepo = GameoraApp.get().container.gameRepository
    private val categoryRepo = GameoraApp.get().container.categoryRepository

    private val _games = MutableLiveData<List<Game>>()
    val games: LiveData<List<Game>> = _games

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _result = MutableLiveData<UiState<Unit>>()
    val result: LiveData<UiState<Unit>> = _result

    fun loadLookups() {
        viewModelScope.launch { gameRepo.getGames().onSuccess { _games.value = it } }
        viewModelScope.launch { categoryRepo.getCategories().onSuccess { _categories.value = it } }
    }

    fun create(dto: ProductCreateDto) {
        _result.value = UiState.Loading
        viewModelScope.launch {
            productRepo.createProduct(dto).fold(
                onSuccess = { _result.value = UiState.Success(Unit) },
                onFailure = { _result.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    fun update(id: String, dto: ProductUpdateDto) {
        _result.value = UiState.Loading
        viewModelScope.launch {
            productRepo.updateProduct(id, dto).fold(
                onSuccess = { _result.value = UiState.Success(Unit) },
                onFailure = { _result.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    fun delete(id: String) {
        _result.value = UiState.Loading
        viewModelScope.launch {
            productRepo.deleteProduct(id).fold(
                onSuccess = { _result.value = UiState.Success(Unit) },
                onFailure = { _result.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
