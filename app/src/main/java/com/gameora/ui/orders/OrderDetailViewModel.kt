package com.gameora.ui.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Order
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class OrderDetailViewModel : ViewModel() {

    private val orderRepo = GameoraApp.get().container.orderRepository

    private val _order = MutableLiveData<UiState<Order>>()
    val order: LiveData<UiState<Order>> = _order

    private val _cancelled = MutableLiveData<UiState<Unit>>()
    val cancelled: LiveData<UiState<Unit>> = _cancelled

    fun load(id: String) {
        _order.value = UiState.Loading
        viewModelScope.launch {
            orderRepo.getOrder(id).fold(
                onSuccess = { _order.value = UiState.Success(it) },
                onFailure = { _order.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    fun cancel(id: String) {
        _cancelled.value = UiState.Loading
        viewModelScope.launch {
            orderRepo.cancelOrder(id).fold(
                onSuccess = { _cancelled.value = UiState.Success(Unit); load(id) },
                onFailure = { _cancelled.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
