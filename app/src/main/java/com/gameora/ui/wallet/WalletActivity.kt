package com.gameora.ui.wallet

import android.content.Intent
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

class WalletActivity :
    BaseActivity<ActivityWalletBinding>(ActivityWalletBinding::inflate) {

    private val vm by lazy {
        ViewModelProvider(this)[WalletViewModel::class.java]
    }

    private lateinit var stateView: StateView

    private var balanceVisible = true
    private var currentBalanceText = "0.00 EGP"

    private val txAdapter = GenericAdapter<Transaction>(
        layoutRes = R.layout.item_transaction,
        onBind = { v, t, _ ->
            v.findViewById<TextView>(R.id.tx_description).text =
                t.description ?: t.type ?: "—"

            v.findViewById<TextView>(R.id.tx_type).text =
                t.type ?: ""

            v.findViewById<TextView>(R.id.tx_amount).text =
                Formatters.price(t.amount, t.currency)
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        balanceVisible = savedInstanceState?.getBoolean(
            KEY_BALANCE_VISIBLE,
            true
        ) ?: true

        stateView = StateView(binding.stateView.root) {
            vm.load()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.walletTransactions.layoutManager =
            LinearLayoutManager(this)

        binding.walletTransactions.adapter = txAdapter

        setupBalanceToggle()
        setupWalletActions()

        vm.wallet.observe(this) { state ->
            stateView.bind(state)

            if (state is UiState.Success) {
                val wallet = state.data

                currentBalanceText =
                    Formatters.price(
                        wallet.balance,
                        wallet.currency
                    )

                updateBalanceVisibility()

                binding.walletPending.text =
                    "Pending: " +
                        Formatters.price(
                            wallet.pendingBalance,
                            wallet.currency
                        )

                binding.walletAvailableLabel.text =
                    Formatters.price(
                        wallet.balance,
                        wallet.currency
                    )

                binding.walletPendingLabel.text =
                    Formatters.price(
                        wallet.pendingBalance,
                        wallet.currency
                    )
            }
        }

        vm.transactions.observe(this) {
            txAdapter.submit(it)
        }

        vm.load()
    }

    private fun setupBalanceToggle() {
        binding.walletToggleBalance.setOnClickListener {
            balanceVisible = !balanceVisible
            updateBalanceVisibility()
        }
    }

    private fun updateBalanceVisibility() {
        if (balanceVisible) {
            binding.walletBalance.text = currentBalanceText
            binding.walletToggleBalance.setImageResource(
                android.R.drawable.ic_menu_view
            )
            binding.walletToggleBalance.contentDescription =
                "إخفاء الرصيد"
        } else {
            binding.walletBalance.text = "••••••••"
            binding.walletToggleBalance.setImageResource(
                android.R.drawable.ic_menu_view
            )
            binding.walletToggleBalance.contentDescription =
                "إظهار الرصيد"
        }
    }

    private fun setupWalletActions() {

        // إضافة رصيد
        binding.walletAddBalanceCard.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    PaymentActivity::class.java
                )
            )
        }

        // السحب - هنربطه بعدين
        binding.walletWithdrawCard.setOnClickListener {
            // سيتم ربط صفحة السحب في الخطوة القادمة.
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(
            KEY_BALANCE_VISIBLE,
            balanceVisible
        )

        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val KEY_BALANCE_VISIBLE =
            "wallet_balance_visible"
    }
    }
