package com.paypal.android.ui.selectcard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.data.card.TestCard
import androidx.navigation.fragment.findNavController

class SelectCardFragment : Fragment() {

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

    private fun navigateToCardForm(card: TestCard? = null) {
        findNavController().navigate(
            SelectCardFragmentDirections.actionSelectCardFragmentToCardFragment(card)
        )
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
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .selectable(
                            selected = false,
                            onClick = { navigateToCardForm() }
                        )
                ) {
                    Text("ENTER CARD MANUALLY")
                }
            }
            stickyHeader {
                TestCardHeader("Cards without 3DS")
            }
            items(viewModel.nonThreeDSCards) { card ->
                TestCardView(
                    card = card,
                    selected = false,
                    onClick = {
                        onTestCardSelected(card)
                    }
                )
            }
            stickyHeader {
                TestCardHeader("Cards with 3DS")
            }
            items(viewModel.threeDSCards) { card ->
                TestCardView(
                    card = card,
                    selected = false,
                    onClick = {
                        onTestCardSelected(card)
                    }
                )
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
    fun TestCardHeader(text: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            Text(
                text = text,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    @Composable
    fun TestCardView(card: TestCard, selected: Boolean, onClick: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    onClick = onClick
                )
        ) {
            Text(
                text = card.name,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
