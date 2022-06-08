package com.paypal.android.ui.testcards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemTestCardsDataBinding
import com.paypal.android.databinding.ItemTestCardsHeaderBinding


class TestCardsAdapter(
    private val items: MutableList<TestCardsItem>,
    private val listener: TestCardsListener
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private enum class ViewType(val value: Int) {
        HEADER(0),
        DATA(1);

        companion object {
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

    override fun getItemViewType(position: Int) =
        when (items[position]) {
            is TestCardsItem.Header -> ViewType.HEADER.value
            else -> ViewType.DATA.value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (ViewType.fromInt(viewType)) {
            ViewType.HEADER -> {
                val binding =
                    ItemTestCardsHeaderBinding.inflate(layoutInflater, parent, false)
                TestCardsHeaderViewHolder(binding)
            }
            else -> {
                val binding =
                    ItemTestCardsDataBinding.inflate(layoutInflater, parent, false)
                TestCardsDataViewHolder(binding, listener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TestCardsHeaderViewHolder -> {
                (items[position] as? TestCardsItem.Header)?.let { holder.bind(it) }
            }
            is TestCardsDataViewHolder -> {
                (items[position] as? TestCardsItem.Data)?.let { holder.bind(it) }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
