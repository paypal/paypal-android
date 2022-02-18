package com.paypal.android.checkout

import android.app.Application
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
class PayPalClient(application: Application, coreConfig: CoreConfig, returnUrl: String) {

    /**
     * Sets a listener to receive notifications when a PayPal event occurs.
     */
    var listener: PayPalListener? = null

    init {
        val config = CheckoutConfig(
            application = application,
            clientId = coreConfig.clientId,
            environment = getPayPalEnvironment(coreConfig.environment),
            returnUrl = returnUrl,
        )
        PayPalCheckout.setConfig(config)
    }

    /**
     * Initiate a PayPal checkout for an order.
     *
     * @param orderId the id of the order
     */
    fun checkout(orderId: String) {
        PayPalCheckout.start(CreateOrder { createOrderActions ->
            createOrderActions.set(orderId)
        },
            onApprove = OnApprove { approval ->
                val result = approval.run { PayPalCheckoutResult(data.orderId, data.payerId) }
                listener?.onPayPalSuccess(result)
            },
            onCancel = OnCancel {
                listener?.onPayPalCanceled()
            },
            onError = OnError { errorInfo ->
                val error = APIClientError.payPalCheckoutError(errorInfo.reason)
                listener?.onPayPalFailure(error)
            })
    }
}
