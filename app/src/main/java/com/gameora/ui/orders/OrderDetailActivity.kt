package com.gameora.ui.orders

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityOrderDetailBinding
import com.gameora.domain.model.OrderItem
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState
import com.gameora.util.toast

class OrderDetailActivity : BaseActivity<ActivityOrderDetailBinding>(ActivityOrderDetailBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[OrderDetailViewModel::class.java] }
    private lateinit var stateView: StateView

    private val itemsAdapter = GenericAdapter<OrderItem>(
        layoutRes = R.layout.item_order_product,
        onBind = { v, it, _ ->
            Images.load(v.findViewById<ImageView>(R.id.order_item_image), it.imageUrl)
            v.findViewById<TextView>(R.id.order_item_title).text = it.title ?: "—"
            v.findViewById<TextView>(R.id.order_item_price).text = Formatters.price(it.price, it.currency)
            v.findViewById<TextView>(R.id.order_item_qty).text = "x${it.quantity}"
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load(orderId()) }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.orderDetailItems.layoutManager = LinearLayoutManager(this)
        binding.orderDetailItems.adapter = itemsAdapter

        binding.orderDetailCancel.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.cancel_confirm)
                .setPositiveButton(R.string.action_cancel_order) { _, _ -> vm.cancel(orderId()) }
                .setNegativeButton(R.string.action_close, null)
                .show()
        }

        vm.order.observe(this) { state ->
            stateView.bind(state)
            if (state is UiState.Success) {
                val o = state.data
                binding.orderDetailId.text = "#" + o.id
                binding.orderDetailStatus.apply {
                    text = o.status.raw
                    setTextColor(getColor(Formatters.orderStatusColorRes(o.status)))
                }
                binding.orderDetailTotal.text = Formatters.price(o.total, o.currency)
                binding.orderDetailDate.text = o.createdAt ?: ""
                itemsAdapter.submit(o.items)
                binding.orderDetailCancel.visibility =
                    if (o.status.name == "PENDING_SELLER_APPROVAL") View.VISIBLE else View.GONE
            }
        }
        vm.cancelled.observe(this) { if (it is UiState.Success) toast(R.string.order_cancelled) }

        vm.load(orderId())
    }

    private fun orderId(): String = intent.getStringExtra(Nav.ORDER_ID).orEmpty()
}
