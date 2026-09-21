package com.gameora.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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

            val displayName =
                if (c.type == "support") v.context.getString(R.string.support_desk_name)
                else c.otherUserName ?: "—"

            v.findViewById<TextView>(R.id.conversation_name).text = displayName
            v.findViewById<TextView>(R.id.conversation_last).text = c.lastMessage ?: ""
            v.findViewById<TextView>(R.id.conversation_time).text = Formatters.shortTime(c.lastMessageAt)

            val unreadView = v.findViewById<TextView>(R.id.conversation_unread)
            if (c.unreadCount > 0) {
                unreadView.text = if (c.unreadCount > 99) "99+" else c.unreadCount.toString()
                unreadView.visibility = View.VISIBLE
            } else {
                unreadView.visibility = View.GONE
            }
        },
        onClick = { c, _ ->
            val title = if (c.type == "support") null else c.otherUserName
            startActivity(Intent(this, ChatActivity::class.java).apply {
                putExtra(Nav.CONVERSATION_ID, c.id)
                putExtra(Nav.TITLE, title)
            })
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load() }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.conversationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.conversationsRecycler.adapter = adapter
        vm.conversations.observe(this) { state -> stateView.bind(state); if (state is UiState.Success) adapter.submit(state.data) }
        vm.load()
    }
}
