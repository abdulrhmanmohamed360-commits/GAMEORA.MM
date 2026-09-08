package com.gameora.ui.sell

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import com.gameora.R
import com.gameora.databinding.ActivitySellBinding
import com.gameora.data.remote.dto.ProductCreateDto
import com.gameora.ui.common.BaseActivity
import com.gameora.util.UiState
import com.gameora.util.toast

class SellActivity : BaseActivity<ActivitySellBinding>(ActivitySellBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[SellViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbar.title = getString(R.string.sell_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.sellCurrency.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.currency_options)
        )

        vm.games.observe(this) { games ->
            binding.sellGame.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, games.map { it.name }
            )
        }
        vm.categories.observe(this) { cats ->
            binding.sellCategory.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_dropdown_item, cats.map { it.name }
            )
        }
        vm.result.observe(this) { state ->
            binding.sellProgress.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
            binding.sellSave.isEnabled = state !is UiState.Loading
            when (state) {
                is UiState.Success -> { toast(R.string.sell_success); finish() }
                is UiState.Error -> toast(state.message)
                else -> {}
            }
        }

        binding.sellSave.setOnClickListener { save() }

        vm.loadLookups()
    }

    private fun save() {
        val title = binding.sellTitle.text?.toString()?.trim().orEmpty()
        val price = binding.sellPrice.text?.toString()?.trim()?.toDoubleOrNull()
        if (title.isEmpty() || price == null) { toast(R.string.error_generic); return }

        val games = vm.games.value.orEmpty()
        val cats = vm.categories.value.orEmpty()
        val gamePos = binding.sellGame.selectedItemPosition
        val catPos = binding.sellCategory.selectedItemPosition
        val currency = binding.sellCurrency.selectedItem?.toString() ?: "USD"

        val images = binding.sellImages.text?.toString()?.split(",")?.map { it.trim() }
            ?.filter { it.isNotBlank() }.orEmpty()

        val dto = ProductCreateDto(
            gameId = games.getOrNull(gamePos)?.id ?: "",
            categoryId = cats.getOrNull(catPos)?.id,
            title = title,
            description = binding.sellDescription.text?.toString()?.trim(),
            price = price,
            currency = currency,
            images = images,
            level = binding.sellLevel.text?.toString()?.trim(),
            rank = binding.sellRank.text?.toString()?.trim(),
            server = binding.sellServer.text?.toString()?.trim()
        )
        vm.create(dto)
    }
}
