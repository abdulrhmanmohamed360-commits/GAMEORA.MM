package com.gameora.ui.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Order
import com.gameora.util.Paged
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class OrdersViewModel : ViewModel() {

    private val orderRepo = GameoraApp.get().container.orderRepository

    private val _orders = MutableLiveData<UiState<List<Order>>>(UiState.Loading)
    val orders: LiveData<UiState<List<Order>>> = _orders

    private val _loadingMore = MutableLiveData(false)
    val loadingMore: LiveData<Boolean> = _loadingMore

    private var page = 1
    private var canLoadMore = true
    private val limit = 20

    fun loadFirst() {
        page = 1; canLoadMore = true; load(reset = true)
    }

    fun loadMore() {
        if (_loadingMore.value == true || !canLoadMore) return
        page++; load(reset = false)
    }

    private fun load(reset: Boolean) {
        if (reset) _orders.value = UiState.Loading else _loadingMore.value = true
        viewModelScope.launch {
            orderRepo.getOrders(page, limit).fold(
                onSuccess = { paged: Paged<Order> ->
                    _loadingMore.value = false
                    canLoadMore = paged.hasMore
                    _orders.value = if (reset) {
                        if (paged.items.isEmpty()) UiState.Empty else UiState.Success(paged.items)
                    } else {
                        val current = (_orders.value as? UiState.Success)?.data.orEmpty()
                        UiState.Success(current + paged.items)
                    }
                },
                onFailure = {
                    _loadingMore.value = false
                    if (reset) _orders.value = UiState.Error(it.message ?: "error")
                }
            )
        }
    }
}
