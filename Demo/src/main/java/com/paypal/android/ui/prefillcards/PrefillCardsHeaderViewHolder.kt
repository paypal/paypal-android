package com.paypal.android.ui.prefillcards

import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemPrefillCardsHeaderBinding

class PrefillCardsHeaderViewHolder(private val binding: ItemPrefillCardsHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PrefillCardsItem.Header) {
        binding.run {
            title.text = item.title
        }
    }
}