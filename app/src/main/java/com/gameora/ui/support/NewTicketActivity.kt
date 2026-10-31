package com.gameora.ui.support

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.gameora.R
import com.gameora.databinding.ActivityNewTicketBinding
import com.gameora.ui.common.BaseActivity
import com.gameora.util.UiState
import com.gameora.util.toast

/** إنشاء شكوى/طلب مساعدة جديد (SupportTicket). */
class NewTicketActivity : BaseActivity<ActivityNewTicketBinding>(ActivityNewTicketBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[NewTicketViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.ticketSubmit.setOnClickListener {
            val subject = binding.ticketSubjectInput.text?.toString()?.trim().orEmpty()
            val category = binding.ticketCategoryInput.text?.toString()?.trim()
            val description = binding.ticketDescriptionInput.text?.toString()?.trim().orEmpty()

            vm.submit(subject, description, category)
        }

        vm.created.observe(this) { state ->
            when (state) {
                is UiState.Success -> {
                    toast(R.string.ticket_created)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is UiState.Error -> toast(state.message)
                else -> {}
            }
        }
    }
}
