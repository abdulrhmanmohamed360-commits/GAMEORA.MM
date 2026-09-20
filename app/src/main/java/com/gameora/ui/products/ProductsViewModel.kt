package com.gameora.ui.products

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.config.ApiConfig
import com.gameora.domain.model.Category
import com.gameora.domain.model.Game
import com.gameora.domain.model.Product
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class ProductsViewModel : ViewModel() {

    private val productRepo = GameoraApp.get().container.productRepository
    private val gameRepo = GameoraApp.get().container.gameRepository
    private val categoryRepo = GameoraApp.get().container.categoryRepository

    private val _products = MutableLiveData<UiState<List<Product>>>(UiState.Loading)
    val products: LiveData<UiState<List<Product>>> = _products

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    private val _games = MutableLiveData<List<Game>>()
    val games: LiveData<List<Game>> = _games

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val filters = mutableMapOf<String, String>()
    private var page = 1
    private var canLoadMore = true
    private val limit = ApiConfig.DEFAULT_PAGE_LIMIT

    fun init(gameId: String?) {
        if (!gameId.isNullOrBlank()) filters["gameId"] = gameId
    }

    fun setQuery(query: String?) {
        if (query.isNullOrBlank()) filters.remove("search") else filters["search"] = query
    }

    fun applyFilters(newFilters: Map<String, String>) {
        filters.clear()
        filters.putAll(newFilters)
        loadFirst()
    }

    fun clearFilters(keepQuery: Boolean) {
        val q = if (keepQuery) filters["search"] else null
        filters.clear()
        if (q != null) filters["search"] = q
        loadFirst()
    }

    fun loadFirst() {
        page = 1
        canLoadMore = true
        load(reset = true)
    }

    fun loadMore() {
        if (_loadingMore.value == true || !canLoadMore) return
        page++
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        if (reset) _products.value = UiState.Loading else _loadingMore.value = true
        viewModelScope.launch {
            productRepo.getProducts(filters, page, limit).fold(
                onSuccess = { paged ->
                    _loadingMore.value = false
                    canLoadMore = paged.hasMore
                    _products.value = if (reset) {
                        if (paged.items.isEmpty()) UiState.Empty else UiState.Success(paged.items)
                    } else {
                        val current = (_products.value as? UiState.Success)?.data.orEmpty()
                        UiState.Success(current + paged.items)
                    }
                },
                onFailure = {
                    _loadingMore.value = false
                    if (reset) _products.value = UiState.Error(it.message ?: "error")
                }
            )
        }
    }

    fun loadFilterData() {
        viewModelScope.launch { gameRepo.getGames().onSuccess { _games.value = it } }
        viewModelScope.launch { categoryRepo.getCategories().onSuccess { _categories.value = it } }
    }
}
