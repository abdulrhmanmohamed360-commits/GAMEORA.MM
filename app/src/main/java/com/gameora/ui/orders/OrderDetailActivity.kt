package com.gameora.ui.orders

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.GameoraApp
import com.gameora.R
import com.gameora.data.repository.ChatRepository
import com.gameora.databinding.ActivityOrderDetailBinding
import com.gameora.domain.model.Order
import com.gameora.domain.model.OrderItem
import com.gameora.ui.chat.ChatActivity
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState
import com.gameora.util.toast
import kotlinx.coroutines.launch

class OrderDetailActivity :
BaseActivity<ActivityOrderDetailBinding>(ActivityOrderDetailBinding::inflate) {

private val vm by lazy {
    ViewModelProvider(this)[OrderDetailViewModel::class.java]
}

private val chatRepository by lazy {
    ChatRepository(GameoraApp.get().container.apiService)
}

private val currentUserId: String?
    get() = GameoraApp.get().container.sessionManager.currentUser.value?.id

private lateinit var stateView: StateView

private val itemsAdapter = GenericAdapter<OrderItem>(
    layoutRes = R.layout.item_order_product,
    onBind = { v, item, _ ->
        Images.load(
            v.findViewById(R.id.order_item_image),
            item.imageUrl
        )

        v.findViewById<TextView>(R.id.order_item_title).text =
            item.title ?: "—"

        v.findViewById<TextView>(R.id.order_item_price).text =
            Formatters.price(item.price, item.currency)

        v.findViewById<TextView>(R.id.order_item_qty).text =
            "x${item.quantity}"
    }
)

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    stateView = StateView(binding.stateView.root) {
        vm.load(orderId())
    }

    binding.toolbar.setNavigationOnClickListener {
        finish()
    }

    binding.orderDetailItems.layoutManager =
        LinearLayoutManager(this)

    binding.orderDetailItems.adapter =
        itemsAdapter

    setupActions()

    vm.order.observe(this) { state ->
        stateView.bind(state)

        if (state is UiState.Success) {
            val order = state.data

            binding.orderDetailId.text =
                "#" + order.id

            binding.orderDetailStatus.apply {
                text = order.status.raw
                setTextColor(
                    getColor(
                        Formatters.orderStatusColorRes(order.status)
                    )
                )
            }

            binding.orderDetailTotal.text =
                Formatters.price(
                    order.total,
                    order.currency
                )

            binding.orderDetailDate.text =
                order.createdAt ?: ""

            itemsAdapter.submit(order.items)

            updateActions(order)
        }
    }

    vm.actionState.observe(this) { state ->
        when (state) {
            is UiState.Success -> {
                toast("تم تنفيذ العملية بنجاح")
            }

            is UiState.Error -> {
                toast(state.message)
            }

            else -> Unit
        }
    }

    vm.cancelled.observe(this) { state ->
        when (state) {
            is UiState.Success -> {
                toast(R.string.order_cancelled)
                vm.load(orderId())
            }

            is UiState.Error -> {
                toast(state.message)
            }

            else -> Unit
        }
    }

    vm.load(orderId())
}

private fun setupActions() {

    binding.orderDetailCancel.setOnClickListener {
        AlertDialog.Builder(this)
            .setMessage(R.string.cancel_confirm)
            .setPositiveButton(
                R.string.action_cancel_order
            ) { _, _ ->
                vm.cancel(orderId())
            }
            .setNegativeButton(
                R.string.action_close,
                null
            )
            .show()
    }

    binding.orderDetailApprove.setOnClickListener {
        AlertDialog.Builder(this)
            .setMessage(
                "هل تريد قبول هذا الطلب والبدء في تجهيزه؟"
            )
            .setPositiveButton("قبول") { _, _ ->
                vm.approve(orderId())
            }
            .setNegativeButton(
                R.string.action_close,
                null
            )
            .show()
    }

    binding.orderDetailReject.setOnClickListener {
        AlertDialog.Builder(this)
            .setMessage(
                "هل تريد رفض هذا الطلب؟ سيتم إعادة المبلغ للمشتري."
            )
            .setPositiveButton("رفض") { _, _ ->
                vm.reject(orderId())
            }
            .setNegativeButton(
                R.string.action_close,
                null
            )
            .show()
    }

    binding.orderDetailDeliver.setOnClickListener {
        vm.deliver(orderId())
    }

    binding.orderDetailConfirm.setOnClickListener {
        AlertDialog.Builder(this)
            .setMessage(
                "هل اختبرت الحساب وتأكدت أن البيانات صحيحة؟"
            )
            .setPositiveButton("تأكيد الاستلام") { _, _ ->
                vm.confirm(orderId())
            }
            .setNegativeButton(
                R.string.action_close,
                null
            )
            .show()
    }

    binding.orderDetailDispute.setOnClickListener {
        AlertDialog.Builder(this)
            .setMessage("هل تريد فتح نزاع على هذا الطلب؟")
            .setPositiveButton("فتح النزاع") { _, _ ->
                vm.dispute(orderId())
            }
            .setNegativeButton(
                R.string.action_close,
                null
            )
            .show()
    }

    binding.orderDetailChat.setOnClickListener {
        openOrderChat()
    }

    binding.orderBuyerChat.setOnClickListener {
        openOrderChat()
    }
}

