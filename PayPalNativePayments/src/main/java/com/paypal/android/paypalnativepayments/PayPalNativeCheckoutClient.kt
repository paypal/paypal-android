package com.paypal.android.paypalnativepayments

import android.app.Application
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.checkout.PayPalCheckout
import com.paypal.checkout.approve.OnApprove
import com.paypal.checkout.cancel.OnCancel
import com.paypal.checkout.config.AuthConfig
import com.paypal.checkout.config.CheckoutConfig
import com.paypal.checkout.config.UIConfig
import com.paypal.checkout.createorder.CreateOrder
import com.paypal.checkout.error.OnError
import com.paypal.checkout.shipping.OnShippingChange
import com.paypal.checkout.shipping.ShippingChangeActions
import com.paypal.checkout.shipping.ShippingChangeData
import com.paypal.checkout.shipping.ShippingChangeType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Use this client to checkout with PayPal.
 */
class PayPalNativeCheckoutClient internal constructor(
    private val application: Application,
    private val coreConfig: CoreConfig,
    private val returnUrl: String,
    private val analyticsService: AnalyticsService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    /**
     * Create an instance of PayPalNativeCheckoutClient to process PayPal transactions
     * @param application your app's Application object
     * @param coreConfig CoreConfig to configure the paypal client
     * @param returnUrl This is the Return URL value that was added to your app in the
     * PayPal Developer Portal. Please ensure that this value is set in the PayPal Developer Portal,
     * as it is required for a successful checkout flow. The Return URL should contain your app's
     * package name appended with "://paypalpay". Example: "com.sample.example://paypalpay".
     * See Also: [Developer Portal](https://developer.paypal.com/developer/applications/)
     */
    constructor(application: Application, coreConfig: CoreConfig, returnUrl: String) :
            this(
                application,
                coreConfig,
                returnUrl,
                AnalyticsService(application, coreConfig),
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
     * Sets a listener to receive notifications when a change in shipping address or method occurs.
     */
    var shippingListener: PayPalNativeShippingListener? = null

    /**
     * Present a Paypal Paysheet and start a PayPal transaction.
     *
     * @param request the PayPalNativeCheckoutRequest for the transaction
     */
    fun startCheckout(request: PayPalNativeCheckoutRequest) {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:started", request.orderId)
        CoroutineScope(dispatcher).launch(exceptionHandler) {
            try {
                val authConfig: AuthConfig? =
                    request.userAuthenticationEmail?.let { AuthConfig(it) }
                val config = CheckoutConfig(
                    application = application,
                    clientId = coreConfig.clientId,
                    environment = getPayPalEnvironment(coreConfig.environment),
                    uiConfig = UIConfig(
                        showExitSurveyDialog = false
                    ),
                    returnUrl = returnUrl,
                    authConfig = authConfig
                )
                PayPalCheckout.setConfig(config)
                listener?.onPayPalCheckoutStart()
                PayPalCheckout.startCheckout(CreateOrder {
                    it.set(request.orderId)
                })
            } catch (e: PayPalSDKError) {
                listener?.onPayPalCheckoutFailure(
                    APIClientError.clientIDNotFoundError(
                        e.code,
                        e.correlationId
                    )
                )
            }
        }
    }

    private fun registerCallbacks() {
        PayPalCheckout.registerCallbacks(
            onApprove = OnApprove { approval ->
                val result = approval.data.run { PayPalNativeCheckoutResult(orderId, payerId) }
                notifyOnSuccess(result, approval.data.orderId)
            },
            onCancel = OnCancel {
                notifyOnCancel()
            },
            onError = OnError { errorInfo ->
                val description = errorInfo.reason
                val reason = PayPalNativeCheckoutError(errorInfo)
                val error = APIClientError.payPalNativeCheckoutError(description, reason)
                notifyOnFailure(error, errorInfo.orderId)
            },
            onShippingChange = OnShippingChange { shippingChangeData, shippingChangeActions ->
                notifyOnShippingChange(shippingChangeData, shippingChangeActions)
            }
        )
    }

    private fun notifyOnFailure(error: PayPalSDKError, orderId: String?) {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:failed", orderId)
        listener?.onPayPalCheckoutFailure(error)
    }

    private fun notifyOnSuccess(result: PayPalNativeCheckoutResult, orderId: String?) {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:succeeded", orderId)
        listener?.onPayPalCheckoutSuccess(result)
    }

    private fun notifyOnShippingChange(
        shippingChangeData: ShippingChangeData,
        shippingChangeActions: ShippingChangeActions
    ) {
        shippingListener?.let {
            when (shippingChangeData.shippingChangeType) {
                ShippingChangeType.ADDRESS_CHANGE -> {
                    analyticsService.sendAnalyticsEvent(
                        "paypal-native-payments:shipping-address-changed",
                        null
                    )
                    it.onPayPalNativeShippingAddressChange(
                        PayPalNativePaysheetActions(shippingChangeActions),
                        PayPalNativeShippingAddress(shippingChangeData.shippingAddress)
                    )
                }

                ShippingChangeType.OPTION_CHANGE -> {
                    analyticsService.sendAnalyticsEvent(
                        "paypal-native-payments:shipping-method-changed",
                        null
                    )
                    it.onPayPalNativeShippingMethodChange(
                        PayPalNativePaysheetActions(shippingChangeActions),
                        PayPalNativeShippingMethod(shippingChangeData.selectedShippingOption!!)
                    )
                }
            }
        }
    }

    private fun notifyOnCancel() {
        analyticsService.sendAnalyticsEvent("paypal-native-payments:canceled", null)
        listener?.onPayPalCheckoutCanceled()
    }
}
