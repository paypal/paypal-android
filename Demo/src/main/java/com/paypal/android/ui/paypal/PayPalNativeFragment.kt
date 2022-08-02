package com.paypal.android.ui.paypal

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.BuildConfig
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkout.PayPalCheckoutResult
import com.paypal.android.checkout.PayPalClient
import com.paypal.android.checkout.PayPalListener
import com.paypal.android.core.APIClientError
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.databinding.FragmentPayPalNativeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class PayPalNativeFragment : Fragment(), PayPalListener {

    companion object {
        private val TAG = PayPalNativeFragment::class.qualifiedName
    }

    private lateinit var binding: FragmentPayPalNativeBinding

    @Inject
    lateinit var payPalDemoApi: PayPalDemoApi

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentPayPalNativeBinding.inflate(inflater, container, false)

        binding.startNativeCheckout.setOnClickListener { launchNativeCheckout() }
        return binding.root
    }

    private fun launchNativeCheckout() {
        lifecycleScope.launch {
            try {
                val accessToken = payPalDemoApi.fetchAccessToken().value
                val orderJson =
                    JsonParser.parseString(OrderUtils.orderWithShipping) as JsonObject
                val order = payPalDemoApi.createOrder(orderJson)

                val coreConfig = CoreConfig(accessToken = accessToken)
                val paypalNativeClient = PayPalClient(
                    requireActivity().application,
                    coreConfig,
                    "${BuildConfig.APPLICATION_ID}://paypalpay"
                )
                paypalNativeClient.listener = this@PayPalNativeFragment
                order.id?.let { orderId ->
                    paypalNativeClient.startCheckout(orderId)
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

    override fun onPayPalSuccess(result: PayPalCheckoutResult) {
        Toast.makeText(requireContext(), "PayPal SUCCESS", Toast.LENGTH_LONG).show()
    }

    override fun onPayPalFailure(error: PayPalSDKError) {
        Toast.makeText(requireContext(), "PayPal FAILURE", Toast.LENGTH_LONG).show()
    }

    override fun onPayPalCanceled() {
        Toast.makeText(requireContext(), "PayPal CANCELED", Toast.LENGTH_LONG).show()
    }
}
