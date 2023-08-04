package com.paypal.android.ui.vault

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment

class VaultFragment : Fragment() {

    @ExperimentalMaterial3Api
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        VaultView()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @ExperimentalMaterial3Api
    @Composable
    fun VaultView(
    ) {

    }

    @ExperimentalMaterial3Api
    @Preview
    @Composable
    fun VaultViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                VaultView()
            }
        }
    }
}