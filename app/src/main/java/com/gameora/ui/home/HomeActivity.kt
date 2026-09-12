package com.gameora.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityHomeBinding
import com.gameora.domain.model.Category
import com.gameora.domain.model.Game
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.GenericAdapter
import com.gameora.ui.common.Images
import com.gameora.ui.common.Nav
import com.gameora.ui.common.StateView
import com.gameora.ui.offers.OffersActivity
import com.gameora.ui.products.ProductsActivity
import com.gameora.ui.profile.ProfileActivity
import com.gameora.ui.wallet.WalletActivity
import com.gameora.util.UiState

class HomeActivity :
    BaseActivity<ActivityHomeBinding>(ActivityHomeBinding::inflate) {

    private val vm by lazy {
        ViewModelProvider(this)[HomeViewModel::class.java]
    }

    private lateinit var stateView: StateView

    // ==========================================
    // الألعاب
    // ==========================================

    private val gamesAdapter = GenericAdapter<Game>(
        layoutRes = R.layout.item_game,

        onBind = { v, game, _ ->

            Images.load(
                v.findViewById<ImageView>(R.id.game_icon),
                game.iconUrl ?: game.imageUrl
            )

            v.findViewById<TextView>(R.id.game_name).text =
                game.name
        },

        onClick = { game, _ ->

            // عند الضغط على اللعبة
            // نفتح صفحة خدمات اللعبة
            startActivity(
                Intent(
                    this,
                    ProductsActivity::class.java
                ).apply {
                    putExtra(Nav.GAME_ID, game.id)
                    putExtra(Nav.TITLE, game.name)
                }
            )
        }
    )

    // ==========================================
    // التصنيفات
    // ==========================================

    private val categoriesAdapter = GenericAdapter<Category>(
        layoutRes = R.layout.item_category,

        onBind = { v, category, _ ->

            Images.load(
                v.findViewById<ImageView>(R.id.category_icon),
                category.iconUrl ?: category.imageUrl
            )

            v.findViewById<TextView>(R.id.category_name).text =
                category.name
        }
    )

    // ==========================================
    // إنشاء الصفحة
    // ==========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ==========================================
        // State
        // ==========================================

        stateView = StateView(
            binding.stateView.root
        ) {
            vm.load()
        }

        // ==========================================
        // الألعاب
        // ==========================================

        binding.gamesRecycler.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        binding.gamesRecycler.adapter =
            gamesAdapter

        // ==========================================
        // التصنيفات
        // ==========================================

        binding.categoriesRecycler.layoutManager =
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )

        binding.categoriesRecycler.adapter =
            categoriesAdapter

        // ==========================================
        // البحث
        // ==========================================

        binding.searchInput.isFocusable = false
        binding.searchInput.isClickable = true

        binding.searchInput.setOnClickListener {
            openStore()
        }

        binding.searchCard.setOnClickListener {
            openStore()
        }

        // ==========================================
        // العروض
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
        // Bottom Navigation
        // ==========================================

        // الرئيسية
        binding.navHome.setOnClickListener {
            // نحن بالفعل في الصفحة الرئيسية
        }

        // المتجر
        binding.navStore.setOnClickListener {
            openStore()
        }

        // المحفظة
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

        // البروفايل
        binding.navProfile.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ProfileActivity::class.java
                )
            )
        }

        // ==========================================
        // بيانات الألعاب
        // ==========================================

        vm.games.observe(this) { state ->

            stateView.bind(state)

            if (state is UiState.Success) {

                gamesAdapter.submit(
                    state.data
                )
            }
        }

        // ==========================================
        // بيانات التصنيفات
        // ==========================================

        vm.categories.observe(this) { state ->

            if (state is UiState.Success) {

                categoriesAdapter.submit(
                    state.data
                )
            }
        }

        // ==========================================
        // تحميل البيانات
        // ==========================================

        vm.load()
    }

    // ==========================================
    // فتح المتجر
    // ==========================================

    private fun openStore() {

        startActivity(
            Intent(
                this,
                ProductsActivity::class.java
            )
        )
    }
    }
