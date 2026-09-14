package com.gameora.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.Toast
import com.gameora.R
import com.gameora.databinding.ActivityPaymentBinding
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import java.util.Locale

class PaymentActivity :
    BaseActivity<ActivityPaymentBinding>(ActivityPaymentBinding::inflate) {

    private var selectedCurrency = "EGP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupCurrency()
        setupQuickAmounts()
        setupAmountInput()
        setupPaymentMethod()
        updateSummary()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupCurrency() {
        val currencies = listOf(
            "EGP",
            "USD",
            "SAR",
            "AED",
            "EUR"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            currencies
        )

        binding.paymentCurrency.setAdapter(adapter)
        binding.paymentCurrency.setText(
            selectedCurrency,
            false
        )

        binding.paymentCurrency.setOnItemClickListener { _, _, position, _ ->
            selectedCurrency = currencies[position]
            updateSummary()
        }
    }

    private fun setupQuickAmounts() {
        binding.paymentQuick100.setOnClickListener {
            setAmount(100)
        }

        binding.paymentQuick250.setOnClickListener {
            setAmount(250)
        }

        binding.paymentQuick500.setOnClickListener {
            setAmount(500)
        }
    }

    private fun setupAmountInput() {
        binding.paymentAmount.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    updateSummary()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    private fun setupPaymentMethod() {
        binding.paymentMethodCard.setOnClickListener {
            Toast.makeText(
                this,
                "سيتم عرض طرق الدفع المتاحة هنا",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setAmount(amount: Int) {
        binding.paymentAmount.setText(
            amount.toString()
        )

        binding.paymentAmount.setSelection(
            binding.paymentAmount.text?.length ?: 0
        )

        updateSummary()
    }

    private fun getAmount(): Double {
        return binding.paymentAmount.text
            ?.toString()
            ?.replace(",", ".")
            ?.toDoubleOrNull()
            ?: 0.0
    }

    private fun formatAmount(amount: Double): String {
        return String.format(
            Locale.US,
            "%.2f %s",
            amount,
            selectedCurrency
        )
    }

    private fun updateSummary() {
        val amount = getAmount()

        /*
         * الرسوم سيتم حسابها من الـBackend
         * بعد ربط بوابة الدفع الحقيقية.
         *
         * حاليًا الرسوم = صفر.
         */
        val fee = 0.0
        val total = amount + fee

        binding.paymentSummaryAmount.text =
            formatAmount(amount)

        binding.paymentFee.text =
            formatAmount(fee)

        binding.paymentTotal.text =
            formatAmount(total)
    }
    }
