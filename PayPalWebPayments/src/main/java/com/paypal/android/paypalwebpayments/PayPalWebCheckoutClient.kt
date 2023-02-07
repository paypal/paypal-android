package com.paypal.android.paypalwebpayments

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.corepayments.*
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.*

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val activity: FragmentActivity,
    private val coreConfig: CoreConfig,
    private val api: API,
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
        API(configuration, activity),
        BrowserSwitchClient(),
        BrowserSwitchHelper(urlScheme)
    )

    private val exceptionHandler = CoreCoroutineExceptionHandler {
        listener?.onPayPalWebFailure(it)
    }

    private var browserSwitchResult: BrowserSwitchResult? = null

    /**
     * Sets a listener to receive notifications when a PayPal event occurs.
     */
    var listener: PayPalWebCheckoutListener? = null
        /**
         * @param value a [PayPalWebCheckoutListener] to receive results from the PayPal flow
         */
        set(value) {
            field = value
            browserSwitchResult?.also {
                handleBrowserSwitchResult()
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
    fun start(request: PayPalWebCheckoutRequest) {
        CoroutineScope(dispatcher).launch(exceptionHandler) {
            try {
                api.fetchCachedOrRemoteClientID()
            } catch (e: PayPalSDKError) {
                listener?.onPayPalWebFailure(APIClientError.clientIDNotFoundError(e.code, e.correlationID))
            }
        }

        val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(
            request.orderID,
            coreConfig,
            request.fundingSource
        )
        browserSwitchClient.start(activity, browserSwitchOptions)
    }

    internal fun handleBrowserSwitchResult() {
        browserSwitchResult = browserSwitchClient.deliverResult(activity)
        listener?.also {
            browserSwitchResult?.also { result ->
                when (result.status) {
                    BrowserSwitchStatus.SUCCESS -> deliverSuccess()
                    BrowserSwitchStatus.CANCELED -> deliverCancellation()
                }
            }
        }
    }

    private fun deliverSuccess() {
        if (browserSwitchResult?.deepLinkUrl != null && browserSwitchResult?.requestMetadata != null) {
            val webResult = PayPalDeepLinkUrlResult(
                browserSwitchResult?.deepLinkUrl!!,
                browserSwitchResult?.requestMetadata!!
            )
            if (!webResult.orderId.isNullOrBlank() && !webResult.payerId.isNullOrBlank()) {
                listener?.onPayPalWebSuccess(
                    PayPalWebCheckoutResult(
                        webResult.orderId,
                        webResult.payerId
                    )
                )
            } else {
                listener?.onPayPalWebFailure(PayPalWebCheckoutError.malformedResultError)
            }
        } else {
            listener?.onPayPalWebFailure(PayPalWebCheckoutError.unknownError)
        }
        browserSwitchResult = null
    }

    private fun deliverCancellation() {
        browserSwitchResult = null
        listener?.onPayPalWebCanceled()
    }
}
