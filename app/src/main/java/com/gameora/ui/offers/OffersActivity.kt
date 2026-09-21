package com.gameora.ui.offers

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.gameora.R
import com.gameora.databinding.ActivityOffersBinding
import com.gameora.ui.common.BaseActivity
import com.gameora.ui.common.StateView
import com.gameora.util.UiState

class OffersActivity :
    BaseActivity<ActivityOffersBinding>(ActivityOffersBinding::inflate) {

    private val vm by lazy {
        ViewModelProvider(this)[OffersViewModel::class.java]
    }

    private lateinit var stateView: StateView

    private val adapter = OffersAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        stateView = StateView(binding.stateView.root) {
            vm.load()
        }

        binding.offersRecycler.layoutManager =
            LinearLayoutManager(this)

        binding.offersRecycler.adapter = adapter

        binding.backButton.setOnClickListener {
            finish()
        }

        vm.offers.observe(this) { state ->
            stateView.bind(state)

            if (state is UiState.Success) {
                adapter.submitList(state.data)
            }
        }

        vm.load()
    }
    }
