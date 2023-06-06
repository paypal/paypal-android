package com.paypal.android.ui.paypal

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.paypal.android.R
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.databinding.FragmentPaymentButtonBinding
import com.paypal.android.utils.OrderUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class PayPalFragment : Fragment(), PayPalWebCheckoutListener {

    companion object {
        private val TAG = PayPalFragment::class.qualifiedName
    }

    private lateinit var binding: FragmentPaymentButtonBinding

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private lateinit var paypalClient: PayPalWebCheckoutClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentButtonBinding.inflate(inflater, container, false)

        binding.submitButton.setOnClickListener { launchWebCheckout() }
        binding.payPalButton.setOnClickListener { launchWebCheckout(PayPalWebCheckoutFundingSource.PAY_LATER) }
        binding.payPalCreditButton.setOnClickListener {
            launchWebCheckout(PayPalWebCheckoutFundingSource.PAYPAL_CREDIT)
        }
        binding.payPalPayLater.setOnClickListener { launchWebCheckout(PayPalWebCheckoutFundingSource.PAY_LATER) }
        binding.customizeButton.setOnClickListener {
            findNavController().navigate(PayPalFragmentDirections.actionPayPalFragmentToPayPalButtonsFragment())
        }

        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")

        when (binding.radioGroupIntent.checkedRadioButtonId) {
            R.id.intent_authorize -> captureOrder(result)
            R.id.intent_capture -> authorizeOrder(result)
        }

        val title = getString(R.string.order_approved)

        val payerId = getString(R.string.payer_id, result.payerId)
        val orderId = getString(R.string.order_id, result.orderId)
        val statusText = "$payerId\n$orderId"

        updateStatusText("$title\n$statusText")
    }

    private fun captureOrder(payPalResult: PayPalWebCheckoutResult) =
        viewLifecycleOwner.lifecycleScope.launch {
            payPalResult.orderId?.let { orderId ->
                updateStatusText("Capturing order with ID: $orderId")
                val result = sdkSampleServerAPI.captureOrder(orderId)
                updateStatusTextWithPayPalResult(payPalResult, result.status)
                hideLoader()
            }
        }


    private fun authorizeOrder(payPalResult: PayPalWebCheckoutResult) =
        viewLifecycleOwner.lifecycleScope.launch {
            payPalResult.orderId?.let { orderId ->
                updateStatusText("Authorizing order with ID: $orderId")
                val result = sdkSampleServerAPI.authorizeOrder(orderId)
                updateStatusTextWithPayPalResult(payPalResult, result.status)
                hideLoader()
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebFailure(error: PayPalSDKError) {
        Log.i(TAG, "Checkout Error: ${error.errorDescription}")

        val title = getString(R.string.order_failed)
        val statusText = getString(R.string.reason, error.errorDescription)

        binding.statusText.text = "$title\n$statusText"
        hideLoader()
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebCanceled() {
        Log.i(TAG, "User cancelled")

        val title = getString(R.string.checkout_cancelled)
        val statusText = getString(R.string.user_cancelled)

        binding.statusText.text = "$title\n$statusText"
        hideLoader()
    }

    private fun launchWebCheckout(funding: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL) {
        showLoader()

        lifecycleScope.launch {
            try {
                binding.statusText.setText(R.string.getting_token)
                val clientId = sdkSampleServerAPI.fetchClientId()
                val coreConfig = CoreConfig(clientId)
                paypalClient =
                    PayPalWebCheckoutClient(
                        requireActivity(),
                        coreConfig,
                        "com.paypal.android.demo"
                    )
                paypalClient.listener = this@PayPalFragment
                binding.statusText.setText(R.string.creating_order)

                val orderRequest = OrderUtils.createOrderBuilder("5.0")
                val order = sdkSampleServerAPI.createOrder(orderRequest)
                order.id?.let { orderId ->
                    paypalClient.start(PayPalWebCheckoutRequest(orderId, funding))
                }
            } catch (e: UnknownHostException) {
                Log.e(TAG, e.message!!)
                val error = APIClientError.payPalCheckoutError(e.message!!)
                onPayPalWebFailure(error)
            } catch (e: HttpException) {
                Log.e(TAG, e.message!!)
                val error = APIClientError.payPalCheckoutError(e.message!!)
                onPayPalWebFailure(error)
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

    private fun updateStatusTextWithPayPalResult(
        result: PayPalWebCheckoutResult,
        orderStatus: String?
    ) {
        val statusText = "Confirmed Order: ${result.orderId} Status: $orderStatus"
        updateStatusText(statusText)
    }

    private fun updateStatusText(text: String) {
        requireActivity().runOnUiThread {
            if (!isDetached) {
                binding.statusText.text = text
            }
        }
    }
}
