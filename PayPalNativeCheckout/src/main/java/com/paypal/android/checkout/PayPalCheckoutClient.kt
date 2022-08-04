package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.core.API
import com.paypal.android.core.APIClientError
import com.paypal.android.core.CoreConfig
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError

/**
 * Use this client to checkout with PayPal.
 */
class PayPalCheckoutClient internal constructor (
    private val application: Application,
    private val coreConfig: CoreConfig,
    private val returnUrl: String,
    private val api: API
) {

    constructor(application: Application, coreConfig: CoreConfig, returnUrl: String) : this(application, coreConfig, returnUrl, API(coreConfig))


    /**
     * Sets a listener to receive notifications when a PayPal event occurs.
     */
    var listener: PayPalCheckoutListener? = null
        set(value) {
            if (value != null) {
                registerCallbacks(value)
            }
        }
    /**
     * Initiate a PayPal checkout for an order.
     *
     * @param createOrder the id of the order
     */
    suspend fun startCheckout(createOrder: CreateOrder) {
        val config = CheckoutConfig(
            application = application,
            clientId = api.getClientId(),
            environment = getPayPalEnvironment(coreConfig.environment),
            returnUrl = returnUrl,
        )
        PayPalCheckout.setConfig(config)
        listener?.onPayPalCheckoutStart()
        PayPalCheckout.startCheckout(createOrder)
    }

    private fun registerCallbacks(listener: PayPalCheckoutListener) {
        PayPalCheckout.registerCallbacks(
            onApprove = OnApprove { approval ->
                val result = approval.run {
                    PayPalCheckoutResult(this)
                }
                listener.onPayPalCheckoutSuccess(result)
            },
            onCancel = OnCancel {
                listener.onPayPalCheckoutCanceled()
            },
            onError = OnError { errorInfo ->
                val error = APIClientError.payPalCheckoutError(errorInfo.reason)
                listener.onPayPalCheckoutFailure(error)
            })
    }
}
