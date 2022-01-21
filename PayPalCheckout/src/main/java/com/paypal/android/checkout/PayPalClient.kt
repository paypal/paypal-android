package com.paypal.android.checkout

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.paypal.android.core.CoreConfig


class PayPalClient(val activity: FragmentActivity, val coreConfig: CoreConfig, returnUrl: String) {

    private val browserSwitchClient: BrowserSwitchClient
    private val browserSwitchHelper: BrowserSwitchHelper
    // TODO: confirm if this approach works well for process/activity destroy scenarios
    private var callback: PayPalCheckoutResultCallback? = null

    init {
//        val config = CheckoutConfig(
//            application = context,
//            clientId = coreConfig.clientId,
//            environment = getPayPalEnvironment(coreConfig.environment),
//            returnUrl = returnUrl,
//        )
        browserSwitchClient = BrowserSwitchClient()
        browserSwitchHelper = BrowserSwitchHelper()
//        PayPalCheckout.setConfig(config)
    }

    fun approveOrder(payPalRequest: PayPalRequest, callback: PayPalCheckoutResultCallback) {
        this.callback = callback
        val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(payPalRequest.orderID, coreConfig)
        browserSwitchClient.start(activity, browserSwitchOptions)


        // TODO: Native Checkout will be re-integrated in the future
//        PayPalCheckout.start(CreateOrder { createOrderActions ->
//            createOrderActions.set(orderId)
//        },
//            onApprove = OnApprove { approval ->
//                val result =
//                    PayPalCheckoutResult.Success(approval.data.orderId, approval.data.payerId)
//                callback.onPayPalCheckoutResult(result)
//            },
//            onCancel = OnCancel {
//                callback.onPayPalCheckoutResult(PayPalCheckoutResult.Cancellation)
//            },
//            onError = OnError { errorInfo ->
//                callback.onPayPalCheckoutResult(PayPalCheckoutResult.Failure(ErrorInfo(errorInfo)))
//            })
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
