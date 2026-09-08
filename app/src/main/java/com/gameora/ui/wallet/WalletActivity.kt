package com.gameora.ui.wallet

import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityWalletBinding
import com.gameora.domain.model.Transaction
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

class WalletActivity : BaseActivity<ActivityWalletBinding>(ActivityWalletBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[WalletViewModel::class.java] }
    private lateinit var stateView: StateView

    private val txAdapter = GenericAdapter<Transaction>(
        layoutRes = R.layout.item_transaction,
        onBind = { v, t, _ ->
            v.findViewById<TextView>(R.id.tx_description).text = t.description ?: t.type ?: "—"
            v.findViewById<TextView>(R.id.tx_type).text = t.type ?: ""
            val amount = if (t.amount >= 0) "+%.2f".format(t.amount) else "%.2f".format(t.amount)
            v.findViewById<TextView>(R.id.tx_amount).text = Formatters.price(t.amount, t.currency)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.walletTransactions.layoutManager = LinearLayoutManager(this)
        binding.walletTransactions.adapter = txAdapter

        vm.wallet.observe(this) { state ->
            stateView.bind(state)
            if (state is UiState.Success) {
                val w = state.data
                binding.walletBalance.text = Formatters.price(w.balance, w.currency)
                binding.walletPending.text = "Pending: " + Formatters.price(w.pendingBalance, w.currency)
            }
        }
        vm.transactions.observe(this) { txAdapter.submit(it) }

        vm.load()
    }
}
