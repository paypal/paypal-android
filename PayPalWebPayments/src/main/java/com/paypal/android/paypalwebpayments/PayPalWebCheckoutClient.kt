package com.paypal.android.paypalwebpayments

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    private val exceptionHandler = CoreCoroutineExceptionHandler {
        deliverFailure(it)
    }

    private var browserSwitchResult: BrowserSwitchResult? = null

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
            browserSwitchResult?.also {
                handleBrowserSwitchResult()
            }
        }

    var vaultListener: PayPalWebCheckoutVaultListener? = null
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
        analyticsService.sendAnalyticsEvent("paypal-web-payments:started", orderId)

        CoroutineScope(dispatcher).launch(exceptionHandler) {
            try {
                val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(
                    request.orderId,
                    coreConfig,
                    request.fundingSource
                )
                browserSwitchClient.start(activity, browserSwitchOptions)
            } catch (e: PayPalSDKError) {
                deliverFailure(APIClientError.clientIDNotFoundError(e.code, e.correlationId))
            }
        }
    }

    internal fun handleBrowserSwitchResult() {
        browserSwitchResult = browserSwitchClient.deliverResult(activity)
        vaultListener?.also {
            browserSwitchResult?.also { result ->
                val isVaultResult = result.requestMetadata?.has("setup_token_id") ?: false
                when (result.status) {
                    BrowserSwitchStatus.SUCCESS -> {
                        if (isVaultResult) {
                            deliverVaultSuccess()
                        } else {
                            deliverSuccess()
                        }
                    }

                    BrowserSwitchStatus.CANCELED -> {
                        if (isVaultResult) {
                            deliverVaultCancellation()
                        } else {
                            deliverCancellation()
                        }
                    }
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
                deliverSuccess(
                    PayPalWebCheckoutResult(
                        webResult.orderId,
                        webResult.payerId
                    )
                )
            } else {
                deliverFailure(PayPalWebCheckoutError.malformedResultError)
            }
        } else {
            deliverFailure(PayPalWebCheckoutError.unknownError)
        }
        browserSwitchResult = null
    }

    private fun deliverCancellation() {
        browserSwitchResult = null
        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", orderId)
        listener?.onPayPalWebCanceled()
    }

    private fun deliverVaultSuccess() {
        val deepLinkUrlResult = browserSwitchResult?.deepLinkUrl
        val requestMetadata = browserSwitchResult?.requestMetadata

        // Setup Token Approval URL:
        // {custom_url_scheme}://vault_paypal/success?approval_token_id={value}&approval_session_id={value}
        if (deepLinkUrlResult != null && requestMetadata != null) {
            val approvalTokenId = deepLinkUrlResult.getQueryParameter("approval_token_id")
            val approvalSessionId = deepLinkUrlResult.getQueryParameter("approval_session_id")
            val result = PayPalWebCheckoutVaultResult(approvalTokenId, approvalSessionId)
            vaultListener?.onPayPalWebVaultSuccess(result)
        } else {
            vaultListener?.onPayPalWebVaultFailure(PayPalWebCheckoutError.malformedResultError)
        }
        browserSwitchResult = null
    }

    private fun deliverVaultCancellation() {
        browserSwitchResult = null
        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", orderId)
        vaultListener?.onPayPalWebVaultCanceled()
    }

    private fun deliverFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:failed", orderId)
        listener?.onPayPalWebFailure(error)
    }

    private fun deliverSuccess(result: PayPalWebCheckoutResult) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:succeeded", orderId)
        listener?.onPayPalWebSuccess(result)
    }

    fun vault(activity: AppCompatActivity, setupTokenId: String, approveVaultHref: String) {
        val browserSwitchOptions = browserSwitchHelper.configurePayPalVaultApproveSwitchOptions(
            setupTokenId,
            approveVaultHref
        )
        browserSwitchClient.start(activity, browserSwitchOptions)
    }
}
