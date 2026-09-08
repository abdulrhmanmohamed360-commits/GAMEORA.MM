package com.gameora.ui.chat

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityChatBinding
import com.gameora.domain.model.Message
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState
import com.gameora.util.toast

class ChatActivity : BaseActivity<ActivityChatBinding>(ActivityChatBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[ChatViewModel::class.java] }
    private lateinit var stateView: StateView

    private val adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
        private val items = mutableListOf<Message>()

        fun submit(list: List<Message>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        fun append(m: Message) { items.add(m); notifyItemInserted(items.size - 1) }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
            object : androidx.recyclerview.widget.RecyclerView.ViewHolder(
                android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            ) {}

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
            val m = items[position]
            val tv = holder.itemView.findViewById<TextView>(R.id.message_text)
            tv.text = m.text
            val mine = m.senderId == vm.currentUserId
            val params = (holder.itemView.layoutParams as FrameLayout.LayoutParams)
            params.gravity = if (mine) Gravity.END else Gravity.START
            holder.itemView.layoutParams = params
            if (mine) tv.setBackgroundResource(R.drawable.bg_bubble_out) else tv.setBackgroundResource(R.drawable.bg_bubble_in)
            if (mine) tv.setTextColor(getColor(R.color.text_inverse))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val cid = intent.getStringExtra(Nav.CONVERSATION_ID).orEmpty()
        binding.toolbar.title = intent.getStringExtra(Nav.TITLE) ?: getString(R.string.conversations_title)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.messagesRecycler.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.messagesRecycler.adapter = adapter

        binding.chatSend.setOnClickListener {
            val text = binding.chatInput.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) { vm.send(cid, text); binding.chatInput.setText("") }
        }

        vm.messages.observe(this) { state ->
            if (state is UiState.Success) { adapter.submit(state.data); binding.messagesRecycler.scrollToPosition(state.data.size - 1) }
        }
        vm.sent.observe(this) { if (it is UiState.Error) toast(it.message) }

        vm.load(cid)
    }
}
