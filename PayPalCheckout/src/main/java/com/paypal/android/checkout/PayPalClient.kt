package com.paypal.android.checkout

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.checkout.paymentbutton.error.PayPalSDKError
import com.paypal.android.core.CoreConfig


// TODO: doc string and unit test
class PayPalClient(private val activity: FragmentActivity, private val coreConfig: CoreConfig, urlScheme: String) {

    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient()
    private val browserSwitchHelper: BrowserSwitchHelper = BrowserSwitchHelper(urlScheme)

    fun approveOrder(payPalRequest: PayPalRequest) {
        val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(payPalRequest.orderID, coreConfig)
        browserSwitchClient.start(activity, browserSwitchOptions)
    }

    fun handleBrowserSwitchResult(activity: FragmentActivity, callback: PayPalCheckoutResultCallback) {
        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        browserSwitchResult?.let { result ->
            when(result.status) {
                BrowserSwitchStatus.SUCCESS -> handleSuccess(browserSwitchResult, callback)
                BrowserSwitchStatus.CANCELED -> handleCancellation(callback)
            }
        }
    }

    private fun handleSuccess(result: BrowserSwitchResult, callback: PayPalCheckoutResultCallback) {
        val payPalCheckoutResult = if (result.deepLinkUrl != null && result.requestMetadata != null) {
            val webResult = PayPalCheckoutWebResult(result.deepLinkUrl!!,
                result.requestMetadata!!
            )
            if (!webResult.orderId.isNullOrBlank() && !webResult.payerId.isNullOrBlank()) {
                PayPalCheckoutResult.Success(webResult.orderId, webResult.payerId)
            } else {
                PayPalCheckoutResult.Failure(PayPalSDKError("PayerId or OrderId are null - PayerId: ${webResult.orderId}, orderId: ${webResult.payerId}"))
            }
        } else {
            PayPalCheckoutResult.Failure(PayPalSDKError("Something went wrong"))
        }
        callback.onPayPalCheckoutResult(payPalCheckoutResult)
    }

    private fun handleCancellation(callback: PayPalCheckoutResultCallback) {
        val payPalCheckoutResult = PayPalCheckoutResult.Cancellation
        callback.onPayPalCheckoutResult(payPalCheckoutResult)
    }
}
