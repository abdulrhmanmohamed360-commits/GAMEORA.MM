package com.gameora.ui.notifications

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityNotificationsBinding
import com.gameora.domain.model.AppNotification
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

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
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.notificationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.notificationsRecycler.adapter = adapter
        vm.notifications.observe(this) { state -> stateView.bind(state); if (state is UiState.Success) adapter.submit(state.data) }
        vm.load()
    }
}
