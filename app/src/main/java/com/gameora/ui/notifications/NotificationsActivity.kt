package com.gameora.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityNotificationsBinding
import com.gameora.domain.model.AppNotification
import com.gameora.ui.chat.ChatActivity
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState
import com.gameora.util.toast
import kotlinx.coroutines.launch

class NotificationsActivity : BaseActivity<ActivityNotificationsBinding>(ActivityNotificationsBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[NotificationsViewModel::class.java] }
    private lateinit var stateView: StateView

    private val adapter = GenericAdapter<AppNotification>(
        layoutRes = R.layout.item_notification,
        onBind = { v, n, _ ->
            v.findViewById<TextView>(R.id.notification_title).text = n.title ?: n.type ?: "—"
            v.findViewById<TextView>(R.id.notification_body).text = n.body ?: ""
            v.findViewById<TextView>(R.id.notification_time).text = n.createdAt ?: ""
            v.findViewById<View>(R.id.notification_dot).visibility = if (!n.read) View.VISIBLE else View.INVISIBLE
        },
        onClick = { n, _ -> onNotificationClicked(n) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.notificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecycler.adapter = adapter
        vm.notifications.observe(this) { state -> stateView.bind(state); if (state is UiState.Success) adapter.submit(state.data) }
        vm.load()

        /*
         * لو الشاشة دي اتفتحت من الضغط على Push Notification حقيقي
         * (من شريط الإشعارات في النظام)، بنكون استقبلنا orderId جاهز
         * كـ extra - نفتح المحادثة المرتبطة بيه فورًا بنفس منطق
         * الضغط اليدوي على عنصر في القائمة، من غير ما ننتظر تاتش
         * تاني من المستخدم.
         */
        intent.getStringExtra(Nav.ORDER_ID)?.takeIf { it.isNotBlank() }?.let { orderId ->
            openChatForOrder(orderId)
        }
    }

    /**
     * تنفيذ متطلب: الضغط على إشعار مرتبط بطلب يفتح نفس محادثة
     * هذا الطلب مباشرة - بدون إنشاء Conversation جديدة، وبدون المرور
     * على شاشة تفاصيل الطلب. نستخدم orderId من الإشعار للبحث عن
     * المحادثة المرتبطة به من بين محادثات المستخدم الحالية.
     */
    private fun onNotificationClicked(notification: AppNotification) {
        val orderId = notification.orderId

        if (orderId.isNullOrBlank()) {
            // إشعار غير مرتبط بطلب (مثلاً إشعار عام) - لا يوجد محادثة لفتحها.
            return
        }

        openChatForOrder(orderId)
    }

    private fun openChatForOrder(orderId: String) {
        lifecycleScope.launch {
            container.chatRepository.getConversations().fold(
                onSuccess = { conversations ->
                    val conversation = conversations.firstOrNull { it.orderId == orderId }

                    if (conversation == null) {
                        toast("لم يتم العثور على محادثة هذا الطلب")
                        return@fold
                    }

                    startActivity(
                        Intent(this@NotificationsActivity, ChatActivity::class.java).apply {
                            putExtra(Nav.CONVERSATION_ID, conversation.id)
                            putExtra(Nav.TITLE, conversation.otherUserName)
                        }
                    )
                },
                onFailure = { error ->
                    toast(error.message ?: "حدث خطأ أثناء فتح المحادثة")
                }
            )
        }
    }
}
