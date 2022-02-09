package com.paypal.android.ui.paypal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.api.model.Order
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.checkout.PayPalListener
import com.paypal.android.checkout.pojo.CorrelationIds
import com.paypal.android.checkout.pojo.ErrorInfo
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.ui.theme.DemoTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class PayPalFragment : Fragment(), PayPalListener {

    companion object {
        private val TAG = PayPalFragment::class.qualifiedName
    }

    @Inject
    private lateinit var payPalDemoApi: PayPalDemoApi

    private val payPalViewModel: PayPalViewModel by viewModels()
    private lateinit var paypalClient: PayPalClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_payment_button, container, false)

        val coreConfig = CoreConfig(BuildConfig.CLIENT_ID, environment = Environment.SANDBOX)
        val application = requireActivity().application
        val returnUrl = BuildConfig.APPLICATION_ID + "://paypalpay"
        paypalClient = PayPalClient(application, coreConfig, returnUrl)
        paypalClient.listener = this

        view.findViewById<ComposeView>(R.id.compose_view).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PayPalFragmentView()
            }
        }

        return view
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
                enabled = !isLoading,
                onClick = { launchNativeCheckout() },
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp)
                    .fillMaxWidth()
                    .constrainAs(button) {
                        bottom.linkTo(text.top)
                    }
            ) {
                Text(stringResource(R.string.start_checkout))
            }
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
                text = getString(R.string.order_approved),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = getString(R.string.payer_id, result.payerId),
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = getString(R.string.order_id, result.orderId),
                fontSize = 16.sp,
                color = Color.Gray,
            )
        }
    }

    @Composable
    private fun CheckoutFailure(result: PayPalCheckoutResult.Failure, modifier: Modifier) {
        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            Text(
                text = getString(R.string.order_failed),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = getString(R.string.reason, result.error.reason),
                fontSize = 16.sp,
                color = Color.Gray,
            )
        }
    }

    @Composable
    private fun CheckoutCancelled(modifier: Modifier) {
        Column(modifier = modifier.padding(horizontal = 8.dp)) {
            Text(
                text = getString(R.string.checkout_cancelled),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = getString(R.string.user_cancelled),
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
            Text(text = getString(R.string.creating_order))
        }
    }

    private suspend fun fetchOrder(): Order {
        val orderJson = JsonParser.parseString(OrderUtils.orderWithShipping) as JsonObject
        return payPalDemoApi.fetchOrderId(countryCode = "US", orderJson)
    }

    private fun launchNativeCheckout() {
        showLoader()

        lifecycleScope.launch {
            try {
                val order = fetchOrder()
                order.id?.let { orderId ->
                    paypalClient.checkout(orderId)
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, e.message!!)
                val error = PayPalCheckoutResult.Failure(
                    error = ErrorInfo(
                        e,
                        e.message!!,
                        CorrelationIds(),
                        null
                    )
                )
                onPayPalFailure(error)
            } catch (e: HttpException) {
                Log.e(TAG, e.message!!)
                val error = PayPalCheckoutResult.Failure(
                    error = ErrorInfo(
                        e,
                        e.message!!,
                        CorrelationIds(),
                        null
                    )
                )
                onPayPalFailure(error)
            }
        }
    }

    override fun onPayPalSuccess(result: PayPalCheckoutResult.Success) {
        TODO("Update result text")
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")
        hideLoader()
    }

    override fun onPayPalFailure(failure: PayPalCheckoutResult.Failure) {
        TODO("Update result text")
        Log.i(TAG, "Checkout Error: ${failure.error.reason}")
        hideLoader()
    }

    override fun onPayPalCanceled() {
        TODO("Update result text")
        Log.i(TAG, "User cancelled")
        hideLoader()
    }

    private fun showLoader() {
        payPalViewModel.isLoading.value = true
    }

    private fun hideLoader() {
        payPalViewModel.isLoading.value = false
    }
}
