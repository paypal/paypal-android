package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val analytics: PayPalWebAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher,
    private val sessionStore: PayPalSessionStore
) {

    /**
     * Capture instance state for later restoration. This can be useful for recovery during a
     * process kill.
     */
    val instanceState: String
        get() = sessionStore.toBase64EncodedJSON()

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param context an Android context
     * @param configuration a [CoreConfig] object
     * @param urlScheme the custom URl scheme used to return to your app from a browser switch flow
     */
    constructor(context: Context, configuration: CoreConfig, urlScheme: String) : this(
        PayPalWebAnalytics(AnalyticsService(context.applicationContext, configuration)),
        PayPalWebLauncher(urlScheme, configuration),
        PayPalSessionStore()
    )

    /**
     * Restore a feature client using instance state. @see [instanceState]
     */
    fun restore(instanceState: String) {
        sessionStore.restore(instanceState)
    }

    /**
     * Confirm PayPal payment source for an order.
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(request: PayPalWebCheckoutRequest): PayPalPresentAuthChallengeResult {
        sessionStore.clear()
        sessionStore.checkoutOrderId = request.orderId

        analytics.notify(CheckoutEvent.STARTED, sessionStore.checkoutOrderId)

        val result = payPalWebLauncher.launchPayPalWebCheckout(request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    sessionStore.checkoutOrderId
                )
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure ->
                analytics.notify(CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, sessionStore.checkoutOrderId)
        }
        return result
    }

    /**
     * Vault PayPal as a payment method.
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    fun vault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        sessionStore.clear()

        sessionStore.vaultSetupTokenId = request.setupTokenId
        analytics.notify(VaultEvent.STARTED, sessionStore.vaultSetupTokenId)

        val result = payPalWebLauncher.launchPayPalWebVault(activity, request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    sessionStore.vaultSetupTokenId
                )
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure ->
                analytics.notify(VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, sessionStore.vaultSetupTokenId)
        }
        return result
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.start]), call this method to see if a user has
     * successfully authorized a PayPal account as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishStart(intent: Intent): PayPalWebCheckoutFinishStartResult? =
        sessionStore.authState?.let { authState ->
            val result = payPalWebLauncher.completeCheckoutAuthRequest(intent, authState)
            when (result) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                    analytics.notify(CheckoutEvent.SUCCEEDED, sessionStore.checkoutOrderId)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    analytics.notify(CheckoutEvent.CANCELED, sessionStore.checkoutOrderId)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    analytics.notify(CheckoutEvent.FAILED, sessionStore.checkoutOrderId)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishStartResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            result
        }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.vault]), call this method to see if a user has
     * successfully authorized a PayPal account for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishVault(intent: Intent): PayPalWebCheckoutFinishVaultResult? =
        sessionStore.authState?.let { authState ->
            val result = payPalWebLauncher.completeVaultAuthRequest(intent, authState)
            // TODO: see if we can get setup token id from somewhere for tracking
            when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success -> {
                    analytics.notify(VaultEvent.SUCCEEDED, sessionStore.vaultSetupTokenId)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishVaultResult.Failure -> {
                    analytics.notify(VaultEvent.FAILED, sessionStore.vaultSetupTokenId)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.Canceled -> {
                    analytics.notify(VaultEvent.CANCELED, sessionStore.vaultSetupTokenId)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            result
        }
}
