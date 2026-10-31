package com.gameora.ui.common

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.gameora.GameoraApp
import com.gameora.di.AppContainer
import com.gameora.ui.auth.LoginActivity
import com.gameora.util.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Base activity that wires ViewBinding and exposes the [AppContainer].
 */
abstract class BaseActivity<VB : ViewBinding>(
    private val inflate: (LayoutInflater) -> VB
) : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB
        get() = _binding!!

    protected val container: AppContainer
        get() = (application as GameoraApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /**
     * Runs [then] only when the user is authenticated.
     *
     * The Firebase session may still be restoring immediately
     * after application startup, so we wait briefly instead of
     * treating a null state as an automatic logout.
     */
    protected fun requireLogin(then: () -> Unit) {
        lifecycleScope.launch {

            repeat(30) {
                when (container.sessionManager.isLoggedIn.value) {

                    true -> {
                        then()
                        return@launch
                    }

                    false -> {
                        toast(com.gameora.R.string.login_required)
                        startActivity(
                            Intent(
                                this@BaseActivity,
                                LoginActivity::class.java
                            )
                        )
                        return@launch
                    }

                    null -> {
                        delay(100)
                    }
                }
            }

            // Session restoration took too long.
            // Check Firebase directly before asking for login.
            if (container.authRepository.isLoggedIn()) {
                then()
            } else {
                toast(com.gameora.R.string.login_required)
                startActivity(
                    Intent(
                        this@BaseActivity,
                        LoginActivity::class.java
                    )
                )
            }
        }
    }
}
