package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.core.Environment
import com.paypal.android.core.PaymentsConfiguration
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.Environment.LIVE
import com.paypal.checkout.config.Environment.SANDBOX
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError
import com.paypal.checkout.shipping.OnShippingChange

//If we make it an instance, we might be giving the wrong signal to the merchant, beliving that he
//might instanciate more than one paypal client, but under the hood, is just only one.
class PayPalClient(application: Application, paymentConfig: PaymentsConfiguration) {

    var payPalClientListener: PayPalClientListener? = null //if its a necessary condition

    init {
        val config = CheckoutConfig(
            application = application,
            clientId = paymentConfig.clientId,
            environment =  getPayPalEnvironment(paymentConfig.environment),
            returnUrl = paymentConfig.returnUrl //this might not be necessary
        )
        PayPalCheckout.setConfig(config)
    }

    private fun getPayPalEnvironment(environment:Environment) = when(environment) {
        Environment.LIVE -> LIVE
        Environment.SANDBOX -> SANDBOX
    }

    fun checkout(orderId: String) {
        PayPalCheckout.start(CreateOrder { createOrderActions ->
            createOrderActions.set(orderId)
        },
        OnApprove { approval ->
            payPalClientListener?.onPayPalApprove(approval)
        },
        OnShippingChange { shippingChangeData, shippingChangeActions ->
            payPalClientListener?.onPayPalShippingAddressChange(shippingChangeData, shippingChangeActions)
        },
        OnCancel {
            payPalClientListener?.onPayPalCancel()
        },
        OnError { errorInfo ->
            payPalClientListener?.onPayPalError(errorInfo)
        })
    }
}