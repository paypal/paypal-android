package com.paypal.android.checkout

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.paypal.android.checkout.pojo.ErrorInfo
import com.paypal.android.core.CoreConfig
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError

@RequiresApi(Build.VERSION_CODES.M)
class PayPalClient(application: Application, coreConfig: CoreConfig, returnUrl: String) {

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

    fun checkout(orderId: String) {
        PayPalCheckout.start(CreateOrder { createOrderActions ->
            createOrderActions.set(orderId)
        },
            onApprove = OnApprove { approval ->
                val result = approval.run {
                    PayPalCheckoutResult.Success(data.orderId, data.payerId)
                }
                listener?.onPayPalSuccess(result)
            },
            onCancel = OnCancel {
                listener?.onPayPalCanceled()
            },
            onError = OnError { errorInfo ->
                listener?.onPayPalFailure(PayPalCheckoutResult.Failure(ErrorInfo(errorInfo)))
            })
    }
}
