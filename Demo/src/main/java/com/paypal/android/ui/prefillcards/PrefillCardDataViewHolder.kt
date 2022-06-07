package com.paypal.android.ui.prefillcards

import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemPrefillCardDataBinding

class PrefillCardDataViewHolder(private val binding: ItemPrefillCardDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PrefillCardsData) {
        binding.run {
            name.text = item.prefillCard.name
        }
    }
}
