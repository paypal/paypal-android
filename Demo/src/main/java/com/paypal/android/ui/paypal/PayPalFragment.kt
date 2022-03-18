package com.paypal.android.ui.paypal

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.BuildConfig
import com.paypal.android.R
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.checkout.PayPalCheckoutListener
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
class PayPalFragment : Fragment(), PayPalCheckoutListener {

    companion object {
        private val TAG = PayPalFragment::class.qualifiedName
    }

    private lateinit var binding: FragmentPaymentButtonBinding

    @Inject
    lateinit var payPalDemoApi: PayPalDemoApi

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
        paypalClient = PayPalClient(requireActivity(), coreConfig, returnUrl)
        paypalClient.listener = this

        binding.submitButton.setOnClickListener { launchNativeCheckout() }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalSuccess(result: PayPalCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")

        val title = getString(R.string.order_approved)

        val payerId = getString(R.string.payer_id, result.payerId)
        val orderId = getString(R.string.order_id, result.orderId)
        val statusText = "$payerId\n$orderId"

        binding.statusText.text = "$title\n$statusText"
        hideLoader()
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalFailure(error: PayPalSDKError) {
        Log.i(TAG, "Checkout Error: ${error.errorDescription}")

        val title = getString(R.string.order_failed)
        val statusText = getString(R.string.reason, error.errorDescription)

        binding.statusText.text = "$title\n$statusText"
        hideLoader()
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalCanceled() {
        Log.i(TAG, "User cancelled")

        val title = getString(R.string.checkout_cancelled)
        val statusText = getString(R.string.user_cancelled)

        binding.statusText.text = "$title\n$statusText"
        hideLoader()
    }

    private fun launchNativeCheckout() {
        showLoader()

        lifecycleScope.launch {
            try {
                binding.statusText.setText(R.string.creating_order)
                val orderJson = JsonParser().parse(OrderUtils.orderWithShipping) as JsonObject
                val order = payPalDemoApi.fetchOrderId(countryCode = "US", orderJson)
                order.id?.let { orderId ->
                    //paypalClient.checkout(orderId)
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
        binding.progressIndicator.visibility = View.VISIBLE
        binding.progressIndicator.animate()
    }

    private fun hideLoader() {
        binding.progressIndicator.visibility = View.INVISIBLE
    }
}
