package com.paypal.android.ui.testcards

import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemTestCardsDataBinding

class TestCardsDataViewHolder(
    private val binding: ItemTestCardsDataBinding,
    private val listener: TestCardsListener
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TestCardsItem.Data) {
        binding.run {
            name.text = item.testCard.name
            cardNumber.text = item.testCard.card.number

            root.setOnClickListener {
                listener.onTestCardSelected(item.testCard)
            }
        }
    }
}
