package com.paypal.android.ui.testcards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemPrefillCardDataBinding
import com.paypal.android.databinding.ItemPrefillCardsHeaderBinding


class PrefillCardsAdapter(
    private val items: MutableList<PrefillCardsItem>,
    private val listener: PrefillCardsListener
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
            is PrefillCardsItem.Header -> ViewType.HEADER.value
            else -> ViewType.DATA.value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (ViewType.fromInt(viewType)) {
            ViewType.HEADER -> {
                val binding =
                    ItemPrefillCardsHeaderBinding.inflate(layoutInflater, parent, false)
                PrefillCardsHeaderViewHolder(binding)
            }
            else -> {
                val binding =
                    ItemPrefillCardDataBinding.inflate(layoutInflater, parent, false)
                PrefillCardsDataViewHolder(binding, listener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PrefillCardsHeaderViewHolder -> {
                (items[position] as? PrefillCardsItem.Header)?.let { holder.bind(it) }
            }
            is PrefillCardsDataViewHolder -> {
                (items[position] as? PrefillCardsItem.Data)?.let { holder.bind(it) }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
