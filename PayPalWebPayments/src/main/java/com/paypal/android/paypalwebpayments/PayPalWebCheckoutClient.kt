package com.paypal.android.paypalwebpayments

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val activity: FragmentActivity,
    private val coreConfig: CoreConfig,
    private val analyticsService: AnalyticsService,
    private val browserSwitchClient: BrowserSwitchClient,
    private val browserSwitchHelper: BrowserSwitchHelper,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param activity a [FragmentActivity]
     * @param configuration a [CoreConfig] object
     * @param urlScheme the custom URl scheme used to return to your app from a browser switch flow
     */
    constructor(
        activity: FragmentActivity,
        configuration: CoreConfig,
        urlScheme: String
    ) : this(
        activity,
        configuration,
        AnalyticsService(activity.applicationContext, configuration),
        BrowserSwitchClient(),
        BrowserSwitchHelper(urlScheme)
    )

    private var payPalAuthResult: PayPalWebAuthChallengeResult? = null

    private var orderId: String? = null

    /**
     * Sets a listener to receive notifications when a PayPal event occurs.
     */
    var listener: PayPalWebCheckoutListener? = null
        /**
         * @param value a [PayPalWebCheckoutListener] to receive results from the PayPal flow
         */
        set(value) {
            field = value
            payPalAuthResult?.also {
                deliverContinuationResult(it)
            }
        }

    init {
        activity.lifecycle.addObserver(PayPalWebCheckoutLifeCycleObserver(this))
    }

    /**
     * Confirm PayPal payment source for an order. Result will be delivered to your [PayPalWebCheckoutListener].
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(request: PayPalWebCheckoutRequest): PayPalWebAuthChallenge {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:started", orderId)

        val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(
            request.orderId,
            coreConfig,
            request.fundingSource
        )
        return PayPalWebAuthChallenge(browserSwitchOptions)
    }

    private fun deliverContinuationResult(payPalAuthResult: PayPalWebAuthChallengeResult) {
        if (listener != null) {
            if (payPalAuthResult is PayPalWebAuthChallengeSuccess) {
                analyticsService.sendAnalyticsEvent("paypal-web-payments:succeeded", orderId)
                val result = payPalAuthResult.run { PayPalWebCheckoutResult(orderId, payerId) }
                listener?.onPayPalWebSuccess(result)
            } else if (payPalAuthResult is PayPalWebAuthChallengeError) {
                if (payPalAuthResult.error === PayPalWebCheckoutError.userCanceledError) {
                    analyticsService.sendAnalyticsEvent(
                        "paypal-web-payments:browser-login:canceled",
                        orderId
                    )
                    listener?.onPayPalWebCanceled()
                } else {
                    analyticsService.sendAnalyticsEvent("paypal-web-payments:failed", orderId)
                    listener?.onPayPalWebFailure(payPalAuthResult.error)
                }
            }
            this.payPalAuthResult = null
        }
    }

    fun continueStart(payPalAuthResult: PayPalWebAuthChallengeResult) {
        this.payPalAuthResult = payPalAuthResult
        deliverContinuationResult(payPalAuthResult)
    }
}
