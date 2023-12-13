package com.paypal.android.paypalwebpayments

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.CoreCoroutineExceptionHandler
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService
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
    // NEXT MAJOR VERSION: remove hardcoded activity reference
    private val activity: FragmentActivity,
    private val coreConfig: CoreConfig,
    private val analyticsService: AnalyticsService,
    private val browserSwitchClient: BrowserSwitchClient,
    private val browserSwitchHelper: BrowserSwitchHelper,
    val experienceContext: PayPalWebCheckoutVaultExperienceContext,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    companion object {
        internal const val VAULT_DOMAIN = "vault_paypal"
        internal const val VAULT_RETURN_URL_PATH = "success"
        internal const val VAULT_CANCEL_URL_PATH = "user_canceled"

        internal const val DEEP_LINK_PARAM_APPROVAL_TOKEN_ID = "approval_token_id"
        internal const val DEEP_LINK_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
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
        notifyWebCheckoutFailure(it)
    }

    /**
     * Sets a listener to receive notifications when a PayPal Checkout event occurs.
     */
    var listener: PayPalWebCheckoutListener? = null

    /**
     * Sets a listener to receive notificatioins when a Paypal Vault event occurs.
     */
    var vaultListener: PayPalWebCheckoutVaultListener? = null

    init {
        activity.lifecycle.addObserver(PayPalWebCheckoutLifeCycleObserver(this))
        // NEXT MAJOR VERSION: remove hardcoded activity reference
    }

    /**
     * Confirm PayPal payment source for an order. Result will be delivered to your [PayPalWebCheckoutListener].
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(request: PayPalWebCheckoutRequest) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:started", request.orderId)

        CoroutineScope(dispatcher).launch(exceptionHandler) {
            try {
                val browserSwitchOptions = browserSwitchHelper.configurePayPalBrowserSwitchOptions(
                    request.orderId,
                    coreConfig,
                    request.fundingSource
                )
                browserSwitchClient.start(activity, browserSwitchOptions)
            } catch (e: PayPalSDKError) {
                notifyWebCheckoutFailure(
                    APIClientError.clientIDNotFoundError(
                        e.code,
                        e.correlationId
                    )
                )
            }
        }
    }

    internal fun handleBrowserSwitchResult() {
        val browserSwitchResult = browserSwitchClient.deliverResult(activity)
        browserSwitchHelper.parsePayPalWebStatus(browserSwitchResult)?.let { status ->
            when (status) {
                is PayPalWebStatus.CheckoutSuccess -> notifyWebCheckoutSuccess(status.result)
                is PayPalWebStatus.CheckoutError -> notifyWebCheckoutFailure(status.error)
                is PayPalWebStatus.CheckoutCanceled -> notifyWebCheckoutCancelation(status.orderId)
                is PayPalWebStatus.VaultSuccess -> notifyVaultSuccess(status.result)
                is PayPalWebStatus.VaultError -> notifyVaultFailure(status.error)
                PayPalWebStatus.VaultCanceled -> notifyVaultCancelation()
            }
        }
    }

    private fun notifyWebCheckoutSuccess(result: PayPalWebCheckoutResult) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:succeeded", null)
        listener?.onPayPalWebSuccess(result)
    }

    private fun notifyWebCheckoutFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:failed", null)
        listener?.onPayPalWebFailure(error)
    }

    private fun notifyWebCheckoutCancelation(
        orderId: String?
    ) {
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:browser-login:canceled",
            orderId
        )
        listener?.onPayPalWebCanceled()
    }

    private fun notifyVaultSuccess(result: PayPalWebCheckoutVaultResult) {
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:browser-login:canceled",
            null
        )
        vaultListener?.onPayPalWebVaultSuccess(result)
    }

    private fun notifyVaultFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:failed", null)
        vaultListener?.onPayPalWebVaultFailure(error)
    }

    private fun notifyVaultCancelation() {
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:browser-login:canceled",
            null
        )
        vaultListener?.onPayPalWebVaultCanceled()
    }

    fun vault(activity: AppCompatActivity, setupTokenId: String, approveVaultHref: String) {
        val browserSwitchOptions = browserSwitchHelper
            .configurePayPalVaultApproveSwitchOptions(setupTokenId, approveVaultHref)
        browserSwitchClient.start(activity, browserSwitchOptions)
    }
}
