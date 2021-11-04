package com.paypal.android.ui.paypal

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.checkout.PayPalCheckoutResult
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

        val coreConfig = CoreConfig(BuildConfig.CLIENT_ID, environment = Environment.SANDBOX)
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
        val isLoading by payPalViewModel.isLoading.observeAsState(initial = false)
        ConstraintLayout(
            modifier = Modifier.fillMaxHeight(),
        ) {
            val (button, text, result) = createRefs()
            if (isLoading) {
                LoadingComposable(modifier = Modifier.constrainAs(result) {
                    top.linkTo(parent.top)
                    bottom.linkTo(button.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                })
            } else {
                CheckoutResult(
                    payPalViewModel.checkoutResult,
                    modifier = Modifier.constrainAs(result) {
                        top.linkTo(parent.top)
                        bottom.linkTo(button.top)
                    })
            }
            Button(
                enabled = canRunPayPalCheckout && !isLoading,
                onClick = { launchNativeCheckout() },
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)
                    .fillMaxWidth()
                    .constrainAs(button) {
                        if (canRunPayPalCheckout) {
                            bottom.linkTo(parent.bottom)
                        } else {
                            bottom.linkTo(text.top)
                        }
                    }
            ) {
                Text(stringResource(R.string.start_checkout))
            }
            if (!canRunPayPalCheckout) Text(
                text = stringResource(id = R.string.minimum_sdk_needed),
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(text) {
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                    },
                textAlign = TextAlign.Center,
            )
        }
    }

    @Composable
    private fun CheckoutResult(resultLiveData: LiveData<PayPalCheckoutResult>, modifier: Modifier) {
        val result by resultLiveData.observeAsState(initial = null)
        when (result) {
            is PayPalCheckoutResult.Success -> CheckoutSuccess(
                result as PayPalCheckoutResult.Success,
                modifier = modifier
            )
            is PayPalCheckoutResult.Failure -> CheckoutFailure(
                result as PayPalCheckoutResult.Failure,
                modifier = modifier
            )
            PayPalCheckoutResult.Cancellation -> CheckoutCancelled(modifier = modifier)
        }

    }

    @Composable
    private fun CheckoutSuccess(result: PayPalCheckoutResult.Success, modifier: Modifier) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Order Approved",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Payer Id: ${result.payerId}",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Order Id: ${result.orderId}",
                fontSize = 16.sp,
                color = Color.Gray,
            )
        }
    }

    @Composable
    private fun CheckoutFailure(result: PayPalCheckoutResult.Failure, modifier: Modifier) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Order Failed",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "Reason: ${result.error.reason}",
                fontSize = 16.sp,
                color = Color.Gray,
            )
        }
    }

    @Composable
    private fun CheckoutCancelled(modifier: Modifier) {
        Column(modifier = modifier.padding(horizontal = 8.dp)) {
            Text(
                text = "Checkout Cancelled",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "User cancelled the checkout ",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
            )
        }
    }

    @Composable
    private fun LoadingComposable(modifier: Modifier) {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Creating order...")
        }
    }

    private fun launchNativeCheckout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            payPalViewModel.startPayPalCheckout()
        }
    }
}
