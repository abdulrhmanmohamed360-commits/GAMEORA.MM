package com.gameora.ui.sell

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setPadding
import androidx.lifecycle.ViewModelProvider
import com.gameora.R
import com.gameora.databinding.ActivitySellBinding
import com.gameora.data.remote.dto.ProductCreateDto
import com.gameora.ui.common.BaseActivity
import com.gameora.util.UiState
import com.gameora.util.toast

class SellActivity : BaseActivity<ActivitySellBinding>(ActivitySellBinding::inflate) {

    private val vm by lazy {
        ViewModelProvider(this)[SellViewModel::class.java]
    }

    private val selectedImages = mutableListOf<Uri>()

    private var currentStep = 0

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris ->

            if (uris.isNullOrEmpty()) return@registerForActivityResult

            selectedImages.clear()
            selectedImages.addAll(uris)

            showImagePreviews()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.toolbar.title = getString(R.string.sell_title)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupCurrency()
        setupLookups()
        setupObservers()
        setupActions()

        updateStep()

        vm.loadLookups()
    }

    private fun setupCurrency() {

        binding.sellCurrency.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.currency_options)
        )
    }

    private fun setupLookups() {

        vm.games.observe(this) { games ->

            binding.sellGame.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                games.map { it.name }
            )
        }

        vm.categories.observe(this) { cats ->

            binding.sellCategory.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                cats.map { it.name }
            )
        }
    }

    private fun setupObservers() {

        vm.result.observe(this) { state ->

            binding.sellProgress.visibility =
                if (state is UiState.Loading) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            binding.sellSave.isEnabled =
                state !is UiState.Loading

            binding.sellNext.isEnabled =
                state !is UiState.Loading

            binding.sellPrevious.isEnabled =
                state !is UiState.Loading

            when (state) {

                is UiState.Success -> {
                    toast(R.string.sell_success)
                    finish()
                }

                is UiState.Error -> {
                    toast(state.message)
                }

                else -> Unit
            }
        }
    }

    private fun setupActions() {

        binding.sellNext.setOnClickListener {

            when (currentStep) {

                0 -> {
                    if (validateStepOne()) {
                        goToStep(1)
                    }
                }

                1 -> {
                    goToStep(2)
                }

                2 -> {
                    goToStep(3)
                }

                3 -> {
                    save()
                }
            }
        }

        binding.sellPrevious.setOnClickListener {

            if (currentStep > 0) {
                goToStep(currentStep - 1)
            }
        }

        binding.sellSave.setOnClickListener {
            save()
        }

        binding.sellPickImages.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun validateStepOne(): Boolean {

        val title =
            binding.sellTitle.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (title.isEmpty()) {
            binding.sellTitle.error = "اكتب اسم الإعلان"
            binding.sellTitle.requestFocus()
            return false
        }

        val games = vm.games.value.orEmpty()

        if (games.isEmpty()) {
            toast("لم يتم تحميل الألعاب بعد")
            return false
        }

        val categories = vm.categories.value.orEmpty()

        if (categories.isEmpty()) {
            toast("لم يتم تحميل أنواع الحسابات بعد")
            return false
        }

        return true
    }

    private fun goToStep(step: Int) {

        if (step < 0 || step > 3) {
            return
        }

        currentStep = step

        updateStep()

        binding.sellScroll.post {
            binding.sellScroll.scrollTo(0, 0)
        }
    }

    private fun updateStep() {

        binding.sellSteps.displayedChild = currentStep

        binding.sellStepTitle.text =
            "الخطوة ${currentStep + 1} من 4"

        binding.sellStepProgress.progress =
            currentStep + 1

        if (currentStep == 0) {

            binding.sellPrevious.visibility = View.GONE

        } else {

            binding.sellPrevious.visibility = View.VISIBLE
        }

        if (currentStep == 3) {

            binding.sellNext.visibility = View.GONE
            binding.sellSave.visibility = View.VISIBLE

        } else {

            binding.sellNext.visibility = View.VISIBLE
            binding.sellSave.visibility = View.GONE
        }
    }

    private fun showImagePreviews() {

        binding.sellImagesPreview.removeAllViews()

        val size =
            (100 * resources.displayMetrics.density).toInt()

        selectedImages.forEachIndexed { index, uri ->

            val imageView = ImageView(this).apply {

                layoutParams = LinearLayout.LayoutParams(
                    size,
                    size
                ).apply {
                    setMargins(6, 0, 6, 0)
                }

                scaleType = ImageView.ScaleType.CENTER_CROP

                contentDescription = null

                setImageURI(uri)

                setPadding(2)

                setBackgroundResource(
                    android.R.drawable.picture_frame
                )

                setOnClickListener {

                    if (index < selectedImages.size) {

                        selectedImages.removeAt(index)

                        showImagePreviews()
                    }
                }
            }

            binding.sellImagesPreview.addView(imageView)
        }
    }

    private fun save() {

        val title =
            binding.sellTitle.text
                ?.toString()
                ?.trim()
                .orEmpty()

        val price =
            binding.sellPrice.text
                ?.toString()
                ?.trim()
                ?.toDoubleOrNull()

        if (title.isEmpty()) {

            toast("اكتب اسم الإعلان")

            currentStep = 0
            updateStep()

            binding.sellTitle.requestFocus()

            return
        }

        if (price == null || price <= 0) {

            toast("أدخل سعرًا صحيحًا")

            currentStep = 3
            updateStep()

            binding.sellPrice.requestFocus()

            return
        }

        val games =
            vm.games.value.orEmpty()

        val cats =
            vm.categories.value.orEmpty()

        val gamePos =
            binding.sellGame.selectedItemPosition

        val catPos =
            binding.sellCategory.selectedItemPosition

        val currency =
            binding.sellCurrency.selectedItem
                ?.toString()
                ?: "USD"

        /*
         * الصور حاليًا محفوظة كـ Uri محلي مؤقتًا.
         * سيتم لاحقًا رفعها إلى خدمة تخزين مجانية
         * وتحويلها إلى روابط HTTPS.
         */

        val images =
            selectedImages.map {
                it.toString()
            }

        val dto = ProductCreateDto(

            gameId =
                games.getOrNull(gamePos)?.id
                    ?: "",

            categoryId =
                cats.getOrNull(catPos)?.id,

            title =
                title,

            description =
                binding.sellDescription.text
                    ?.toString()
                    ?.trim(),

            price =
                price,

            currency =
                currency,

            images =
                images,

            level =
                binding.sellLevel.text
                    ?.toString()
                    ?.trim(),

            rank =
                binding.sellRank.text
                    ?.toString()
                    ?.trim(),

            server = null
        )

        vm.create(dto)
    }
}
