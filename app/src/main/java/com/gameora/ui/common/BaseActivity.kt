package com.gameora.ui.common

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.gameora.GameoraApp
import com.gameora.di.AppContainer
import com.gameora.ui.auth.LoginActivity
import com.gameora.util.toast

/**
 * Base activity that wires ViewBinding and exposes the [AppContainer] (manual DI).
 * Every screen extends this so binding setup and repository access are uniform.
 */
abstract class BaseActivity<VB : ViewBinding>(
    private val inflate: (LayoutInflater) -> VB
) : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    protected val container: AppContainer get() = (application as GameoraApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /** Runs [then] only when the user is logged in; otherwise launches the login flow. */
    protected fun requireLogin(then: () -> Unit) {
        if (container.sessionManager.isLoggedIn.value == true) {
            then()
        } else {
            toast(com.gameora.R.string.login_required)
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
