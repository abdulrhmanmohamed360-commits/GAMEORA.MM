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

    private val _actionState = MutableLiveData<UiState<Unit>>()
    val actionState: LiveData<UiState<Unit>> = _actionState

    private val _cancelled = MutableLiveData<UiState<Unit>>()
    val cancelled: LiveData<UiState<Unit>> = _cancelled

    fun load(id: String) {
        if (id.isBlank()) {
            _order.value = UiState.Error("رقم الطلب غير صالح")
            return
        }

        _order.value = UiState.Loading

        viewModelScope.launch {
            orderRepo.getOrder(id).fold(
                onSuccess = {
                    _order.value = UiState.Success(it)
                },
                onFailure = {
                    _order.value = UiState.Error(
                        it.message ?: "حدث خطأ أثناء تحميل الطلب"
                    )
                }
            )
        }
    }

    fun approve(id: String) {
        runAction {
            orderRepo.approveOrder(id)
        }
    }

    fun reject(id: String) {
        runAction {
            orderRepo.rejectOrder(id)
        }
    }

    fun deliver(id: String) {
        runAction {
            orderRepo.deliverOrder(id)
        }
    }

    fun confirm(id: String) {
        runAction {
            orderRepo.confirmOrder(id)
        }
    }

    fun dispute(id: String) {
        runAction {
            orderRepo.disputeOrder(id)
        }
    }

    fun cancel(id: String) {
        _cancelled.value = UiState.Loading

        viewModelScope.launch {
            orderRepo.cancelOrder(id).fold(
                onSuccess = {
                    _cancelled.value = UiState.Success(Unit)
                    load(id)
                },
                onFailure = {
                    _cancelled.value = UiState.Error(
                        it.message ?: "حدث خطأ أثناء إلغاء الطلب"
                    )
                }
            )
        }
    }

    private fun runAction(
        action: suspend () -> Result<Order>
    ) {
        _actionState.value = UiState.Loading

        viewModelScope.launch {
            action().fold(
                onSuccess = {
                    _actionState.value = UiState.Success(Unit)

                    val currentOrder = _order.value

                    if (currentOrder is UiState.Success) {
                        load(currentOrder.data.id)
                    }
                },
                onFailure = {
                    _actionState.value = UiState.Error(
                        it.message ?: "حدث خطأ أثناء تنفيذ العملية"
                    )
                }
            )
        }
    }
}
