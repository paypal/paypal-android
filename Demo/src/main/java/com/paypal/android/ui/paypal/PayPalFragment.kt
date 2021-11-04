package com.paypal.android.ui.paypal

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.ui.theme.DemoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PayPalFragment : Fragment() {

    private val payPalViewModel: PayPalViewModel by viewModels()

    private val canRunPayPalCheckout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val coreConfig = CoreConfig(BuildConfig.CLIENT_ID, Environment.SANDBOX)
        val application = requireActivity().application
        val returnUrl = BuildConfig.APPLICATION_ID + "://paypalpay"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            payPalViewModel.setPayPalClient(PayPalClient(application, coreConfig, returnUrl))
        }
        return ComposeView(requireContext()).apply {
            setContent {
                PayPalFragmentView()
            }
        }
    }

    @Preview
    @Composable
    fun PayPalFragmentView() = DemoTheme {
        Column {
                Button(
                    enabled = canRunPayPalCheckout,
                    onClick = { launchNativeCheckout() },
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)
                        .fillMaxWidth()
                ) { Text(stringResource(R.string.start_checkout)) }
            if (!canRunPayPalCheckout) Text(text = stringResource(id = R.string.minimum_sdk_needed))
        }
    }

    private fun launchNativeCheckout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            payPalViewModel.startPayPalCheckout()
        }
    }
}
