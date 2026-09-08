package com.gameora.ui.chat

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityConversationsBinding
import com.gameora.domain.model.Conversation
import com.gameora.ui.common.BaseActivity
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
            v.findViewById<TextView>(R.id.conversation_last).text = c.lastMessage ?: ""
            v.findViewById<TextView>(R.id.conversation_time).text = c.lastMessageAt ?: ""
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
        vm.conversations.observe(this) { state -> stateView.bind(state); if (state is UiState.Success) adapter.submit(state.data) }
        vm.load()
    }
}
