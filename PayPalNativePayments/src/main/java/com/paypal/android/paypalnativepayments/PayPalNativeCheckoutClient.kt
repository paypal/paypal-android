package com.paypal.android.paypalnativepayments

import android.app.Application
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.SecureTokenServiceAPI
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.UIConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError
import com.paypal.checkout.shipping.OnShippingChange
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData
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
    private val returnUrl: String,
    private val secureTokenServiceAPI: SecureTokenServiceAPI,
    private val analyticsService: AnalyticsService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    constructor(application: Application, coreConfig: CoreConfig, returnUrl: String) :
            this(
                application,
                coreConfig,
                returnUrl,
                SecureTokenServiceAPI(coreConfig),
                AnalyticsService(application.applicationContext, coreConfig)
            )

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
                registerCallbacks()
            }
        }
    /**
     * Initiate a PayPal checkout for an order.
     *
     * @param returnUrl This is the Return URL value that was added to your app in the
     * PayPal Developer Portal. Please ensure that this value is set in the PayPal Developer Portal,
     * as it is required for a successful checkout flow. The Return URL should contain your app's
     * package name appended with "://paypalpay". Example: "com.sample.example://paypalpay".
     * See Also: [Developer Portal](https://developer.paypal.com/developer/applications/)
     * @param createOrder the id of the order
     */
    fun startCheckout(createOrder: CreateOrder) {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:started")

        CoroutineScope(dispatcher).launch(exceptionHandler) {
            try {
                val clientID = secureTokenServiceAPI.fetchCachedOrRemoteClientID()

                val config = CheckoutConfig(
                    application = application,
                    clientId = clientID,
                    environment = getPayPalEnvironment(coreConfig.environment),
                    uiConfig = UIConfig(
                        showExitSurveyDialog = false
                    ),
                    returnUrl = returnUrl
                )
                PayPalCheckout.setConfig(config)
                listener?.onPayPalCheckoutStart()
                PayPalCheckout.startCheckout(createOrder)
            } catch (e: PayPalSDKError) {
                listener?.onPayPalCheckoutFailure(APIClientError.clientIDNotFoundError(e.code, e.correlationID))
            }
        }
    }

    private fun registerCallbacks() {
        PayPalCheckout.registerCallbacks(
            onApprove = OnApprove { approval ->
                val result = approval.run {
                    PayPalNativeCheckoutResult(this)
                }
                notifyOnSuccess(result)
            },
            onCancel = OnCancel {
                notifyOnCancel()
            },
            onError = OnError { errorInfo ->
                notifyOnFailure(PayPalNativeCheckoutError(0, errorInfo.reason, errorInfo = errorInfo))
            },
            onShippingChange = OnShippingChange { shippingChangeData, shippingChangeActions ->
                notifyOnShippingChange(shippingChangeData, shippingChangeActions)
            }
        )
    }

    private fun notifyOnFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:failed")
        listener?.onPayPalCheckoutFailure(error)
    }

    private fun notifyOnSuccess(result: PayPalNativeCheckoutResult) {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:succeeded")
        listener?.onPayPalCheckoutSuccess(result)
    }

    private fun notifyOnShippingChange(
        shippingChangeData: ShippingChangeData,
        shippingChangeActions: ShippingChangeActions
    ) {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:shipping-address-changed")
        listener?.onPayPalCheckoutShippingChange(shippingChangeData, shippingChangeActions)
    }

    private fun notifyOnCancel() {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:canceled")
        listener?.onPayPalCheckoutCanceled()
    }
}
