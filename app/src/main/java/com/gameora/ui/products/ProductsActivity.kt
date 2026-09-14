package com.gameora.ui.products

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
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
import com.gameora.ui.sell.SellActivity
import com.gameora.util.UiState

class ProductsActivity :
    BaseActivity<ActivityProductsBinding>(ActivityProductsBinding::inflate) {

    private val vm by lazy {
        ViewModelProvider(this)[ProductsViewModel::class.java]
    }

    private lateinit var stateView: StateView

    private lateinit var productsAdapter: GenericAdapter<Product>
    private lateinit var concatAdapter: ConcatAdapter

    private val headerAdapter =
        HeaderAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateView = StateView(binding.stateView.root) {
            vm.loadFirst()
        }

        val gameId = intent.getStringExtra(Nav.GAME_ID)

        vm.init(gameId)

        binding.toolbar.title =
            intent.getStringExtra(Nav.TITLE)
                ?: getString(R.string.products_title)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupProductsAdapter()
        setupHeaderActions()
        setupObservers()

        vm.loadFirst()
    }

    private fun setupProductsAdapter() {

        productsAdapter = GenericAdapter<Product>(
            layoutRes = R.layout.item_product,

            onBind = { v, p, _ ->

                Images.load(
                    v.findViewById<ImageView>(R.id.product_image),
                    p.images.firstOrNull()
                )

                v.findViewById<TextView>(
                    R.id.product_title
                ).text = p.title

                v.findViewById<TextView>(
                    R.id.product_price
                ).text = Formatters.price(
                    p.price,
                    p.currency
                )

                v.findViewById<TextView>(
                    R.id.product_meta
                ).text =
                    listOfNotNull(
                        p.rank,
                        p.level,
                        p.server
                    ).joinToString(" · ")

                v.findViewById<TextView>(
                    R.id.product_seller
                ).visibility = View.GONE
            },

            onClick = { p, _ ->

                startActivity(
                    Intent(
                        this,
                        ProductDetailActivity::class.java
                    ).apply {
                        putExtra(
                            Nav.PRODUCT_ID,
                            p.id
                        )
                    }
                )
            }
        )

        concatAdapter = ConcatAdapter(
            headerAdapter,
            productsAdapter
        )

        binding.productsRecycler.layoutManager =
            LinearLayoutManager(this)

        binding.productsRecycler.adapter =
            concatAdapter

        binding.productsRecycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {

                override fun onScrolled(
                    rv: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {

                    if (dy <= 0) return

                    val lm =
                        rv.layoutManager as LinearLayoutManager

                    val last =
                        lm.findLastVisibleItemPosition()

                    val total =
                        concatAdapter.itemCount

                    if (last >= total - 3) {
                        vm.loadMore()
                    }
                }
            }
        )
    }

    private fun setupHeaderActions() {

        headerAdapter.onSearch = {

            headerAdapter.searchText
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { query ->
                    vm.setQuery(query)
                }

            vm.loadFirst()
        }

        headerAdapter.onFilter = {

            ProductsFilterSheet()
                .show(
                    supportFragmentManager,
                    "filters"
                )
        }

        headerAdapter.onSell = {

            startActivity(
                Intent(
                    this,
                    SellActivity::class.java
                )
            )
        }
    }

    private fun setupObservers() {

        vm.products.observe(this) { state ->

            stateView.bind(state)

            if (state is UiState.Success) {
                productsAdapter.submit(state.data)
            }
        }

        vm.loadingMore.observe(this) { loading ->
            productsAdapter.showFooter(loading)
        }
    }

    fun applyFilters(
        filters: Map<String, String>
    ) {

        val query =
            headerAdapter.searchText
                ?.trim()

        val withQuery =
            filters.toMutableMap()

        if (!query.isNullOrBlank()) {
            withQuery["search"] = query
        }

        vm.applyFilters(withQuery)
    }

    fun clearFilters() {
        vm.clearFilters(
            keepQuery = true
        )
    }

    private class HeaderAdapter :
        RecyclerView.Adapter<HeaderAdapter.HeaderHolder>() {

        var onSearch: (() -> Unit)? = null
        var onFilter: (() -> Unit)? = null
        var onSell: (() -> Unit)? = null

        var searchText: String? = null

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): HeaderHolder {

            val view =
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_products_header,
                        parent,
                        false
                    )

            return HeaderHolder(view)
        }

        override fun onBindViewHolder(
            holder: HeaderHolder,
            position: Int
        ) {

            holder.search.setOnEditorActionListener {
                    _, actionId, _ ->

                if (
                    actionId ==
                    EditorInfo.IME_ACTION_SEARCH
                ) {

                    searchText =
                        holder.search.text
                            ?.toString()

                    onSearch?.invoke()

                    true
                } else {
                    false
                }
            }

            holder.filter.setOnClickListener {
                onFilter?.invoke()
            }

            holder.sell.setOnClickListener {
                onSell?.invoke()
            }
        }

        override fun getItemCount(): Int = 1

        class HeaderHolder(
            view: View
        ) : RecyclerView.ViewHolder(view) {

            val search =
                view.findViewById<android.widget.EditText>(
                    R.id.products_search
                )

            val filter =
                view.findViewById<
                    com.google.android.material.button.MaterialButton
                >(
                    R.id.products_filter_button
                )

            val sell =
                view.findViewById<
                    com.google.android.material.card.MaterialCardView
                >(
                    R.id.sell_banner
                )
        }
    }
    }
