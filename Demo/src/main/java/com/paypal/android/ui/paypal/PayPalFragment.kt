package com.paypal.android.ui.paypal

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.checkout.PayPalConfiguration
import com.paypal.android.core.Environment
import com.paypal.android.ui.theme.DemoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PayPalFragment : Fragment() {

    private val payPalViewModel: PayPalViewModel by viewModels()

    private val canRunPayPalCheckout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    private lateinit var paypalConfig: PayPalConfiguration
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        paypalConfig = PayPalConfiguration(
            application = requireActivity().application,
            clientId = BuildConfig.CLIENT_ID,
            returnUrl = BuildConfig.APPLICATION_ID + "://paypalpay",
            environment = Environment.SANDBOX,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            payPalViewModel.setPayPalConfig(paypalConfig)
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
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.user_action))
                LabelRadioButton(
                    label = stringResource(id = R.string.user_action_pay_now),
                    onClick = { payPalViewModel.userActionSelected(getString(R.string.user_action_pay_now)) },
                    selectedLiveData = payPalViewModel.userAction
                )
                LabelRadioButton(
                    label = stringResource(id = R.string.user_action_continue),
                    onClick = { payPalViewModel.userActionSelected(getString(R.string.user_action_continue)) },
                    selectedLiveData = payPalViewModel.userAction
                )
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.order_intent))
                LabelRadioButton(
                    label = stringResource(id = R.string.order_intent_capture),
                    onClick = { payPalViewModel.orderIntentSelected(getString(R.string.order_intent_capture)) },
                    selectedLiveData = payPalViewModel.orderIntent
                )
                LabelRadioButton(
                    label = stringResource(id = R.string.order_intent_authorize),
                    onClick = { payPalViewModel.orderIntentSelected(getString(R.string.order_intent_authorize)) },
                    selectedLiveData = payPalViewModel.orderIntent
                )
            }
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

    @Composable
    private fun LabelRadioButton(
        label: String,
        onClick: () -> Unit,
        selectedLiveData: LiveData<String>
    ) =
        Row(modifier = Modifier.padding(horizontal = 12.dp)) {
            RadioButton(
                selected = selectedLiveData.observeAsState().value == label,
                onClick = onClick
            )
            Text(text = label)
        }

    private fun launchNativeCheckout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            payPalViewModel.startPayPalCheckout()
        }
    }
}
