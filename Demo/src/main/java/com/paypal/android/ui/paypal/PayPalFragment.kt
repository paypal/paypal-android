package com.paypal.android.ui.paypal

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.paypal.android.DemoViewModel
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
import com.paypal.android.paypalwebpayments.PayPalWebAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebAuthLauncher
import com.paypal.android.utils.OrderUtils
import com.paypal.checkout.createorder.OrderIntent
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

    private val payPalWebAuthLauncher = PayPalWebAuthLauncher()
    private val activityViewModel by activityViewModels<DemoViewModel>()

    private lateinit var binding: FragmentPaymentButtonBinding
    private val orderIntent: OrderIntent
        get() = when (binding.radioGroupIntent.checkedRadioButtonId) {
            R.id.intent_authorize -> OrderIntent.AUTHORIZE
            else -> OrderIntent.CAPTURE
        }

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

    override fun onResume() {
        super.onResume()

        val payPalAuthResult = checkForPayPalAuthResult()
        if (payPalAuthResult != null) {
            payPalWebAuthLauncher.clearPendingRequests(requireContext())
            paypalClient.continueStart(payPalAuthResult)
        }
    }

    private fun checkForPayPalAuthResult(): PayPalWebAuthChallengeResult? {
        val context = requireContext()
        return payPalWebAuthLauncher.parseResult(context, activity?.intent)
            ?: payPalWebAuthLauncher.parseResult(context, activityViewModel.newIntent.value)
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")

        when (orderIntent) {
            OrderIntent.CAPTURE -> captureOrder(result)
            OrderIntent.AUTHORIZE -> authorizeOrder(result)
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
                val statusText =
                    "Confirmed Order: $orderId Status: ${result.status} Intent: CAPTURE"
                updateStatusText(statusText)
                hideLoader()
            }
        }

    private fun authorizeOrder(payPalResult: PayPalWebCheckoutResult) =
        viewLifecycleOwner.lifecycleScope.launch {
            payPalResult.orderId?.let { orderId ->
                updateStatusText("Authorizing order with ID: $orderId")
                val result = sdkSampleServerAPI.authorizeOrder(orderId)
                val statusText =
                    "Confirmed Order: $orderId Status: ${result.status} Intent: AUTHORIZE"
                updateStatusText(statusText)
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
                binding.statusText.setText(R.string.getting_client_id)
                val coreConfig = CoreConfig(sdkSampleServerAPI.clientId)
                paypalClient =
                    PayPalWebCheckoutClient(
                        requireActivity(),
                        coreConfig,
                        "com.paypal.android.demo"
                    )
                paypalClient.listener = this@PayPalFragment
                binding.statusText.setText(R.string.creating_order)

                val orderRequest =
                    OrderUtils.createOrderBuilder("5.0", orderIntent = orderIntent)
                val order = sdkSampleServerAPI.createOrder(orderRequest)
                order.id?.let { orderId ->
                    val authChallenge = paypalClient.start(PayPalWebCheckoutRequest(orderId, funding))
                    payPalWebAuthLauncher.launch(requireActivity(), authChallenge)
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

    private fun updateStatusText(text: String) {
        requireActivity().runOnUiThread {
            if (!isDetached) {
                binding.statusText.text = text
            }
        }
    }
}
