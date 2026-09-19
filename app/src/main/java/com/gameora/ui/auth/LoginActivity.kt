package com.gameora.ui.auth

import android.content.Intent
import android.os.Bundle
import com.gameora.databinding.ActivityLoginBinding
import com.gameora.ui.common.BaseActivity
import com.gameora.util.UiState
import com.gameora.util.toast

class LoginActivity : BaseActivity<ActivityLoginBinding>(ActivityLoginBinding::inflate) {

    private val vm by lazy { androidx.lifecycle.ViewModelProvider(this)[AuthViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.loginButton.setOnClickListener {
            val email = binding.emailField.text?.toString()?.trim().orEmpty()
            val pass = binding.passwordField.text?.toString().orEmpty()
            if (email.isEmpty() || pass.isEmpty()) {
                toast(getString(com.gameora.R.string.error_generic))
                return@setOnClickListener
            }
            vm.login(email, pass)
        }

        binding.toRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        vm.state.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.loginProgress.visibility = android.view.View.VISIBLE
                    binding.loginButton.isEnabled = false
                }
                is UiState.Success -> {
                    binding.loginProgress.visibility = android.view.View.GONE
                    setResult(RESULT_OK)
                    finish()
                }
                is UiState.Error -> {
                    binding.loginProgress.visibility = android.view.View.GONE
                    binding.loginButton.isEnabled = true
                    toast(state.message)
                }
                else -> {}
            }
        }
    }
}
