package com.paypal.android.ui.selectcard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.paypal.android.models.TestCard
import com.paypal.android.ui.WireframeHeader
import com.paypal.android.ui.features.Feature

class SelectCardFragment : Fragment() {

    private val args: SelectCardFragmentArgs by navArgs()

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

    private fun onTestCardSelected(card: TestCard) {
        navigateToCardForm(card)
    }

    private fun navigateToCardForm(testCard: TestCard? = null) {
        val feature = args.feature
        if (feature == Feature.CARD_VAULT) {
            val action = SelectCardFragmentDirections.actionSelectCardFragmentToVaultFragment(
                feature, testCard
            )
            findNavController().navigate(action)
        } else {
            val order = args.order
            val action = SelectCardFragmentDirections.actionSelectCardFragmentToCardFragment(
                feature,
                order,
                testCard
            )
            findNavController().navigate(action)
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @ExperimentalMaterial3Api
    @Composable
    fun SelectCardView(
        viewModel: SelectCardViewModel = viewModel(),
        onTestCardSelected: (TestCard) -> Unit = {}
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            stickyHeader {
                WireframeHeader("Manual Card Entry")
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(color = 0xFF0079C1)) // PayPal blue
                        .selectable(
                            selected = false,
                            onClick = { navigateToCardForm() }
                        )
                ) {
                    Text(
                        text = "ENTER CARD MANUALLY️",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 26.dp, horizontal = 8.dp),
                    )
                }
            }
            stickyHeader {
                WireframeHeader("Test Cards without 3DS")
            }
            items(viewModel.nonThreeDSCards) { card ->
                TestCardView(
                    testCard = card,
                    selected = false,
                    onClick = {
                        onTestCardSelected(card)
                    }
                )
                Divider(color = Color.Black)
            }
            stickyHeader {
                WireframeHeader("Test Cards with 3DS")
            }
            items(viewModel.threeDSCards) { card ->
                TestCardView(
                    testCard = card,
                    selected = false,
                    onClick = {
                        onTestCardSelected(card)
                    }
                )
                Divider(color = Color.Black)
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

    @Composable
    fun TestCardView(testCard: TestCard, selected: Boolean, onClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick
                )
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
