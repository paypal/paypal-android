package com.paypal.android.ui.prefillcards

import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemPrefillCardsHeaderBinding

class HeaderViewHolder(private val binding: ItemPrefillCardsHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PrefillCardsHeader) {
        binding.run {
            title.text = item.title
        }
    }
}