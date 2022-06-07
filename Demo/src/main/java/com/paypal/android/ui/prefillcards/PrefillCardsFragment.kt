package com.paypal.android.ui.prefillcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.paypal.android.data.card.TestCards
import com.paypal.android.databinding.FragmentPrefillCardsBinding

class PrefillCardsFragment : Fragment() {

    private lateinit var binding: FragmentPrefillCardsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPrefillCardsBinding.inflate(inflater, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = mutableListOf<PrefillCardsItem>()

        for (i in 0 until TestCards.numGroups) {
            val group = TestCards.Group.values()[i]
            items += PrefillCardsHeader(group.name)

            val prefillCards = TestCards.cardsInGroup(group)
            items += prefillCards.map { PrefillCardsData(it) }
        }

        binding.run {
            recyclerView.adapter = PrefillCardsAdapter(items)
        }
    }
}