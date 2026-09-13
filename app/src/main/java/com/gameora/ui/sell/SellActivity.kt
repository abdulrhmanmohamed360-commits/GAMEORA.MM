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

        binding.sellSave.setOnClickListener {
            save()
        }

        binding.sellPickImages.setOnClickListener {
            imagePicker.launch("image/*")
        }
    }

    private fun showImagePreviews() {

        binding.sellImagesPreview.removeAllViews()

        selectedImages.forEachIndexed { index, uri ->

            val imageContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(4)
            }

            val imageView = ImageView(this).apply {

                layoutParams = LinearLayout.LayoutParams(
                    100.dp(),
                    100.dp
                )

                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = null

                setImageURI(uri)

                setBackgroundResource(
                    android.R.drawable.picture_frame
                )

                setOnClickListener {
                    selectedImages.removeAt(index)
                    showImagePreviews()
                }
            }

            imageContainer.addView(imageView)

            binding.sellImagesPreview.addView(imageContainer)
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

        if (title.isEmpty() || price == null) {
            toast(R.string.error_generic)
            return
        }

        val games = vm.games.value.orEmpty()
        val cats = vm.categories.value.orEmpty()

        val gamePos =
            binding.sellGame.selectedItemPosition

        val catPos =
            binding.sellCategory.selectedItemPosition

        val currency =
            binding.sellCurrency.selectedItem
                ?.toString()
                ?: "USD"

        /*
         * مؤقتًا نحول الصور المختارة إلى URI strings.
         *
         * في الخطوة القادمة سيتم رفع الصور إلى Firebase Storage
         * وتحويلها إلى روابط آمنة قبل إرسال ProductCreateDto.
         */
        val images =
            selectedImages
                .map { it.toString() }

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

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
