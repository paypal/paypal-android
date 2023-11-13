package com.paypal.android.ui.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.paypal.android.R

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
