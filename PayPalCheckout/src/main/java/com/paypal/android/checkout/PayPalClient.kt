package com.paypal.android.checkout

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.paypal.android.core.CoreConfig


// TODO: doc string and unit test
class PayPalClient(val activity: FragmentActivity, val coreConfig: CoreConfig) {

    private val browserSwitchClient: BrowserSwitchClient
    private val browserSwitchHelper: BrowserSwitchHelper
    //TODO: Implement a listener instead
    private var callback: PayPalCheckoutResultCallback? = null

    init {
        browserSwitchClient = BrowserSwitchClient()
        browserSwitchHelper = BrowserSwitchHelper()
    }

    fun approveOrder(payPalRequest: PayPalRequest, callback: PayPalCheckoutResultCallback) {
        this.callback = callback
        val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(payPalRequest.orderID, coreConfig)
        browserSwitchClient.start(activity, browserSwitchOptions)
    }

    fun handleBrowserSwitchResult(activity: FragmentActivity) {
        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        browserSwitchResult?.let { result ->
            if (result.deepLinkUrl != null && result.requestMetadata != null) {
                val webResult = PayPalCheckoutWebResult(result.deepLinkUrl!!,
                    result.requestMetadata!!
                )
                val payPalCheckoutResult = PayPalCheckoutResult.Success(webResult.orderId, webResult.payerId)
                callback?.onPayPalCheckoutResult(payPalCheckoutResult)
            }
        }
    }
}
