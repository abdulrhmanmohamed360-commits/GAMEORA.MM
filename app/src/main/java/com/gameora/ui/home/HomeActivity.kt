package com.gameora.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityHomeBinding
import com.gameora.domain.model.Category
import com.gameora.domain.model.Game
import com.gameora.domain.model.Product
import com.gameora.ui.chat.ConversationsActivity
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.ui.notifications.NotificationsActivity
import com.gameora.ui.offers.OffersActivity
import com.gameora.ui.orders.OrdersActivity
import com.gameora.ui.productdetail.ProductDetailActivity
import com.gameora.ui.products.ProductsActivity
import com.gameora.ui.profile.ProfileActivity
import com.gameora.ui.sell.SellActivity
import com.gameora.ui.wallet.WalletActivity
import com.gameora.util.UiState

class HomeActivity :
    BaseActivity<ActivityHomeBinding>(ActivityHomeBinding::inflate) {

    private val vm by lazy {
        ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private lateinit var stateView: StateView

    private val gamesAdapter = GenericAdapter<Game>(
        layoutRes = R.layout.item_game,
        onBind = { v, g, _ ->
            Images.load(
                v.findViewById<ImageView>(R.id.game_icon),
                g.iconUrl ?: g.imageUrl
            )

            v.findViewById<TextView>(R.id.game_name).text = g.name
        },
        onClick = { g, _ ->
            startActivity(
                Intent(this, ProductsActivity::class.java).apply {
                    putExtra(Nav.GAME_ID, g.id)
                    putExtra(Nav.TITLE, g.name)
                }
            )
        }
    )

    private val categoriesAdapter = GenericAdapter<Category>(
        layoutRes = R.layout.item_category,
        onBind = { v, c, _ ->
            Images.load(
                v.findViewById<ImageView>(R.id.category_icon),
                c.iconUrl ?: c.imageUrl
            )

            v.findViewById<TextView>(R.id.category_name).text = c.name
        }
    )

    private val recentAdapter = GenericAdapter<Product>(
        layoutRes = R.layout.item_product,
        onBind = { v, p, _ ->

            Images.load(
                v.findViewById<ImageView>(R.id.product_image),
                p.images.firstOrNull()
            )

            v.findViewById<TextView>(R.id.product_title).text = p.title

            v.findViewById<TextView>(R.id.product_price).text =
                Formatters.price(
                    p.price,
                    p.currency
                )

            v.findViewById<TextView>(R.id.product_meta).text =
                listOfNotNull(
                    p.rank,
                    p.level,
                    p.server
                ).joinToString(" · ")

            v.findViewById<TextView>(R.id.product_seller).visibility =
                View.GONE
        },
        onClick = { p, _ ->
            startActivity(
                Intent(this, ProductDetailActivity::class.java).apply {
                    putExtra(Nav.PRODUCT_ID, p.id)
                }
            )
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateView = StateView(
            binding.stateView.root
        ) {
            vm.load()
        }

        // الألعاب
        binding.gamesRecycler.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // التصنيفات
        binding.categoriesRecycler.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        // المنتجات الأخيرة
        binding.recentRecycler.layoutManager =
            GridLayoutManager(this, 2)

        binding.gamesRecycler.adapter =
            gamesAdapter

        binding.categoriesRecycler.adapter =
            categoriesAdapter

        binding.recentRecycler.adapter =
            recentAdapter

        // البحث
        binding.searchInput.isFocusable = false
        binding.searchInput.isClickable = true

        binding.searchInput.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProductsActivity::class.java
                )
            )
        }

        binding.searchCard.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProductsActivity::class.java
                )
            )
        }

        // ==========================================
        // Banner العروض
        // ==========================================

        binding.offersBanner.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    OffersActivity::class.java
                )
            )
        }

        // ==========================================
        // التنقل
        // ==========================================

        binding.navSell.setOnClickListener {
            requireLogin {
                startActivity(
                    Intent(
                        this,
                        SellActivity::class.java
                    )
                )
            }
        }

        binding.navWallet.setOnClickListener {
            requireLogin {
                startActivity(
                    Intent(
                        this,
                        WalletActivity::class.java
                    )
                )
            }
        }

        binding.navOrders.setOnClickListener {
            requireLogin {
                startActivity(
                    Intent(
                        this,
                        OrdersActivity::class.java
                    )
                )
            }
        }

        binding.navChat.setOnClickListener {
            requireLogin {
                startActivity(
                    Intent(
                        this,
                        ConversationsActivity::class.java
                    )
                )
            }
        }

        binding.navNotifications.setOnClickListener {
            requireLogin {
                startActivity(
                    Intent(
                        this,
                        NotificationsActivity::class.java
                    )
                )
            }
        }

        binding.navProfile.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }

        // ==========================================
        // بيانات الصفحة الرئيسية
        // ==========================================

        vm.games.observe(this) { state ->
            stateView.bind(state)

            if (state is UiState.Success) {
                gamesAdapter.submit(state.data)
            }
        }

        vm.categories.observe(this) { state ->
            if (state is UiState.Success) {
                categoriesAdapter.submit(state.data)
            }
        }

        vm.recent.observe(this) { state ->
            if (state is UiState.Success) {
                recentAdapter.submit(state.data)
            }
        }

        vm.load()
    }
    }
