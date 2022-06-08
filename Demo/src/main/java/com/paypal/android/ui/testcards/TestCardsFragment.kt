package com.paypal.android.ui.testcards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.paypal.android.data.card.TestCard
import com.paypal.android.databinding.FragmentTestCardsBinding

class TestCardsFragment : Fragment(), TestCardsListener {

    companion object {
        const val REQUEST_KEY = "TEST_CARD"

        const val RESULT_EXTRA_CARD_NUMBER = "TEST_CARD_NUMBER"
        const val RESULT_EXTRA_CARD_EXPIRATION_MONTH = "TEST_CARD_EXPIRATION_MONTH"
        const val RESULT_EXTRA_CARD_EXPIRATION_YEAR = "TEST_CARD_EXPIRATION_YEAR"
        const val RESULT_EXTRA_CARD_SECURITY_CODE = "TEST_CARD_SECURITY_CODE"
    }

    private val viewModel: TestCardsViewModel by viewModels()

    private lateinit var binding: FragmentTestCardsBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTestCardsBinding.inflate(inflater, null, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = mutableListOf<TestCardsItem>()
        for (group in viewModel.testCardGroups) {
            items += TestCardsItem.Header(group.name)
            items += group.cards.map { TestCardsItem.Data(it) }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = TestCardsAdapter(items, this)
    }

    override fun onTestCardSelected(testCard: TestCard) {
        val bundle = testCard.card.run {
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