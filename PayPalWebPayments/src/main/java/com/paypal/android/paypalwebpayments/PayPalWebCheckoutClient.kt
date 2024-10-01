package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val payPalAnalytics: PayPalAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher,
) {

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param context an Android [Context]
     */
    constructor(context: Context) : this(PayPalAnalytics(context.applicationContext))

    internal constructor(payPalAnalytics: PayPalAnalytics) :
            this(payPalAnalytics, PayPalWebLauncher(payPalAnalytics))

    /**
     * Confirm PayPal payment source for an order. Result will be delivered to your [PayPalWebCheckoutListener].
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    fun start(
        activity: AppCompatActivity,
        request: PayPalWebCheckoutRequest
    ): PayPalWebCheckoutStartResult {
        val analytics = payPalAnalytics.createAnalyticsContext(request)
        val authChallenge = payPalWebLauncher.createAuthChallenge(request, analytics.trackingId)
        val authChallengeResult = payPalWebLauncher.presentAuthChallenge(activity, authChallenge)
        return when (authChallengeResult) {
            is PayPalAuthChallengeResult.Success -> {
                analytics.notifyWebCheckoutStarted()
                PayPalWebCheckoutStartResult.DidLaunchAuth(authChallengeResult.authState)
            }

            is PayPalAuthChallengeResult.Failure -> {
                analytics.notifyWebCheckoutFailure()
                PayPalWebCheckoutStartResult.Failure(authChallengeResult.error)
            }
        }
    }

    /**
     * Vault PayPal as a payment method. Result will be delivered to your [PayPalWebVaultListener].
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    fun vault(
        activity: AppCompatActivity,
        request: PayPalWebVaultRequest
    ): PayPalWebCheckoutVaultResult {
        val analytics = payPalAnalytics.createAnalyticsContext(request)
        val authChallenge = payPalWebLauncher.createAuthChallenge(request, analytics.trackingId)
        val authChallengeResult = payPalWebLauncher.presentAuthChallenge(activity, authChallenge)
        return when (authChallengeResult) {
            is PayPalAuthChallengeResult.Success -> {
                analytics.notifyWebVaultStarted()
                PayPalWebCheckoutVaultResult.DidLaunchAuth(authChallengeResult.authState)
            }

            is PayPalAuthChallengeResult.Failure -> {
                analytics.notifyVaultFailure()
                PayPalWebCheckoutVaultResult.Failure(authChallengeResult.error)
            }
        }
    }

    fun getCheckoutAuthResult(intent: Intent, state: String): PayPalWebCheckoutAuthResult =
        payPalWebLauncher.getCheckoutAuthResult(intent, state)

    fun getVaultAuthResult(intent: Intent, state: String): PayPalWebVaultAuthResult =
        payPalWebLauncher.getVaultAuthResult(intent, state)
}
