package com.gameora.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityConversationsBinding
import com.gameora.domain.model.Conversation
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

class ConversationsActivity : BaseActivity<ActivityConversationsBinding>(ActivityConversationsBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[ConversationsViewModel::class.java] }
    private lateinit var stateView: StateView

    private val adapter = GenericAdapter<Conversation>(
        layoutRes = R.layout.item_conversation,
        onBind = { v, c, _ ->
            Images.avatar(v.findViewById<ImageView>(R.id.conversation_avatar), c.otherUserAvatarUrl)
            v.findViewById<TextView>(R.id.conversation_name).text = c.otherUserName ?: "—"
            val last = v.findViewById<TextView>(R.id.conversation_last)
            last.text = c.lastMessage ?: ""
            last.setTextColor(
                v.context.getColor(if (c.unreadCount > 0) R.color.text_primary else R.color.text_secondary)
            )
            v.findViewById<TextView>(R.id.conversation_time).text =
                Formatters.messageTime(c.lastMessageAt)
            val badge = v.findViewById<TextView>(R.id.conversation_unread)
            if (c.unreadCount > 0) {
                badge.text = if (c.unreadCount > 99) "99+" else c.unreadCount.toString()
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        },
        onClick = { c, _ ->
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra(Nav.CONVERSATION_ID, c.id)
                putExtra(Nav.TITLE, c.otherUserName)
            })
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.conversationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.conversationsRecycler.adapter = adapter
        vm.conversations.observe(this) { state ->
            stateView.bind(state)
            when (state) {
                is UiState.Success -> adapter.submit(state.data)
                is UiState.Empty -> adapter.clear()
                else -> Unit
            }
        }
    }

    /** Listener واحد بس شغال طول ما الشاشة ظاهرة (بيتقفل في onStop). */
    override fun onStart() {
        super.onStart()
        requireLogin {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) vm.start()
        }
    }

    override fun onStop() {
        vm.stop()
        super.onStop()
    }
}
