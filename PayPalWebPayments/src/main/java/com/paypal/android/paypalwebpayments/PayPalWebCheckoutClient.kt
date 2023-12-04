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

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val activity: FragmentActivity,
    private val coreConfig: CoreConfig,
    private val analyticsService: AnalyticsService,
    private val browserSwitchClient: BrowserSwitchClient,
    private val browserSwitchHelper: BrowserSwitchHelper,
    val experienceContext: PayPalWebCheckoutVaultExperienceContext,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    companion object {
        private const val VAULT_DOMAIN = "vault_paypal"
        private const val VAULT_RETURN_URL_PATH = "success"
        private const val VAULT_CANCEL_URL_PATH = "user_canceled"

        private const val DEEP_LINK_PARAM_APPROVAL_TOKEN_ID = "approval_token_id"
        private const val DEEP_LINK_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }

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
        BrowserSwitchHelper(urlScheme),
        PayPalWebCheckoutVaultExperienceContext(
            urlScheme,
            domain = VAULT_DOMAIN,
            returnUrlPath = VAULT_RETURN_URL_PATH,
            cancelUrlPath = VAULT_CANCEL_URL_PATH
        )
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
                val isVaultResult = result.deepLinkUrl?.path?.contains(VAULT_DOMAIN) ?: false
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

        if (deepLinkUrlResult != null && requestMetadata != null) {
            val isFailure = deepLinkUrlResult.path?.contains(VAULT_CANCEL_URL_PATH) ?: false
            if (isFailure) {
                vaultListener?.onPayPalWebVaultFailure(PayPalWebCheckoutError.malformedResultError)
            } else {
                val approvalTokenId =
                    deepLinkUrlResult.getQueryParameter(DEEP_LINK_PARAM_APPROVAL_TOKEN_ID)
                val approvalSessionId =
                    deepLinkUrlResult.getQueryParameter(DEEP_LINK_PARAM_APPROVAL_SESSION_ID)
                val result = PayPalWebCheckoutVaultResult(approvalTokenId, approvalSessionId)
                vaultListener?.onPayPalWebVaultSuccess(result)
            }
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
