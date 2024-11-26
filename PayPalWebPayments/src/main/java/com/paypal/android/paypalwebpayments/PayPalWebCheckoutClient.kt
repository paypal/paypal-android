package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.analytics.AnalyticsService

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val analyticsService: AnalyticsService,
    private val analytics: PayPalWebAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher
) {

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param context an Android context
     * @param configuration a [CoreConfig] object
     * @param urlScheme the custom URl scheme used to return to your app from a browser switch flow
     */
    constructor(context: Context, configuration: CoreConfig, urlScheme: String) : this(
        AnalyticsService(context.applicationContext, configuration),
        PayPalWebAnalytics(AnalyticsService(context.applicationContext, configuration)),
        PayPalWebLauncher(urlScheme, configuration),
    )

    /**
     * Sets a listener to receive notifications when a PayPal Checkout event occurs.
     */
    var listener: PayPalWebCheckoutListener? = null

    /**
     * Sets a listener to receive notifications when a Paypal Vault event occurs.
     */
    var vaultListener: PayPalWebVaultListener? = null

    /**
     * Confirm PayPal payment source for an order. Result will be delivered to your [PayPalWebCheckoutListener].
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(
        activity: ComponentActivity,
        request: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        analytics.notifyCheckoutStarted(request.orderId)
        val result = payPalWebLauncher.launchPayPalWebCheckout(activity, request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notifyCheckoutAuthChallengeFailed(request.orderId)
                listener?.onPayPalWebFailure(result.error)
            }

            is PayPalPresentAuthChallengeResult.Success ->
                analytics.notifyCheckoutAuthChallengeStarted(request.orderId)
        }
        return result
    }

    /**
     * Vault PayPal as a payment method. Result will be delivered to your [PayPalWebVaultListener].
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    fun vault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        analytics.notifyVaultStarted(request.setupTokenId)
        val result = payPalWebLauncher.launchPayPalWebVault(activity, request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notifyVaultAuthChallengeFailed(request.setupTokenId)
                vaultListener?.onPayPalWebVaultFailure(result.error)
            }

            is PayPalPresentAuthChallengeResult.Success ->
                analytics.notifyVaultAuthChallengeStarted(request.setupTokenId)
        }
        return result
    }

    fun completeAuthChallenge(intent: Intent, authState: String): PayPalWebStatus {
        val status = payPalWebLauncher.completeAuthRequest(intent, authState)
        when (status) {
            is PayPalWebStatus.CheckoutSuccess -> {
                analytics.notifyCheckoutAuthChallengeSucceeded(status.result.orderId)
                listener?.onPayPalWebSuccess(status.result)
            }

            is PayPalWebStatus.CheckoutError -> {
                analytics.notifyCheckoutAuthChallengeFailed(status.orderId)
                listener?.onPayPalWebFailure(status.error)
            }

            is PayPalWebStatus.CheckoutCanceled -> {
                analytics.notifyCheckoutAuthChallengeCanceled(status.orderId)
                listener?.onPayPalWebCanceled()
            }

            is PayPalWebStatus.VaultSuccess -> {
                // TODO: see if we can get setup token id from somewhere
                analytics.notifyVaultAuthChallengeSucceeded(null)
                vaultListener?.onPayPalWebVaultSuccess(status.result)
            }
            is PayPalWebStatus.VaultError -> {
                // TODO: see if we can get setup token id from somewhere
                analytics.notifyVaultAuthChallengeFailed(null)
                vaultListener?.onPayPalWebVaultFailure(status.error)
            }
            PayPalWebStatus.VaultCanceled -> {
                // TODO: see if we can get setup token id from somewhere
                analytics.notifyVaultAuthChallengeCanceled(null)
                vaultListener?.onPayPalWebVaultCanceled()
            }
            is PayPalWebStatus.UnknownError -> {
                Log.d("PayPalSDK", "An unknown error occurred: ${status.error.message}")
            }

            PayPalWebStatus.NoResult -> {
                // ignore
            }
        }
        return status
    }

    /**
     * Call this method at the end of the web checkout flow to clear out all observers and listeners
     */
    fun removeObservers() {
        vaultListener = null
        listener = null
    }
}
