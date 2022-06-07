package com.paypal.android.ui.prefillcards

import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemPrefillCardDataBinding

class PrefillCardsDataViewHolder(private val binding: ItemPrefillCardDataBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PrefillCardsItem.Data) {
        binding.run {
            name.text = item.prefillCard.name

            root.setOnClickListener {

            }
        }
    }
}
