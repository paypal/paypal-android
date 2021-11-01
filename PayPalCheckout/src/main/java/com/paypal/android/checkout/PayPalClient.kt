package com.paypal.android.checkout

import android.os.Build
import androidx.annotation.RequiresApi
import com.paypal.android.checkout.pojo.Approval
import com.paypal.android.checkout.pojo.ErrorInfo
import com.paypal.android.checkout.pojo.ShippingChangeData
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError
import com.paypal.checkout.shipping.OnShippingChange

@RequiresApi(Build.VERSION_CODES.M)
class PayPalClient(payPalConfig: PayPalConfiguration) {

    init {
        val config = CheckoutConfig(
            application = payPalConfig.application,
            clientId = payPalConfig.paymentsConfiguration.clientId,
            environment = getPayPalEnvironment(payPalConfig.paymentsConfiguration.environment),
            returnUrl = payPalConfig.returnUrl,
            currencyCode = payPalConfig.currencyCode?.asNativeCheckout,
            paymentButtonIntent = payPalConfig.paymentButtonIntent?.asNativeCheckout,
            settingsConfig = payPalConfig.settingsConfig.asNativeCheckout,
            userAction = payPalConfig.userAction?.asNativeCheckout
        )
        PayPalCheckout.setConfig(config)
    }

    fun checkout(orderId: String, complete: (PayPalCheckoutResult) -> Unit) {
        PayPalCheckout.start(CreateOrder { createOrderActions ->
            createOrderActions.set(orderId)
        },
            onApprove = OnApprove { approval ->
                complete(PayPalCheckoutResult.Success(Approval(approval)))
            },
            onShippingChange = OnShippingChange { shippingChangeData, _ ->
                complete(PayPalCheckoutResult.ShippingChange(ShippingChangeData(shippingChangeData)))
            },
            onCancel = OnCancel {
                complete(PayPalCheckoutResult.Cancellation)
            },
            onError = OnError { errorInfo ->
                complete(PayPalCheckoutResult.Failure(ErrorInfo(errorInfo)))
            })
    }
}
