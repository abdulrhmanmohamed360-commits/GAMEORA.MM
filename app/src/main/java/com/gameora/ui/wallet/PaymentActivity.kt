package com.gameora.ui.wallet

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.gameora.GameoraApp
import com.gameora.databinding.ActivityPaymentBinding
import com.gameora.ui.common.BaseActivity
import kotlinx.coroutines.launch
import java.util.Locale

class PaymentActivity :
    BaseActivity<ActivityPaymentBinding>(
        ActivityPaymentBinding::inflate
    ) {

    private val walletRepository
        get() = GameoraApp.get()
            .container
            .walletRepository

    private var selectedCurrency = "EGP"

    private var isCreatingPayment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupToolbar()
        setupQuickAmounts()
        setupAmountInput()
        setupPaymentMethod()
        setupContinueButton()

        updateCurrentBalance()
        updateSummary()
    }

    // ------------------------------------------------------------
    // Toolbar
    // ------------------------------------------------------------

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    // ------------------------------------------------------------
    // Quick amounts
    // ------------------------------------------------------------

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

    // ------------------------------------------------------------
    // Amount input
    // ------------------------------------------------------------

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

    // ------------------------------------------------------------
    // Payment method
    // ------------------------------------------------------------

    private fun setupPaymentMethod() {

        binding.paymentMethodCard.setOnClickListener {

            Toast.makeText(
                this,
                "الدفع متاح حاليًا بالجنيه المصري",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ------------------------------------------------------------
    // Continue / Paymob
    // ------------------------------------------------------------

    private fun setupContinueButton() {

        binding.paymentContinue.setOnClickListener {

            if (isCreatingPayment) {
                return@setOnClickListener
            }

            val amount = getAmount()

            if (amount < 10.0) {

                Toast.makeText(
                    this,
                    "الحد الأدنى لإضافة الرصيد هو 10 جنيه",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (amount > 100000.0) {

                Toast.makeText(
                    this,
                    "الحد الأقصى لإضافة الرصيد هو 100000 جنيه",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            createPayment(amount)
        }
    }

    // ------------------------------------------------------------
    // Create payment
    // ------------------------------------------------------------

    private fun createPayment(amount: Double) {

        isCreatingPayment = true

        setPaymentLoading(true)

        lifecycleScope.launch {

            try {

                val result =
                    walletRepository.createDeposit(
                        amount = amount
                    )

                result
                    .onSuccess { payment ->

                        val paymentUrl =
                            payment.paymentUrl

                        if (
                            paymentUrl.isNullOrBlank()
                        ) {

                            Toast.makeText(
                                this@PaymentActivity,
                                "تعذر إنشاء صفحة الدفع",
                                Toast.LENGTH_LONG
                            ).show()

                            return@onSuccess
                        }

                        openPaymobCheckout(
                            paymentUrl
                        )
                    }
                    .onFailure { error ->

                        Toast.makeText(
                            this@PaymentActivity,
                            error.message
                                ?: "حدث خطأ أثناء إنشاء عملية الدفع",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } catch (e: Exception) {

                Toast.makeText(
                    this@PaymentActivity,
                    e.message
                        ?: "حدث خطأ أثناء الاتصال بالسيرفر",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                isCreatingPayment = false

                setPaymentLoading(false)
            }
        }
    }

    // ------------------------------------------------------------
    // Open Paymob
    // ------------------------------------------------------------

    private fun openPaymobCheckout(
        paymentUrl: String
    ) {

        try {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(paymentUrl)
            )

            startActivity(intent)

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "تعذر فتح صفحة الدفع",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ------------------------------------------------------------
    // Loading state
    // ------------------------------------------------------------

    private fun setPaymentLoading(
        loading: Boolean
    ) {

        binding.paymentContinue.isEnabled =
            !loading

        binding.paymentAmount.isEnabled =
            !loading

        binding.paymentQuick100.isEnabled =
            !loading

        binding.paymentQuick250.isEnabled =
            !loading

        binding.paymentQuick500.isEnabled =
            !loading

        binding.paymentContinue.text =
            if (loading) {
                "جاري تجهيز الدفع..."
            } else {
                "متابعة للدفع"
            }
    }

    // ------------------------------------------------------------
    // Current balance
    // ------------------------------------------------------------

    private fun updateCurrentBalance() {

        /*
         * الرصيد الحقيقي لا يتم وضعه داخل Activity.
         *
         * سيتم جلبه من Backend.
         *
         * لا يوجد أي رصيد ثابت هنا.
         */
    }

    // ------------------------------------------------------------
    // Summary
    // ------------------------------------------------------------

    private fun updateSummary() {

        val amount = getAmount()

        /*
         * الرسوم لا يتم تثبيتها داخل التطبيق.
         *
         * Backend هو المسؤول عن الرسوم النهائية.
         *
         * حاليًا صفحة الدفع تعرض الرسوم = 0
         * لأن Backend الحالي لا يعيد رسوم منفصلة.
         */

        val fee = 0.0

        val total =
            amount + fee

        binding.paymentSummaryAmount.text =
            formatAmount(amount)

        binding.paymentFee.text =
            formatAmount(fee)

        binding.paymentTotal.text =
            formatAmount(total)
    }

    // ------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------

    private fun formatAmount(
        amount: Double
    ): String {

        return String.format(
            Locale.US,
            "%.2f %s",
            amount,
            selectedCurrency
        )
    }
    }
