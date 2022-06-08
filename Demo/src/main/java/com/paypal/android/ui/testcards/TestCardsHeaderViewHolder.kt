package com.paypal.android.ui.testcards

import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemTestCardsHeaderBinding

class TestCardsHeaderViewHolder(private val binding: ItemTestCardsHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(item: TestCardsItem.Header) {
        binding.run {
            title.text = item.title
        }
    }
}
