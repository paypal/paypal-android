package com.paypal.android.ui.selectcard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel

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
                    SelectCardView()
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun SelectCardView(viewModel: SelectCardViewModel = viewModel()) {
        LazyColumn {
            stickyHeader {
                Text("Cards without 3DS")
            }
            items(viewModel.nonThreeDSCards) { card ->
                Text(card.name)
            }
            stickyHeader {
                Text("Cards with 3DS")
            }
            items(viewModel.threeDSCards) { card ->
                Text(card.name)
            }
        }
    }
}
