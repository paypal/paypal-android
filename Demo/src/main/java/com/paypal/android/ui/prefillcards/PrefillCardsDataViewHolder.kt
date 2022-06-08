package com.paypal.android.ui.prefillcards

import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemPrefillCardDataBinding

class PrefillCardsDataViewHolder(
    private val binding: ItemPrefillCardDataBinding,
    private val listener: PrefillCardsListener
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: PrefillCardsItem.Data) {
        binding.run {
            name.text = item.testCard.name
            cardNumber.text = item.testCard.card.number

            root.setOnClickListener {
                listener.onPrefillCardSelected(item.testCard)
            }
        }
    }
}
