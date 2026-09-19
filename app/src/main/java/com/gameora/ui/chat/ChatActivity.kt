package com.gameora.ui.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gameora.R
import com.gameora.databinding.ActivityChatBinding
import com.gameora.domain.model.Message
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.Nav
import com.gameora.util.UiState
import com.gameora.util.toast

class ChatActivity : BaseActivity<ActivityChatBinding>(ActivityChatBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[ChatViewModel::class.java] }

    private var conversationId: String = ""
    private lateinit var layoutManager: LinearLayoutManager

    private val adapter = MessagesAdapter(
        myId = { vm.currentUserId },
        onRetry = { vm.retry(it.id) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        conversationId = intent.getStringExtra(Nav.CONVERSATION_ID).orEmpty()
        if (conversationId.isBlank()) {
            finish()
            return
        }

        binding.toolbar.title =
            intent.getStringExtra(Nav.TITLE) ?: getString(R.string.conversations_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.messagesRecycler.layoutManager = layoutManager
        binding.messagesRecycler.adapter = adapter
        binding.messagesRecycler.itemAnimator = null

        // لما المستخدم يوصل لأول الرسائل → نحمّل سجل أقدم.
        binding.messagesRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy < 0 && layoutManager.findFirstVisibleItemPosition() <= 2) {
                    vm.loadMore()
                }
            }
        })

        binding.chatInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                binding.chatSend.isEnabled = !s.isNullOrBlank()
            }
        })

        binding.chatSend.setOnClickListener {
            val text = binding.chatInput.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                vm.send(text)
                binding.chatInput.setText("")
            }
        }

        binding.realtimeBanner.setOnClickListener { vm.retryRealtime() }

        vm.messages.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.chatLoading.visibility = View.VISIBLE
                    binding.chatEmpty.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.chatLoading.visibility = View.GONE
                    binding.chatEmpty.visibility = View.GONE
                    render(state.data)
                }
                is UiState.Empty -> {
                    binding.chatLoading.visibility = View.GONE
                    binding.chatEmpty.setText(R.string.chat_empty)
                    binding.chatEmpty.visibility = View.VISIBLE
                    adapter.submitList(emptyList())
                }
                is UiState.Error -> {
                    binding.chatLoading.visibility = View.GONE
                    binding.chatEmpty.text = state.message
                    binding.chatEmpty.visibility = View.VISIBLE
                }
            }
        }

        vm.sendError.observe(this) { message ->
            if (message != null) {
                toast(message)
                vm.consumeSendError()
            }
        }

        vm.realtimeActive.observe(this) { active ->
            binding.realtimeBanner.visibility = if (active) View.GONE else View.VISIBLE
        }
    }

    /**
     * الـ Listener بيبدأ في onStart وبيتقفل في onStop: مفيش Listeners شغالة والشاشة في
     * الخلفية، ومستحيل يتكرروا لأن ChatViewModel.start() آمن للاستدعاء المتكرر.
     */
    override fun onStart() {
        super.onStart()
        if (conversationId.isBlank()) return
        requireLogin {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                vm.start(conversationId)
            }
        }
    }

    override fun onStop() {
        if (conversationId.isNotBlank()) vm.stop()
        super.onStop()
    }

    private fun isNearBottom(): Boolean {
        if (adapter.itemCount == 0) return true
        return layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 2
    }

    private fun render(list: List<Message>) {
        val old = adapter.currentList
        val oldFirstId = old.firstOrNull()?.id
        val oldLastId = old.lastOrNull()?.id
        val wasNearBottom = isNearBottom()

        // Anchor للحفاظ على مكان القراءة لما بيتحمّل سجل أقدم فوق.
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        val anchorId = old.getOrNull(firstPos)?.id
        val anchorOffset = layoutManager.findViewByPosition(firstPos)?.top ?: 0

        adapter.submitList(list) {
            val newLast = list.lastOrNull()
            val appended = newLast != null && newLast.id != oldLastId

            if (old.isEmpty() || (appended && (wasNearBottom || newLast?.senderId == vm.currentUserId))) {
                binding.messagesRecycler.scrollToPosition(list.size - 1)
            } else if (anchorId != null && oldFirstId != null && list.firstOrNull()?.id != oldFirstId) {
                val idx = list.indexOfFirst { it.id == anchorId }
                if (idx >= 0) layoutManager.scrollToPositionWithOffset(idx, anchorOffset)
            }
        }
    }

    // ------------------------------------------------------------- Adapter

    private class MessagesAdapter(
        private val myId: () -> String?,
        private val onRetry: (Message) -> Unit
    ) : ListAdapter<Message, MessagesAdapter.VH>(Diff) {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val bubble: View = view.findViewById(R.id.message_bubble)
            val text: TextView = view.findViewById(R.id.message_text)
            val meta: TextView = view.findViewById(R.id.message_meta)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = getItem(position)
            val ctx = holder.itemView.context
            val mine = m.senderId == myId()

            holder.text.text = m.text

            /*
             * بنغيّر layout_gravity بتاعة الـ bubble (أبوها FrameLayout ثابت جوه item_message.xml)،
             * مش بتاعة holder.itemView، عشان RecyclerView بيستبدل LayoutParams بتاعة الصف نفسه.
             */
            val params = holder.bubble.layoutParams as FrameLayout.LayoutParams
            params.gravity = if (mine) Gravity.END else Gravity.START
            holder.bubble.layoutParams = params

            val time = Formatters.messageTime(m.createdAt, timeOnly = true)
            val failed = m.status == ChatViewModel.STATUS_FAILED

            if (mine) {
                holder.bubble.setBackgroundResource(R.drawable.bg_bubble_out)
                holder.text.setTextColor(ctx.getColor(R.color.text_inverse))
                holder.meta.setTextColor(ctx.getColor(R.color.text_inverse))
                holder.meta.alpha = 0.7f
                holder.meta.text = if (failed) {
                    ctx.getString(R.string.chat_message_failed)
                } else {
                    listOf(time, statusMark(m.status)).filter { it.isNotEmpty() }.joinToString("  ")
                }
            } else {
                holder.bubble.setBackgroundResource(R.drawable.bg_bubble_in)
                holder.text.setTextColor(ctx.getColor(R.color.text_primary))
                holder.meta.setTextColor(ctx.getColor(R.color.text_hint))
                holder.meta.alpha = 1f
                holder.meta.text = time
            }

            holder.itemView.setOnClickListener { if (failed) onRetry(m) }
            holder.itemView.isClickable = failed
        }

        private fun statusMark(status: String?): String = when (status) {
            ChatViewModel.STATUS_SENDING -> "…"
            ChatViewModel.STATUS_READ -> "✓✓"
            else -> "✓"
        }

        object Diff : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(a: Message, b: Message) = a.id == b.id
            override fun areContentsTheSame(a: Message, b: Message) = a == b
        }
    }
}
