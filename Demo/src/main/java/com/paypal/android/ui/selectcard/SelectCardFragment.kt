package com.paypal.android.ui.selectcard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import com.paypal.android.models.TestCard

class SelectCardFragment : Fragment() {

    companion object {
        const val REQUEST_KEY_TEST_CARD = "SELECT_CARD_REQUEST_KEY_TEST_CARD"
        const val DATA_KEY_TEST_CARD = "SELECT_CARD_DATA_KEY_TEST_CARD"
    }

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SelectCardView(onTestCardSelected = { onTestCardSelected(it) })
                }
            }
        }
    }

    private fun onTestCardSelected(testCard: TestCard) {
        val bundle = bundleOf(DATA_KEY_TEST_CARD to testCard)
        setFragmentResult(REQUEST_KEY_TEST_CARD, bundle)

        // go back to previous fragment with test card as a result
        findNavController().navigateUp()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @ExperimentalMaterial3Api
    @Composable
    fun SelectCardView(
        viewModel: SelectCardViewModel = viewModel(),
        onTestCardSelected: (TestCard) -> Unit = {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            stickyHeader {
                TestCardGroupHeader("Verified Test Cards")
            }
            items(viewModel.verifiedTestCards) { card ->
                TestCardView(
                    testCard = card
                ) {
                    onTestCardSelected(card)
                }
            }
            stickyHeader {
                TestCardGroupHeader("Test Cards without 3DS")
            }
            items(viewModel.nonThreeDSCards) { card ->
                TestCardView(
                    testCard = card
                ) {
                    onTestCardSelected(card)
                }
            }
            stickyHeader {
                TestCardGroupHeader("Test Cards with 3DS")
            }
            items(viewModel.threeDSCards) { card ->
                TestCardView(
                    testCard = card
                ) {
                    onTestCardSelected(card)
                }
            }
        }
    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun SelectCardViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                SelectCardView()
            }
        }
    }

    @ExperimentalMaterial3Api
    @Composable
    fun TestCardView(testCard: TestCard, onClick: () -> Unit) {
        OutlinedCard(
            onClick = { onClick() }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = testCard.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
                )
                Text(
                    text = testCard.formattedCardNumber,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                )
            }
        }
    }

    @Composable
    fun TestCardGroupHeader(text: String) {
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text = text,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.size(12.dp))
    }
}
