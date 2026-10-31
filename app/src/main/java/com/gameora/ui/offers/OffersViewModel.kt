package com.gameora.ui.offers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.data.remote.dto.OfferDto
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class OffersViewModel : ViewModel() {

    private val offerRepo = GameoraApp.get().container.offerRepository

    private val _offers = MutableLiveData<UiState<List<OfferDto>>>()
    val offers: LiveData<UiState<List<OfferDto>>> = _offers

    fun load() {
        _offers.value = UiState.Loading

        viewModelScope.launch {
            offerRepo.getOffers().fold(
                onSuccess = {
                    _offers.value =
                        if (it.isEmpty()) {
                            UiState.Empty
                        } else {
                            UiState.Success(it)
                        }
                },
                onFailure = {
                    _offers.value =
                        UiState.Error(it.message ?: "حدث خطأ أثناء تحميل العروض")
                }
            )
        }
    }
}
