package com.paypal.android.ui.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class FeaturesFragment : Fragment() {

    @ExperimentalFoundationApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    FeaturesView(onFeatureSelected = ::onFeatureSelected)
                }
            }
        }
    }

    private fun onFeatureSelected(feature: Feature) {
        val action = when (feature) {
            Feature.PAYPAL_BUTTONS -> {
                FeaturesFragmentDirections.actionPaymentMethodsFragmentToPayPalButtonsFragment()
            }

            Feature.PAYPAL_WEB -> {
                FeaturesFragmentDirections.actionPaymentMethodsFragmentToPayPalWebFragment()
            }

            Feature.PAYPAL_NATIVE -> {
                FeaturesFragmentDirections.actionPaymentMethodsFragmentToPayPalNativeFragment()
            }

            Feature.CARD_APPROVE_ORDER -> {
                FeaturesFragmentDirections.actionPaymentMethodsFragmentToCardFragment()
            }

            Feature.CARD_VAULT -> {
                FeaturesFragmentDirections.actionPaymentMethodsFragmentToVaultFragment()
            }
        }
        findNavController().navigate(action)
    }

    @ExperimentalFoundationApi
    @Preview
    @Composable
    fun PaymentMethodsViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                FeaturesView(onFeatureSelected = {})
            }
        }
    }
}
