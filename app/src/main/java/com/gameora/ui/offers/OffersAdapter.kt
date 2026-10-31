package com.gameora.ui.offers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gameora.data.remote.dto.OfferDto
import com.gameora.databinding.ItemOfferBinding
import com.gameora.ui.common.Formatters
import com.gameora.ui.common.Images

class OffersAdapter :
    ListAdapter<OfferDto, OffersAdapter.OfferViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OfferViewHolder {
        val binding = ItemOfferBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return OfferViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: OfferViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    class OfferViewHolder(
        private val binding: ItemOfferBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(offer: OfferDto) {

            binding.offerTitle.text = offer.title

            binding.offerDescription.text =
                offer.description.orEmpty()

            binding.offerOldPrice.text =
                Formatters.price(
                    offer.oldPrice,
                    offer.currency
                )

            binding.offerFinalPrice.text =
                Formatters.price(
                    offer.finalPrice,
                    offer.currency
                )

            binding.offerDiscount.text =
                "${offer.discountPercent.toInt()}% خصم"

            Images.load(
                binding.offerImage,
                offer.imageUrl
            )
        }
    }

    private class DiffCallback :
        DiffUtil.ItemCallback<OfferDto>() {

        override fun areItemsTheSame(
            oldItem: OfferDto,
            newItem: OfferDto
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: OfferDto,
            newItem: OfferDto
        ): Boolean {
            return oldItem == newItem
        }
    }
    }
