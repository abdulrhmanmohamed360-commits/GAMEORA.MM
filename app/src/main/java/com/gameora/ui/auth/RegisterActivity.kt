package com.gameora.ui.auth

import android.os.Bundle
import com.gameora.databinding.ActivityRegisterBinding
import com.gameora.ui.common.BaseActivity
import com.gameora.util.UiState
import com.gameora.util.toast

class RegisterActivity : BaseActivity<ActivityRegisterBinding>(ActivityRegisterBinding::inflate) {

    private val vm by lazy { androidx.lifecycle.ViewModelProvider(this)[AuthViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.registerButton.setOnClickListener {
            val username = binding.usernameField.text?.toString()?.trim().orEmpty()
            val displayName = binding.displayNameField.text?.toString()?.trim().orEmpty()
            val email = binding.emailField.text?.toString()?.trim().orEmpty()
            val pass = binding.passwordField.text?.toString().orEmpty()
            val confirm = binding.confirmField.text?.toString().orEmpty()

            if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                toast(getString(com.gameora.R.string.error_generic))
                return@setOnClickListener
            }
            if (pass != confirm) {
                toast(getString(com.gameora.R.string.error_generic))
                return@setOnClickListener
            }
            vm.register(username, email, pass, displayName.ifBlank { null })
        }

        binding.toLogin.setOnClickListener { finish() }

        vm.state.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.registerProgress.visibility = android.view.View.VISIBLE
                    binding.registerButton.isEnabled = false
                }
                is UiState.Success -> {
                    binding.registerProgress.visibility = android.view.View.GONE
                    setResult(RESULT_OK)
                    finish()
                }
                is UiState.Error -> {
                    binding.registerProgress.visibility = android.view.View.GONE
                    binding.registerButton.isEnabled = true
                    toast(state.message)
                }
                else -> {}
            }
        }
    }
}
