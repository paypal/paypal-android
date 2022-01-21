package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.core.CoreConfig
import com.paypal.checkout.config.CheckoutConfig

class PayPalClient(application: Application, coreConfig: CoreConfig, returnUrl: String) {

    init {
        val config = CheckoutConfig(
            application = application,
            clientId = coreConfig.clientId,
            environment = getPayPalEnvironment(coreConfig.environment),
            returnUrl = returnUrl,
        )
//        PayPalCheckout.setConfig(config)
    }

    fun checkout(orderId: String, callback: PayPalCheckoutResultCallback) {
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
}
