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
    private val payPalWebLauncher: PayPalWebLauncher
) {

    // for analytics tracking
    private var checkoutOrderId: String? = null
    private var vaultSetupTokenId: String? = null

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
    )

    /**
     * Confirm PayPal payment source for an order.
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(
        activity: ComponentActivity,
        request: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        checkoutOrderId = request.orderId
        analytics.notify(CheckoutEvent.STARTED, checkoutOrderId)

        val result = payPalWebLauncher.launchPayPalWebCheckout(activity, request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> analytics.notify(
                CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                checkoutOrderId
            )

            is PayPalPresentAuthChallengeResult.Failure ->
                analytics.notify(CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, checkoutOrderId)
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
        vaultSetupTokenId = request.setupTokenId
        analytics.notify(VaultEvent.STARTED, vaultSetupTokenId)

        val result = payPalWebLauncher.launchPayPalWebVault(activity, request)
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> analytics.notify(
                VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                vaultSetupTokenId
            )

            is PayPalPresentAuthChallengeResult.Failure ->
                analytics.notify(VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, vaultSetupTokenId)
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
     * @param [authState] A continuation state received from [PayPalPresentAuthChallengeResult.Success]
     * when calling [PayPalWebCheckoutClient.start]. This is needed to properly verify that an
     * authorization completed successfully.
     */
    fun finishStart(intent: Intent, authState: String): PayPalWebCheckoutFinishStartResult {
        val result = payPalWebLauncher.completeCheckoutAuthRequest(intent, authState)
        when (result) {
            is PayPalWebCheckoutFinishStartResult.Success ->
                analytics.notify(CheckoutEvent.SUCCEEDED, checkoutOrderId)

            is PayPalWebCheckoutFinishStartResult.Canceled ->
                analytics.notify(CheckoutEvent.CANCELED, checkoutOrderId)

            is PayPalWebCheckoutFinishStartResult.Failure ->
                analytics.notify(CheckoutEvent.FAILED, checkoutOrderId)

            PayPalWebCheckoutFinishStartResult.NoResult -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.vault]), call this method to see if a user has
     * successfully authorized a PayPal account for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     * @param [authState] A continuation state received from [PayPalPresentAuthChallengeResult.Success]
     * when calling [PayPalWebCheckoutClient.vault]. This is needed to properly verify that an
     * authorization completed successfully.
     */
    fun finishVault(intent: Intent, authState: String): PayPalWebCheckoutFinishVaultResult {
        val result = payPalWebLauncher.completeVaultAuthRequest(intent, authState)
        // TODO: see if we can get setup token id from somewhere for tracking
        when (result) {
            is PayPalWebCheckoutFinishVaultResult.Success ->
                analytics.notify(VaultEvent.SUCCEEDED, vaultSetupTokenId)

            is PayPalWebCheckoutFinishVaultResult.Failure ->
                analytics.notify(VaultEvent.FAILED, vaultSetupTokenId)

            PayPalWebCheckoutFinishVaultResult.Canceled ->
                analytics.notify(VaultEvent.CANCELED, vaultSetupTokenId)

            PayPalWebCheckoutFinishVaultResult.NoResult -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }
}
