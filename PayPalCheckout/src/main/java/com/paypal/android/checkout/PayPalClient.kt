package com.paypal.android.checkout

import com.paypal.android.checkout.pojo.Approval
import com.paypal.android.checkout.pojo.ErrorInfo
import com.paypal.android.checkout.pojo.ShippingChangeData
import com.paypal.android.core.Environment
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.Environment.LIVE
import com.paypal.checkout.config.Environment.SANDBOX
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError
import com.paypal.checkout.shipping.OnShippingChange

class PayPalClient(payPalConfig: PayPalConfiguration) {

    init {
        val config = CheckoutConfig(
            application = payPalConfig.application,
            clientId = payPalConfig.paymentsConfiguration.clientId,
            environment =  getPayPalEnvironment(payPalConfig.paymentsConfiguration.environment),
            returnUrl = payPalConfig.returnUrl,
            currencyCode = payPalConfig.currencyCode?.asNativeCheckout,
            paymentButtonIntent = payPalConfig.paymentButtonIntent?.asNativeCheckout,
            settingsConfig = payPalConfig.settingsConfig.asNativeCheckout,
            userAction = payPalConfig.userAction?.asNativeCheckout
        )
        PayPalCheckout.setConfig(config)
    }

    private fun getPayPalEnvironment(environment:Environment) = when(environment) {
        Environment.LIVE -> LIVE
        Environment.SANDBOX -> SANDBOX
    }

    // usea a stream of data as an alternative to callbacks
    fun checkout(orderId: String, payPalClientListener: PayPalClientListener) {
        PayPalCheckout.start(CreateOrder { createOrderActions ->
            createOrderActions.set(orderId)
        },
        OnApprove { approval ->
            payPalClientListener.onPayPalApprove(Approval(approval))
        },
        OnShippingChange { shippingChangeData, shippingChangeActions ->
            payPalClientListener.onPayPalShippingAddressChange(ShippingChangeData(shippingChangeData), shippingChangeActions)
        },
        OnCancel {
            payPalClientListener.onPayPalCancel()
        },
        OnError { errorInfo ->
            payPalClientListener.onPayPalError(ErrorInfo(errorInfo))
        })
    }
}