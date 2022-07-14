package com.paypal.android.ui.paypal

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.R
import com.paypal.android.api.services.PayPalDemoApi
import com.paypal.android.checkoutweb.PayPalWebCheckoutClient
import com.paypal.android.checkoutweb.PayPalWebCheckoutFundingSource
import com.paypal.android.checkoutweb.PayPalWebCheckoutListener
import com.paypal.android.checkoutweb.PayPalWebCheckoutRequest
import com.paypal.android.checkoutweb.PayPalWebCheckoutResult
import com.paypal.android.core.APIClientError
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.databinding.FragmentPaymentButtonBinding
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
    lateinit var payPalDemoApi: PayPalDemoApi

    private lateinit var paypalClient: PayPalWebCheckoutClient

    private val viewModel: PayPalViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentButtonBinding.inflate(inflater, container, false)

        val coreConfig = CoreConfig()
        paypalClient =
            PayPalWebCheckoutClient(requireActivity(), coreConfig, "com.paypal.android.demo")
        paypalClient.listener = this

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.getEligibility().observe(viewLifecycleOwner) {
            Log.d(TAG, "isVenmoEligible: ${it.isVenmoEligible}")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")

        val title = getString(R.string.order_approved)

        val payerId = getString(R.string.payer_id, result.payerId)
        val orderId = getString(R.string.order_id, result.orderId)
        val statusText = "$payerId\n$orderId"

        binding.statusText.text = "$title\n$statusText"
        hideLoader()
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
                binding.statusText.setText(R.string.creating_order)

                val orderJson = JsonParser.parseString(OrderUtils.orderWithShipping) as JsonObject
                val order = payPalDemoApi.createOrder(orderJson)
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
}
