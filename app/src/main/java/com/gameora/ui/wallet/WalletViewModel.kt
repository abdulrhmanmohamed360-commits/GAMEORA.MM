package com.gameora.ui.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gameora.GameoraApp
import com.gameora.domain.model.Transaction
import com.gameora.domain.model.Wallet
import com.gameora.util.UiState
import kotlinx.coroutines.launch

class WalletViewModel : ViewModel() {

    private val walletRepo = GameoraApp.get().container.walletRepository

    private val _wallet = MutableLiveData<UiState<Wallet>>()
    val wallet: LiveData<UiState<Wallet>> = _wallet

    private val _txs = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _txs

    fun load() {
        _wallet.value = UiState.Loading
        viewModelScope.launch {
            walletRepo.getWallet().fold(
                onSuccess = {
                    _wallet.value = UiState.Success(it)
                    loadTransactions()
                },
                onFailure = { _wallet.value = UiState.Error(it.message ?: "error") }
            )
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            walletRepo.getTransactions(page = 1, limit = 30)
                .onSuccess { _txs.value = it.items }
        }
    }
}
