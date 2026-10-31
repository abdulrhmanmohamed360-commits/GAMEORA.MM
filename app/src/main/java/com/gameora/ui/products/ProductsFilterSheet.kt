package com.gameora.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.gameora.R
import com.gameora.databinding.SheetProductsFilterBinding
import com.gameora.domain.model.Category
import com.gameora.domain.model.Game
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProductsFilterSheet : BottomSheetDialogFragment() {

    private var _binding: SheetProductsFilterBinding? = null
    private val binding get() = _binding!!

    private val vm by lazy {
        androidx.lifecycle.ViewModelProvider(requireActivity())[ProductsViewModel::class.java]
    }

    private var games: List<Game> = emptyList()
    private var categories: List<Category> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetProductsFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.filterStatus.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.product_status_options)
        )

        binding.filterApply.setOnClickListener {
            (activity as? ProductsActivity)?.applyFilters(collectFilters())
            dismiss()
        }
        binding.filterClear.setOnClickListener {
            (activity as? ProductsActivity)?.clearFilters()
            dismiss()
        }

        vm.games.observe(viewLifecycleOwner) { populateGames(it) }
        vm.categories.observe(viewLifecycleOwner) { populateCategories(it) }
        vm.loadFilterData()
    }

    private fun populateGames(list: List<Game>) {
        games = list
        val names = listOf(getString(R.string.status_all)) + list.map { it.name }
        binding.filterGame.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
    }

    private fun populateCategories(list: List<Category>) {
        categories = list
        val names = listOf(getString(R.string.status_all)) + list.map { it.name }
        binding.filterCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
    }

    private fun collectFilters(): Map<String, String> {
        val map = mutableMapOf<String, String>()

        val gamePos = binding.filterGame.selectedItemPosition - 1
        if (gamePos in games.indices) map["gameId"] = games[gamePos].id

        val catPos = binding.filterCategory.selectedItemPosition - 1
        if (catPos in categories.indices) map["categoryId"] = categories[catPos].id

        val statusPos = binding.filterStatus.selectedItemPosition
        if (statusPos > 0) {
            map["status"] = resources.getStringArray(R.array.product_status_options)[statusPos]
        }

        binding.filterPriceMin.text?.toString()?.takeIf { it.isNotBlank() }?.let { map["priceMin"] = it }
        binding.filterPriceMax.text?.toString()?.takeIf { it.isNotBlank() }?.let { map["priceMax"] = it }
        binding.filterRank.text?.toString()?.takeIf { it.isNotBlank() }?.let { map["rank"] = it }
        binding.filterLevel.text?.toString()?.takeIf { it.isNotBlank() }?.let { map["level"] = it }
        binding.filterServer.text?.toString()?.takeIf { it.isNotBlank() }?.let { map["server"] = it }

        return map
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
