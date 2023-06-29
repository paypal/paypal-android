package com.paypal.android.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.paypal.android.databinding.FragmentPaymentMethodsBinding

class PaymentMethodsFragment : Fragment() {

    private lateinit var binding: FragmentPaymentMethodsBinding

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
                        onCardMethodSelected = ::launchCardFragment,
                        onPayPalMethodSelected = ::launchPayPalFragment,
                        onPayPalNativeSelected = ::launchPayPalNativeFragment
                    )
                }
            }
        }
    }

    private fun launchPayPalFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToPayPalFragment())
    }

    private fun launchCardFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToSelectCardFragment())
    }

    private fun launchPayPalNativeFragment() {
        navigate(PaymentMethodsFragmentDirections.actionPaymentMethodsFragmentToPayPalNativeFragment())
    }

    private fun navigate(action: NavDirections) {
        findNavController().navigate(action)
    }

    @Composable
    fun PaymentMethodsView(
        onCardMethodSelected: () -> Unit,
        onPayPalMethodSelected: () -> Unit,
        onPayPalNativeSelected: () -> Unit
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Button(
                onClick = onCardMethodSelected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "CARD")
            }
            Button(
                onClick = onPayPalMethodSelected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "PAYPAL")
            }
            Button(
                onClick = onPayPalNativeSelected,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "PAYPAL NATIVE")
            }
        }
    }

    @Preview
    @Composable
    fun PaymentMethodsViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                PaymentMethodsView(
                    onCardMethodSelected = {},
                    onPayPalMethodSelected = {},
                    onPayPalNativeSelected = {}
                )
            }
        }
    }
}
