package com.gameora.ui.wallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import com.gameora.databinding.ActivityPaymentBinding
import com.gameora.ui.common.BaseActivity
import java.util.Locale

class PaymentActivity :
    BaseActivity<ActivityPaymentBinding>(ActivityPaymentBinding::inflate) {

    private var selectedCurrency = "EGP"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupQuickAmounts()
        setupAmountInput()
        setupPaymentMethod()
        updateCurrentBalance()
        updateSummary()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
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

    private fun updateCurrentBalance() {

        /*
         * الرصيد الحقيقي سيتم جلبه من الـBackend.
         * لا يوجد أي رصيد ثابت هنا.
         *
         * سيتم ربطه لاحقًا بالـWallet API.
         */
    }

    private fun updateSummary() {

        val amount = getAmount()

        /*
         * الرسوم لا يتم حسابها أو تثبيتها
         * من التطبيق.
         *
         * الـBackend سيحدد الرسوم النهائية
         * عند إنشاء عملية الدفع الحقيقية.
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
