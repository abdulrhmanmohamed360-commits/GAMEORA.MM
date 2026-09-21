package com.gameora.ui.support

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivitySupportBinding
import com.gameora.domain.model.SupportTicket
import com.gameora.ui.chat.ChatActivity
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

/** قائمة تذاكر الدعم الفني الخاصة بالمستخدم الحالي فقط. */
class SupportActivity : BaseActivity<ActivitySupportBinding>(ActivitySupportBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[SupportViewModel::class.java] }
    private lateinit var stateView: StateView

    private val newTicketLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { vm.load() }

    private val adapter = GenericAdapter<SupportTicket>(
        layoutRes = R.layout.item_ticket,
        onBind = { v, t, _ ->
            v.findViewById<TextView>(R.id.ticket_subject).text = t.subject ?: "—"
            v.findViewById<TextView>(R.id.ticket_last_message).text = t.description ?: ""
            v.findViewById<TextView>(R.id.ticket_time).text = Formatters.shortTime(t.updatedAt ?: t.createdAt)

            val statusView = v.findViewById<TextView>(R.id.ticket_status)
            statusView.setText(Formatters.ticketStatusLabel(t.status))
            statusView.setTextColor(getColor(Formatters.ticketStatusColorRes(t.status)))
        },
        onClick = { t, _ ->
            val conversationId = t.conversationId
            if (conversationId != null) {
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra(Nav.CONVERSATION_ID, conversationId)
                    putExtra(Nav.TICKET_ID, t.ticketId)
                    putExtra(Nav.TITLE, t.subject)
                })
            }
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateView = StateView(binding.stateView.root) { vm.load() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.ticketsRecycler.layoutManager = LinearLayoutManager(this)
        binding.ticketsRecycler.adapter = adapter

        binding.supportNewTicketFab.setOnClickListener {
            requireLogin {
                newTicketLauncher.launch(Intent(this, NewTicketActivity::class.java))
            }
        }

        vm.tickets.observe(this) { state ->
            stateView.bind(state)
            if (state is UiState.Success) adapter.submit(state.data)
        }

        requireLogin { vm.load() }
    }
}
