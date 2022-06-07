package com.paypal.android.ui.prefillcards

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.paypal.android.databinding.ItemPrefillCardDataBinding
import com.paypal.android.databinding.ItemPrefillCardsHeaderBinding


class PrefillCardsAdapter(private val items: MutableList<PrefillCardsItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private enum class ViewType(val value: Int) {
        HEADER(0),
        DATA(1);

        companion object {
            fun fromInt(value: Int) = ViewType.values().first { it.value == value }
        }
    }

    override fun getItemViewType(position: Int) =
        when (items[position]) {
            is PrefillCardsHeader -> ViewType.HEADER.value
            else -> ViewType.HEADER.value
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (ViewType.fromInt(viewType)) {
            ViewType.HEADER -> {
                val binding =
                    ItemPrefillCardsHeaderBinding.inflate(layoutInflater, null, false)
                HeaderViewHolder(binding)
            }
            else -> {
                val binding =
                    ItemPrefillCardDataBinding.inflate(layoutInflater, null, false)
                PrefillCardDataViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                (items[position] as? PrefillCardsHeader)?.let { holder.bind(it) }
            }
            is PrefillCardDataViewHolder -> {
                (items[position] as? PrefillCardsData)?.let { holder.bind(it) }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
