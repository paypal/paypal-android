package com.paypal.android.checkout

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.checkout.paymentbutton.error.PayPalSDKError
import com.paypal.android.core.CoreConfig


// TODO: doc string
class PayPalClient internal constructor(
    private val activity: FragmentActivity,
    private val coreConfig: CoreConfig,
    private val browserSwitchClient: BrowserSwitchClient,
    private val browserSwitchHelper: BrowserSwitchHelper
) {

    constructor(
        activity: FragmentActivity,
        coreConfig: CoreConfig,
        urlScheme: String
    ) : this(activity, coreConfig, BrowserSwitchClient(), BrowserSwitchHelper(urlScheme))

    private var browserSwitchResult: BrowserSwitchResult? = null
    var listener: PayPalCheckoutListener? = null
        set(value) {
            field = value
            browserSwitchResult?.also {
                handleBrowserSwitchResult()
            }
        }

    init {
        activity.lifecycle.addObserver(PayPalLifeCycleObserver(this))
    }

    fun approveOrder(payPalRequest: PayPalRequest) {
        val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(
            payPalRequest.orderID,
            coreConfig
        )
        browserSwitchClient.start(activity, browserSwitchOptions)
    }

    internal fun handleBrowserSwitchResult() {
        browserSwitchResult = browserSwitchClient.deliverResult(activity)
        listener?.also {
            browserSwitchResult?.also { result ->
                when (result.status) {
                    BrowserSwitchStatus.SUCCESS -> deliverSuccess()
                    BrowserSwitchStatus.CANCELED -> deliverCancellation()
                }
            }
        }
    }

    private fun deliverSuccess() {
        val payPalCheckoutResult =
            if (browserSwitchResult?.deepLinkUrl != null && browserSwitchResult?.requestMetadata != null) {
                val webResult = PayPalCheckoutWebResult(
                    browserSwitchResult?.deepLinkUrl!!,
                    browserSwitchResult?.requestMetadata!!
                )
                if (!webResult.orderId.isNullOrBlank() && !webResult.payerId.isNullOrBlank()) {
                    PayPalCheckoutResult.Success(webResult.orderId, webResult.payerId)
                } else {
                    PayPalCheckoutResult.Failure(PayPalSDKError("PayerId or OrderId are null - PayerId: ${webResult.payerId}, orderId: ${webResult.orderId}"))
                }
            } else {
                PayPalCheckoutResult.Failure(PayPalSDKError("Something went wrong"))
            }
        browserSwitchResult = null
        listener?.onPayPalCheckoutResult(payPalCheckoutResult)
    }

    private fun deliverCancellation() {
        browserSwitchResult = null
        val payPalCheckoutResult = PayPalCheckoutResult.Cancellation
        listener?.onPayPalCheckoutResult(payPalCheckoutResult)
    }
}
