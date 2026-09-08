package com.gameora.ui.productdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Product
import com.gameora.domain.model.Review
import com.gameora.domain.model.Seller
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class ProductDetailViewModel : ViewModel() {

    private val productRepo = GameoraApp.get().container.productRepository
    private val sellerRepo = GameoraApp.get().container.sellerRepository
    private val reviewRepo = GameoraApp.get().container.reviewRepository
    private val orderRepo = GameoraApp.get().container.orderRepository

    private val _product = MutableLiveData<UiState<Product>>()
    val product: LiveData<UiState<Product>> = _product

    private val _seller = MutableLiveData<Seller?>()
    val seller: LiveData<Seller?> = _seller

    private val _reviews = MutableLiveData<List<Review>>()
    val reviews: LiveData<List<Review>> = _reviews

    private val _buy = MutableLiveData<UiState<Unit>>()
    val buy: LiveData<UiState<Unit>> = _buy

    fun load(id: String) {
        _product.value = UiState.Loading
        viewModelScope.launch {
            productRepo.getProduct(id).fold(
                onSuccess = { p ->
                    _product.value = UiState.Success(p)
                    p.sellerId?.let { loadSeller(it); loadReviews(p.id) }
                },
                onFailure = { _product.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    private fun loadSeller(id: String) =
        viewModelScope.launch { sellerRepo.getSeller(id).onSuccess { _seller.value = it } }

    private fun loadReviews(productId: String) =
        viewModelScope.launch { reviewRepo.getProductReviews(productId).onSuccess { _reviews.value = it } }

    fun buy(productId: String) {
        _buy.value = UiState.Loading
        viewModelScope.launch {
            orderRepo.createOrder(productId).fold(
                onSuccess = { _buy.value = UiState.Success(Unit) },
                onFailure = { _buy.value = UiState.Error(it.message ?: "error") }
            )
        }
    }
}
