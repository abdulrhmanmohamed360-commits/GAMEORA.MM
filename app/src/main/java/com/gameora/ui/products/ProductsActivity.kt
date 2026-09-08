package com.gameora.ui.products

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gameora.R
import com.gameora.databinding.ActivityProductsBinding
import com.gameora.domain.model.Product
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.ui.productdetail.ProductDetailActivity
import com.gameora.util.UiState

class ProductsActivity : BaseActivity<ActivityProductsBinding>(ActivityProductsBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[ProductsViewModel::class.java] }
    private lateinit var stateView: StateView

    private val adapter = GenericAdapter<Product>(
        layoutRes = R.layout.item_product,
        onBind = { v, p, _ ->
            Images.load(v.findViewById<ImageView>(R.id.product_image), p.images.firstOrNull())
            v.findViewById<TextView>(R.id.product_title).text = p.title
            v.findViewById<TextView>(R.id.product_price).text = Formatters.price(p.price, p.currency)
            v.findViewById<TextView>(R.id.product_meta).text =
                listOfNotNull(p.rank, p.level, p.server).joinToString(" · ")
            v.findViewById<TextView>(R.id.product_seller).visibility = View.GONE
        },
        onClick = { p, _ ->
            startActivity(android.content.Intent(this, ProductDetailActivity::class.java).apply {
                putExtra(Nav.PRODUCT_ID, p.id)
            })
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateView = StateView(binding.stateView.root) { vm.loadFirst() }

        val gameId = intent.getStringExtra(Nav.GAME_ID)
        vm.init(gameId)
        binding.toolbar.title = intent.getStringExtra(Nav.TITLE) ?: getString(R.string.products_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.productsRecycler.layoutManager = LinearLayoutManager(this)
        binding.productsRecycler.adapter = adapter
        binding.productsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) vm.loadMore()
            }
        })

        binding.productsSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                vm.setQuery(binding.productsSearch.text?.toString()?.trim())
                vm.loadFirst()
                true
            } else false
        }

        binding.productsFilterButton.setOnClickListener {
            ProductsFilterSheet().show(supportFragmentManager, "filters")
        }

        binding.productsRefresh.setOnRefreshListener { vm.loadFirst() }

        vm.products.observe(this) { state ->
            binding.productsRefresh.isRefreshing = false
            stateView.bind(state)
            if (state is UiState.Success) adapter.submit(state.data)
        }
        vm.loadingMore.observe(this) { loading ->
            adapter.showFooter(loading)
        }

        vm.loadFirst()
    }

    fun applyFilters(filters: Map<String, String>) {
        val query = binding.productsSearch.text?.toString()?.trim()
        val withQuery = filters.toMutableMap()
        if (!query.isNullOrBlank()) withQuery["search"] = query
        vm.applyFilters(withQuery)
    }

    fun clearFilters() {
        vm.clearFilters(keepQuery = true)
    }
}