private fun updateActions(order: Order) {

    val status = order.status.name
    val myId = currentUserId
    val isSeller = myId != null && myId == order.sellerId
    val isBuyer = myId != null && myId == order.buyerId

    binding.orderSellerActions.visibility =
        View.GONE

    binding.orderBuyerActions.visibility =
        View.GONE

    binding.orderDetailApprove.visibility =
        View.GONE

    binding.orderDetailReject.visibility =
        View.GONE

    binding.orderDetailDeliver.visibility =
        View.GONE

    binding.orderDetailConfirm.visibility =
        View.GONE

    binding.orderDetailDispute.visibility =
        View.GONE

    binding.orderDetailChat.visibility =
        View.GONE

    binding.orderBuyerChat.visibility =
        View.GONE

    binding.orderDetailCancel.visibility =
        View.GONE

    when (status) {

        "PENDING_SELLER_APPROVAL" -> {
            if (isSeller) {
                binding.orderSellerActions.visibility =
                    View.VISIBLE

                binding.orderDetailApprove.visibility =
                    View.VISIBLE

                binding.orderDetailReject.visibility =
                    View.VISIBLE
            }
        }

        "SELLER_ACCEPTED",
        "CHAT_ACTIVE" -> {

            if (isSeller) {
                binding.orderSellerActions.visibility =
                    View.VISIBLE

                binding.orderDetailDeliver.visibility =
                    View.VISIBLE

                binding.orderDetailChat.visibility =
                    View.VISIBLE
            }

            if (isBuyer) {
                binding.orderBuyerActions.visibility =
                    View.VISIBLE

                binding.orderBuyerChat.visibility =
                    View.VISIBLE
            }
        }

        "ACCOUNT_DELIVERED",
        "BUYER_TESTING" -> {

            if (isBuyer) {
                binding.orderBuyerActions.visibility =
                    View.VISIBLE

                binding.orderDetailConfirm.visibility =
                    View.VISIBLE

                binding.orderDetailDispute.visibility =
                    View.VISIBLE

                binding.orderBuyerChat.visibility =
                    View.VISIBLE
            }
        }

        "DISPUTED" -> {

            if (isBuyer) {
                binding.orderBuyerActions.visibility =
                    View.VISIBLE

                binding.orderDetailDispute.visibility =
                    View.GONE

                binding.orderBuyerChat.visibility =
                    View.VISIBLE
            }
        }

        "COMPLETED" -> {

            if (isBuyer) {
                binding.orderBuyerActions.visibility =
                    View.VISIBLE

                binding.orderBuyerChat.visibility =
                    View.VISIBLE
            }
        }
    }
}

private fun openOrderChat() {

    val id = orderId()

    if (id.isBlank()) {
        toast("رقم الطلب غير صالح")
        return
    }

    lifecycleScope.launch {

        chatRepository.getConversations()
            .fold(

                onSuccess = { conversations ->

                    val conversation =
                        conversations.firstOrNull { conversation ->
                            conversation.orderId == id
                        }

                    if (conversation == null) {
                        toast("لم يتم العثور على محادثة هذا الطلب")
                        return@fold
                    }

                    startActivity(
                        Intent(
                            this@OrderDetailActivity,
                            ChatActivity::class.java
                        ).apply {

                            putExtra(
                                Nav.CONVERSATION_ID,
                                conversation.id
                            )

                            putExtra(
                                Nav.TITLE,
                                conversation.otherUserName
                            )
                        }
                    )
                },

                onFailure = { error ->
                    toast(
                        error.message
                            ?: "حدث خطأ أثناء فتح المحادثة"
                    )
                }
            )
    }
}

private fun orderId(): String =
    intent.getStringExtra(Nav.ORDER_ID).orEmpty()

}
