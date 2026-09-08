package com.gameora.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.gameora.R
import com.gameora.databinding.ActivityProfileBinding
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.Images
import com.gameora.ui.common.StateView
import com.gameora.ui.auth.LoginActivity
import com.gameora.ui.products.ProductsActivity
import com.gameora.ui.sell.SellActivity
import com.gameora.util.UiState

class ProfileActivity : BaseActivity<ActivityProfileBinding>(ActivityProfileBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[ProfileViewModel::class.java] }
    private lateinit var stateView: StateView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load() }
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.profileSell.setOnClickListener {
            requireLogin { startActivity(Intent(this, SellActivity::class.java)) }
        }
        binding.profileMyProducts.setOnClickListener {
            requireLogin { startActivity(Intent(this, ProductsActivity::class.java).apply {
                putExtra(com.gameora.ui.common.Nav.TITLE, getString(R.string.my_products))
            }) }
        }
        binding.profileLogout.setOnClickListener { vm.logout() }

        vm.user.observe(this) { state ->
            stateView.bind(state)
            if (state is UiState.Success) {
                val u = state.data
                Images.avatar(binding.profileAvatar, u.avatarUrl)
                binding.profileName.text = u.displayName ?: u.username
                binding.profileUsername.text = u.username
                binding.profileEmail.text = u.email
                binding.profileRating.text = Formatters.rating(u.rating)
                binding.profileVerified.visibility = if (u.verified) View.VISIBLE else View.GONE
            }
        }
        vm.loggedOut.observe(this) { out ->
            if (out) {
                startActivity(Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }
        }

        vm.load()
    }
}
