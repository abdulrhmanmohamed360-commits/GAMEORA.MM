package com.gameora.ui.orders

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gameora.R
import com.gameora.databinding.ActivitySellerOrdersBinding
import com.gameora.domain.model.Order
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

class SellerOrdersActivity :
    BaseActivity<ActivitySellerOrdersBinding>(
        ActivitySellerOrdersBinding::inflate
    ) {

    private val vm by lazy {
        ViewModelProvider(this)[SellerOrdersViewModel::class.java]
    }

    private lateinit var stateView: StateView

    private val adapter = GenericAdapter<Order>(
        layoutRes = R.layout.item_order,
        onBind = { view, order, _ ->

            view.findViewById<TextView>(
                R.id.order_id
            ).text = "#" + order.id

            view.findViewById<TextView>(
                R.id.order_status
            ).apply {
                text = order.status.raw
                setTextColor(
                    getColor(
                        Formatters.orderStatusColorRes(
                            order.status
                        )
                    )
                )
            }

            view.findViewById<TextView>(
                R.id.order_total
            ).text = Formatters.price(
                order.total,
                order.currency
            )

            view.findViewById<TextView>(
                R.id.order_date
            ).text = order.createdAt ?: ""
        },

        onClick = { order, _ ->
            startActivity(
                Intent(
                    this,
                    OrderDetailActivity::class.java
                ).apply {
                    putExtra(
                        Nav.ORDER_ID,
                        order.id
                    )
                }
            )
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateView = StateView(
            binding.stateView.root
        ) {
            vm.loadFirst()
        }

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.sellerOrdersRecycler.layoutManager =
            LinearLayoutManager(this)

        binding.sellerOrdersRecycler.adapter =
            adapter

        binding.sellerOrdersRecycler.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {

                override fun onScrolled(
                    recyclerView: RecyclerView,
                    dx: Int,
                    dy: Int
                ) {
                    super.onScrolled(
                        recyclerView,
                        dx,
                        dy
                    )

                    val layoutManager =
                        recyclerView.layoutManager
                            as LinearLayoutManager

                    if (
                        layoutManager.findLastVisibleItemPosition() >=
                        adapter.itemCount - 3
                    ) {
                        vm.loadMore()
                    }
                }
            }
        )

        binding.sellerOrdersRefresh.setOnRefreshListener {
            vm.loadFirst()
        }

        vm.orders.observe(this) { state ->

            binding.sellerOrdersRefresh.isRefreshing =
                false

            stateView.bind(state)

            if (state is UiState.Success) {
                adapter.submit(state.data)
            }
        }

        vm.loadingMore.observe(this) {
            adapter.showFooter(it)
        }

        vm.loadFirst()
    }
}
