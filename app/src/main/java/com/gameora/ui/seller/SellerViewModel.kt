package com.gameora.ui.seller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Review
import com.gameora.domain.model.Seller
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class SellerViewModel : ViewModel() {

    private val sellerRepo = GameoraApp.get().container.sellerRepository
    private val reviewRepo = GameoraApp.get().container.reviewRepository

    private val _seller = MutableLiveData<UiState<Seller>>()
    val seller: LiveData<UiState<Seller>> = _seller

    private val _reviews = MutableLiveData<List<Review>>()
    val reviews: LiveData<List<Review>> = _reviews

    fun load(id: String) {
        _seller.value = UiState.Loading
        viewModelScope.launch {
            sellerRepo.getSeller(id).fold(
                onSuccess = {
                    _seller.value = UiState.Success(it)
                    loadReviews(id)
                },
                onFailure = { _seller.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    private fun loadReviews(id: String) =
        viewModelScope.launch { reviewRepo.getSellerReviews(id).onSuccess { _reviews.value = it } }
}
