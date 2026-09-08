package com.gameora.ui.productdetail

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityProductDetailBinding
import com.gameora.domain.model.Review
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.ui.seller.SellerActivity
import com.gameora.util.UiState
import com.gameora.util.toast

class ProductDetailActivity : BaseActivity<ActivityProductDetailBinding>(ActivityProductDetailBinding::inflate) {

    private val vm by lazy { ViewModelProvider(this)[ProductDetailViewModel::class.java] }
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
        stateView = StateView(binding.stateView.root) { vm.load(productId()) }
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.reviewsRecycler.layoutManager = LinearLayoutManager(this)
        binding.reviewsRecycler.adapter = reviewsAdapter

        binding.detailSellerCard.setOnClickListener {
            vm.seller.value?.let { s ->
                startActivity(android.content.Intent(this, SellerActivity::class.java).apply {
                    putExtra(Nav.SELLER_ID, s.id)
                    putExtra(Nav.TITLE, s.displayName ?: s.username)
                })
            }
        }
        binding.detailBuy.setOnClickListener {
            requireLogin { vm.buy(productId()); toast(R.string.order_created) }
        }
        binding.detailContact.setOnClickListener { requireLogin { toast(R.string.contact_via_chat) } }

        vm.product.observe(this) { state ->
            stateView.bind(state)
            if (state is UiState.Success) {
                val p = state.data
                binding.detailTitle.text = p.title
                binding.detailPrice.text = Formatters.price(p.price, p.currency)
                binding.detailMeta.text = listOfNotNull(p.rank, p.level, p.server).joinToString(" · ")
                binding.detailDescription.text = p.description ?: ""
                Images.load(binding.detailBanner, p.images.firstOrNull())
            }
        }
        vm.seller.observe(this) { s ->
            if (s != null) {
                Images.avatar(binding.sellerAvatar, s.avatarUrl)
                binding.sellerName.text = s.displayName ?: s.username
                binding.sellerRating.text = Formatters.rating(s.rating)
                binding.sellerVerified.visibility = if (s.verified) View.VISIBLE else View.GONE
            }
        }
        vm.reviews.observe(this) { reviewsAdapter.submit(it) }
        vm.buy.observe(this) { if (it is UiState.Error) toast(it.message) }

        vm.load(productId())
    }

    private fun productId(): String = intent.getStringExtra(Nav.PRODUCT_ID).orEmpty()
}
