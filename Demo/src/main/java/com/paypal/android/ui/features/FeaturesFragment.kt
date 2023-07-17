package com.paypal.android.ui.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.paypal.android.ui.WireframeHeader

class FeaturesFragment : Fragment() {

    private val cardFeatures = listOf(
        Feature.CARD_APPROVE_ORDER,
        Feature.CARD_VAULT_WITH_PURCHASE,
        Feature.CARD_VAULT_WITHOUT_PURCHASE
    )

    private val payPalWebFeatures = listOf(
        Feature.PAYPAL_WEB
    )

    private val payPalNativeFeatures = listOf(
        Feature.PAYPAL_NATIVE
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PaymentMethodsView(
                        cardFeatures = cardFeatures,
                        payPalWebFeatures = payPalWebFeatures,
                        payPalNativeFeatures = payPalNativeFeatures,
                        onFeatureSelected = ::onFeatureSelected,
                    )
                }
            }
        }
    }

    private fun onFeatureSelected(feature: Feature) {
        when (feature) {
            Feature.CARD_APPROVE_ORDER -> launchCardFragment()
            Feature.CARD_VAULT_WITH_PURCHASE -> launchCardFragment()
            Feature.CARD_VAULT_WITHOUT_PURCHASE -> launchCardFragment()
            Feature.PAYPAL_WEB -> launchPayPalFragment()
            Feature.PAYPAL_NATIVE -> launchPayPalNativeFragment()
        }
    }

    private fun launchPayPalFragment() {
        navigate(FeaturesFragmentDirections.actionPaymentMethodsFragmentToPayPalFragment())
    }

    private fun launchCardFragment() {
        navigate(FeaturesFragmentDirections.actionPaymentMethodsFragmentToSelectCardFragment())
    }

    private fun launchPayPalNativeFragment() {
        navigate(FeaturesFragmentDirections.actionPaymentMethodsFragmentToPayPalNativeFragment())
    }

    private fun navigate(action: NavDirections) {
        findNavController().navigate(action)
    }

    @Composable
    @OptIn(ExperimentalFoundationApi::class)
    fun PaymentMethodsView(
        cardFeatures: List<Feature>,
        payPalWebFeatures: List<Feature>,
        payPalNativeFeatures: List<Feature>,
        onFeatureSelected: (Feature) -> Unit,
    ) {
        LazyColumn {
            stickyHeader {
                WireframeHeader("Card")
            }
            items(cardFeatures) { feature ->
                FeatureView(feature = feature, onClick = { onFeatureSelected(feature) })
            }
            stickyHeader {
                WireframeHeader("PayPal Web")
            }
            items(payPalWebFeatures) { feature ->
                FeatureView(feature = feature, onClick = { onFeatureSelected(feature) })
            }
            stickyHeader {
                WireframeHeader("PayPal Native")
            }
            items(payPalNativeFeatures) { feature ->
                FeatureView(feature = feature, onClick = { onFeatureSelected(feature) })
            }
        }
    }

    @Preview
    @Composable
    fun PaymentMethodsViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                PaymentMethodsView(
                    cardFeatures = cardFeatures,
                    payPalWebFeatures = payPalWebFeatures,
                    payPalNativeFeatures = payPalNativeFeatures,
                    onFeatureSelected = {},
                )
            }
        }
    }

    @Composable
    fun FeatureView(feature: Feature, onClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = false,
                    onClick = onClick
                )
        ) {
            Text(
                text = stringResource(id = feature.stringRes),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
            )
            Divider()
        }
    }
}
