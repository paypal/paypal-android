package com.paypal.android.checkout

import android.app.Application
import com.paypal.android.corepayments.API
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.UIConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError
import com.paypal.checkout.shipping.OnShippingChange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to checkout with PayPal.
 */
class PayPalNativeCheckoutClient internal constructor (
    private val application: Application,
    private val coreConfig: CoreConfig,
    private val api: API,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    constructor(application: Application, coreConfig: CoreConfig) :
            this(application, coreConfig, API(coreConfig, application))

    private val exceptionHandler = CoreCoroutineExceptionHandler {
        listener?.onPayPalCheckoutFailure(it)
    }

    /**
     * Sets a listener to receive notifications when a PayPal event occurs.
     */
    var listener: PayPalNativeCheckoutListener? = null
        set(value) {
            field = value
            if (value != null) {
                registerCallbacks(value)
            }
        }
    /**
     * Initiate a PayPal checkout for an order.
     *
     * @param createOrder the id of the order
     */
    fun startCheckout(createOrder: CreateOrder) {
        CoroutineScope(dispatcher).launch(exceptionHandler) {
            val config = CheckoutConfig(
                application = application,
                clientId = api.getClientId(),
                environment = getPayPalEnvironment(coreConfig.environment),
                uiConfig = UIConfig(
                    showExitSurveyDialog = false
                )
            )
            PayPalCheckout.setConfig(config)
            listener?.onPayPalCheckoutStart()
            PayPalCheckout.startCheckout(createOrder)
        }
    }

    private fun registerCallbacks(listener: PayPalNativeCheckoutListener) {
        PayPalCheckout.registerCallbacks(
            onApprove = OnApprove { approval ->
                val result = approval.run {
                    PayPalNativeCheckoutResult(this)
                }
                listener.onPayPalCheckoutSuccess(result)
            },
            onCancel = OnCancel {
                listener.onPayPalCheckoutCanceled()
            },
            onError = OnError { errorInfo ->
                listener.onPayPalCheckoutFailure(PayPalNativeCheckoutError(0, errorInfo.reason, errorInfo = errorInfo))
            },
            onShippingChange = OnShippingChange { shippingChangeData, shippingChangeActions ->
                listener.onPayPalCheckoutShippingChange(shippingChangeData, shippingChangeActions)
            }
        )
    }
}
