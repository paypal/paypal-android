package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
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
        analyticsService.sendAnalyticsEvent("paypal-web-payments:started", request.orderId)
        val result = payPalWebLauncher.launchPayPalWebCheckout(activity, request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Failure -> {
                notifyWebCheckoutFailure(result.error, request.orderId)
            }

            is PayPalPresentAuthChallengeResult.Success -> {
                // TODO: track success with analytics
            }
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
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:started")
        val result = payPalWebLauncher.launchPayPalWebVault(activity, request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Failure -> {
                notifyVaultFailure(result.error)
            }

            is PayPalPresentAuthChallengeResult.Success -> {
                // TODO: track success with analytics
            }
        }
        return result
    }

    fun completeAuthChallenge(intent: Intent, authState: String): PayPalWebStatus {
        val status = payPalWebLauncher.completeAuthRequest(intent, authState)
        when (status) {
            is PayPalWebStatus.CheckoutSuccess -> notifyWebCheckoutSuccess(status.result)
            is PayPalWebStatus.CheckoutError -> status.run {
                notifyWebCheckoutFailure(error, orderId)
            }

            is PayPalWebStatus.CheckoutCanceled -> notifyWebCheckoutCancelation(status.orderId)
            is PayPalWebStatus.VaultSuccess -> notifyVaultSuccess(status.result)
            is PayPalWebStatus.VaultError -> notifyVaultFailure(status.error)
            PayPalWebStatus.VaultCanceled -> notifyVaultCancelation()
            PayPalWebStatus.NoResult -> {
                // ignore
            }
        }
        return status
    }

    private fun notifyWebCheckoutSuccess(result: PayPalWebCheckoutResult) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:succeeded", result.orderId)
        listener?.onPayPalWebSuccess(result)
    }

    private fun notifyWebCheckoutFailure(error: PayPalSDKError, orderId: String?) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:failed", orderId)
        listener?.onPayPalWebFailure(error)
    }

    private fun notifyWebCheckoutCancelation(orderId: String?) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", orderId)
        listener?.onPayPalWebCanceled()
    }

    private fun notifyVaultSuccess(result: PayPalWebVaultResult) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:succeeded")
        vaultListener?.onPayPalWebVaultSuccess(result)
    }

    private fun notifyVaultFailure(error: PayPalSDKError) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:failed")
        vaultListener?.onPayPalWebVaultFailure(error)
    }

    private fun notifyVaultCancelation() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:canceled")
        vaultListener?.onPayPalWebVaultCanceled()
    }

    /**
     * Call this method at the end of the web checkout flow to clear out all observers and listeners
     */
    fun removeObservers() {
        vaultListener = null
        listener = null
    }
}
