package com.gameora.ui.seller

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivitySellerBinding
import com.gameora.domain.model.Review
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

class SellerActivity : BaseActivity<ActivitySellerBinding>(ActivitySellerBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[SellerViewModel::class.java] }
    private lateinit var stateView: StateView

    private val reviewsAdapter = GenericAdapter<Review>(
        layoutRes = R.layout.item_review,
        onBind = { v, r, _ ->
            Images.avatar(v.findViewById<ImageView>(R.id.review_avatar), r.authorAvatarUrl)
            v.findViewById<TextView>(R.id.review_author).text = r.authorName ?: "—"
            v.findViewById<TextView>(R.id.review_rating).text = Formatters.rating(r.rating)
            v.findViewById<TextView>(R.id.review_comment).text = r.comment ?: ""
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stateView = StateView(binding.stateView.root) { vm.load(sellerId()) }
        binding.toolbar.title = intent.getStringExtra(Nav.TITLE) ?: getString(R.string.seller_info)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.reviewsRecycler.layoutManager = LinearLayoutManager(this)
        binding.reviewsRecycler.adapter = reviewsAdapter

        vm.seller.observe(this) { state ->
            stateView.bind(state)
            if (state is UiState.Success) {
                val s = state.data
                Images.avatar(binding.sellerAvatar, s.avatarUrl)
                binding.sellerName.text = s.displayName ?: s.username
                binding.sellerUsername.text = s.username ?: ""
                binding.sellerRating.text = Formatters.rating(s.rating)
                binding.sellerVerified.visibility = if (s.verified) View.VISIBLE else View.GONE
            }
        }
        vm.reviews.observe(this) { reviewsAdapter.submit(it) }

        vm.load(sellerId())
    }

    private fun sellerId(): String = intent.getStringExtra(Nav.SELLER_ID).orEmpty()
}
