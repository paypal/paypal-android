package com.paypal.android.ui.paypal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.checkout.PayPalListener
import com.paypal.android.core.APIClientError
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.Environment
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.databinding.FragmentPaymentButtonBinding
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

    private lateinit var binding: FragmentPaymentButtonBinding

    @Inject
    lateinit var payPalDemoApi: PayPalDemoApi

    private val payPalViewModel: PayPalViewModel by viewModels()
    private lateinit var paypalClient: PayPalClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentButtonBinding.inflate(inflater, container, false)

        val coreConfig = CoreConfig(BuildConfig.CLIENT_ID, environment = Environment.SANDBOX)
        val application = requireActivity().application
        val returnUrl = BuildConfig.APPLICATION_ID + "://paypalpay"
        paypalClient = PayPalClient(application, coreConfig, returnUrl)
        paypalClient.listener = this

        binding.submitButton.setOnClickListener { launchNativeCheckout() }

        return binding.root
    }

    override fun onPayPalSuccess(result: PayPalCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")

        val title = getString(R.string.order_approved)

        val payerId = getString(R.string.payer_id, result.payerId)
        val orderId = getString(R.string.order_id, result.orderId)
        val statusText = "$payerId\n$orderId"

        payPalViewModel.statusTitle.value = title
        payPalViewModel.statusText.value = statusText
        hideLoader()
    }

    override fun onPayPalFailure(error: PayPalSDKError) {
        Log.i(TAG, "Checkout Error: ${error.errorDescription}")

        val title = getString(R.string.order_failed)
        val statusText = getString(R.string.reason, error.errorDescription)

        payPalViewModel.statusTitle.value = title
        payPalViewModel.statusText.value = statusText
        hideLoader()
    }

    override fun onPayPalCanceled() {
        Log.i(TAG, "User cancelled")

        val title = getString(R.string.checkout_cancelled)
        val statusText = getString(R.string.user_cancelled)

        payPalViewModel.statusTitle.value = title
        payPalViewModel.statusText.value = statusText
        hideLoader()
    }

    private fun launchNativeCheckout() {
        showLoader()

        lifecycleScope.launch {
            try {
                val orderJson = JsonParser.parseString(OrderUtils.orderWithShipping) as JsonObject
                val order = payPalDemoApi.fetchOrderId(countryCode = "US", orderJson)
                order.id?.let { orderId ->
                    paypalClient.checkout(orderId)
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, e.message!!)
                val error = APIClientError.payPalCheckoutError(e.message!!)
                onPayPalFailure(error)
            } catch (e: HttpException) {
                Log.e(TAG, e.message!!)
                val error = APIClientError.payPalCheckoutError(e.message!!)
                onPayPalFailure(error)
            }
        }
    }

    private fun showLoader() {
        payPalViewModel.isLoading.value = true
    }

    private fun hideLoader() {
        payPalViewModel.isLoading.value = false
    }
}
