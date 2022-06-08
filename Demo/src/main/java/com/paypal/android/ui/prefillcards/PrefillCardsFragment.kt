package com.paypal.android.ui.prefillcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.paypal.android.data.card.PrefillCard
import com.paypal.android.data.card.TestCards
import com.paypal.android.databinding.FragmentPrefillCardsBinding

class PrefillCardsFragment : Fragment(), PrefillCardsListener {

    private lateinit var binding: FragmentPrefillCardsBinding

    companion object {
        const val REQUEST_KEY = "PREFILL_CARD"

        const val RESULT_EXTRA_CARD_NUMBER = "PREFILL_CARD_NUMBER"
        const val RESULT_EXTRA_CARD_EXPIRATION_MONTH = "PREFILL_CARD_EXPIRATION_MONTH"
        const val RESULT_EXTRA_CARD_EXPIRATION_YEAR = "PREFILL_CARD_EXPIRATION_YEAR"
        const val RESULT_EXTRA_CARD_SECURITY_CODE = "PREFILL_CARD_SECURITY_CODE"
    }

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
            items += PrefillCardsItem.Header(group.name)

            val prefillCards = TestCards.cardsInGroup(group)
            items += prefillCards.map { PrefillCardsItem.Data(it) }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = PrefillCardsAdapter(items, this)
    }

    override fun onPrefillCardSelected(prefillCard: PrefillCard) {
        val bundle = prefillCard.card.run {
            Bundle().apply {
                putString(RESULT_EXTRA_CARD_NUMBER, number)
                putString(RESULT_EXTRA_CARD_EXPIRATION_MONTH, expirationMonth)
                putString(RESULT_EXTRA_CARD_EXPIRATION_YEAR, expirationYear)
                putString(RESULT_EXTRA_CARD_SECURITY_CODE, securityCode)
            }
        }

        setFragmentResult(REQUEST_KEY, bundle)
        findNavController().popBackStack()
    }
}