package com.gameora.ui.orders

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gameora.R
import com.gameora.databinding.ActivityOrdersBinding
import com.gameora.domain.model.Order
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

class OrdersActivity : BaseActivity<ActivityOrdersBinding>(ActivityOrdersBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[OrdersViewModel::class.java] }
    private lateinit var stateView: StateView

    private val adapter = GenericAdapter<Order>(
        layoutRes = R.layout.item_order,
        onBind = { v, o, _ ->
            v.findViewById<TextView>(R.id.order_id).text = "#" + o.id
            v.findViewById<TextView>(R.id.order_status).apply {
                text = o.status.raw
                setTextColor(getColor(Formatters.orderStatusColorRes(o.status)))
            }
            v.findViewById<TextView>(R.id.order_total).text = Formatters.price(o.total, o.currency)
            v.findViewById<TextView>(R.id.order_date).text = o.createdAt ?: ""
        },
        onClick = { o, _ ->
            startActivity(Intent(this, OrderDetailActivity::class.java).apply {
                putExtra(Nav.ORDER_ID, o.id)
            })
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.loadFirst() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.ordersRecycler.layoutManager = LinearLayoutManager(this)
        binding.ordersRecycler.adapter = adapter
        binding.ordersRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= adapter.itemCount - 3) vm.loadMore()
            }
        })
        binding.ordersRefresh.setOnRefreshListener { vm.loadFirst() }

        vm.orders.observe(this) { state ->
            binding.ordersRefresh.isRefreshing = false
            stateView.bind(state)
            if (state is UiState.Success) adapter.submit(state.data)
        }
        vm.loadingMore.observe(this) { adapter.showFooter(it) }

        vm.loadFirst()
    }
}
