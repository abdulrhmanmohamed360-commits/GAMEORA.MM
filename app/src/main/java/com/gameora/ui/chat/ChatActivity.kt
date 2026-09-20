package com.gameora.ui.chat

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gameora.R
import com.gameora.databinding.ActivityChatBinding
import com.gameora.domain.model.Message
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Nav
import com.gameora.util.UiState
import com.gameora.util.toast

class ChatActivity : BaseActivity<ActivityChatBinding>(ActivityChatBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[ChatViewModel::class.java] }
    private var conversationId: String = ""

    private val adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val items = mutableListOf<Message>()

        fun submit(list: List<Message>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            object : RecyclerView.ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            ) {}

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val m = items[position]
            val tv = holder.itemView.findViewById<TextView>(R.id.message_text)
            val statusTv = holder.itemView.findViewById<TextView>(R.id.message_status)

            tv.text = m.text

            val mine = m.senderId == vm.currentUserId

            /*
             * مهم: بنغيّر الـ layoutParams بتاعة الـ TextView الداخلية
             * (اللي أبوها المباشر هو FrameLayout ثابت طول الوقت جوه
             * item_message.xml)، مش بتاعة holder.itemView نفسها.
             * لو غيرنا layoutParams بتاعة holder.itemView، الـ RecyclerView
             * بيستبدلها بنوعه الخاص (RecyclerView.LayoutParams) أول ما
             * الصف يترسم فعليًا، وأي محاولة cast لـ FrameLayout.LayoutParams
             * بعد كده (لما الصف يتعاد استخدامه أثناء الـ scroll) بترمي
             * ClassCastException وتقفل الشاشة. الـ TextView مالهاش المشكلة
             * دي لأن أبوها بيفضل FrameLayout ثابت.
             */
            val params = (tv.layoutParams as FrameLayout.LayoutParams)
            params.gravity = if (mine) Gravity.END else Gravity.START
            tv.layoutParams = params

            if (mine) {
                tv.setBackgroundResource(R.drawable.bg_bubble_out)
                tv.setTextColor(getColor(R.color.text_inverse))
            } else {
                tv.setBackgroundResource(R.drawable.bg_bubble_in)
                tv.setTextColor(getColor(R.color.text_primary))
            }

            val statusParams = (statusTv.layoutParams as FrameLayout.LayoutParams)
            statusParams.gravity = if (mine) Gravity.END else Gravity.START
            statusTv.layoutParams = statusParams

            statusTv.text = when {
                m.status == LocalStatus.SENDING -> getString(R.string.message_status_sending)
                m.status == LocalStatus.FAILED -> getString(R.string.message_status_failed)
                else -> com.gameora.ui.common.Formatters.shortTime(m.createdAt)
            }

            statusTv.setTextColor(
                if (m.status == LocalStatus.FAILED) getColor(R.color.error_color)
                else getColor(R.color.text_hint)
            )

            if (mine && m.status == LocalStatus.FAILED) {
                tv.setOnClickListener { vm.retry(m) }
            } else {
                tv.setOnClickListener(null)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        conversationId = intent.getStringExtra(Nav.CONVERSATION_ID).orEmpty()

        binding.toolbar.title =
            intent.getStringExtra(Nav.TITLE) ?: getString(R.string.conversations_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.messagesRecycler.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.messagesRecycler.adapter = adapter

        binding.chatSend.setOnClickListener {
            val text = binding.chatInput.text?.toString()?.trim().orEmpty()
            if (text.isNotBlank()) {
                vm.send(conversationId, text)
                binding.chatInput.setText("")
            }
        }

        vm.messages.observe(this) { state ->
            if (state is UiState.Success) {
                adapter.submit(state.data)
                if (state.data.isNotEmpty()) {
                    binding.messagesRecycler.post {
                        binding.messagesRecycler.scrollToPosition(state.data.size - 1)
                    }
                }
            } else if (state is UiState.Empty) {
                adapter.submit(emptyList())
            }
        }

        vm.sendError.observe(this) { if (it != null) toast(it) }

        vm.load(conversationId)
    }

    override fun onResume() {
        super.onResume()
        // يمنع ظهور Push Notification لنفس المحادثة المفتوحة حاليًا على الشاشة.
        openConversationId = conversationId
    }

    override fun onPause() {
        super.onPause()
        if (openConversationId == conversationId) {
            openConversationId = null
        }
    }

    companion object {
        /**
         * معرّف المحادثة المعروضة حاليًا على الشاشة (لو موجودة)، بيتقرأ من
         * GameoraMessagingService عشان يقرر يعرض إشعار نظام أو لأ - التطبيق
         * مفتوح على نفس المحادثة أصلاً فمفيش داعي لإشعار.
         */
        var openConversationId: String? = null
    }
}
