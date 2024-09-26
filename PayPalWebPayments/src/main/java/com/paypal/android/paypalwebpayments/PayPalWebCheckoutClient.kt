package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.paypal.android.corepayments.Base64Utils
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import org.json.JSONObject

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
    constructor(context: Context) :
            this(PayPalAnalytics(context.applicationContext), PayPalWebLauncher())

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
        val authChallenge = payPalWebLauncher.createAuthChallenge(request, analytics)
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
        val authChallenge = payPalWebLauncher.createAuthChallenge(request, analytics)
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

    fun checkIfCheckoutAuthComplete(intent: Intent, state: String): PayPalWebCheckoutAuthResult {
        val authStateJSON =
            decodeCardAuthStateJSON(state) ?: return PayPalWebCheckoutAuthResult.NoResult
        val analytics = restoreAnalyticsContextFromAuthState(authStateJSON)
        val result = payPalWebLauncher.checkIfCheckoutAuthComplete(intent, state)
        when (result) {
            is PayPalWebCheckoutAuthResult.Success -> analytics?.notifyWebCheckoutSucceeded()
            is PayPalWebCheckoutAuthResult.Failure -> analytics?.notifyWebCheckoutFailure()
            PayPalWebCheckoutAuthResult.Canceled -> analytics?.notifyWebCheckoutUserCanceled()
            PayPalWebCheckoutAuthResult.NoResult -> {
                // do nothing
            }
        }
        return result
    }

    fun checkIfVaultAuthComplete(intent: Intent, state: String): PayPalWebVaultAuthResult {
        val authStateJSON =
            decodeCardAuthStateJSON(state) ?: return PayPalWebVaultAuthResult.NoResult
        val analytics = restoreAnalyticsContextFromAuthState(authStateJSON)
        val result = payPalWebLauncher.checkIfVaultAuthComplete(intent, state)
        when (result) {
            is PayPalWebVaultAuthResult.Success -> analytics?.notifyWebVaultSucceeded()
            is PayPalWebVaultAuthResult.Failure -> analytics?.notifyWebVaultFailure()
            PayPalWebVaultAuthResult.Canceled -> analytics?.notifyWebVaultUserCanceled()
            PayPalWebVaultAuthResult.NoResult -> {
                // do nothing
            }
        }
        return result
    }

    private fun restoreAnalyticsContextFromAuthState(stateJSON: JSONObject): PayPalAnalyticsContext? {
        val metadata = stateJSON.optJSONObject("metadata")
        val clientId = metadata?.optString("client_id")
        val environmentName = metadata?.optString("environment_name")
        val environment = try {
            Environment.valueOf(environmentName ?: "")
        } catch (e: IllegalArgumentException) {
            null
        }

        if (clientId.isNullOrEmpty() || environment == null) {
            return null
        }
        val coreConfig = CoreConfig(clientId, environment)
        val orderId = metadata.optString("order_id")
        return payPalAnalytics.createAnalyticsContext(coreConfig, orderId)
    }

    private fun decodeCardAuthStateJSON(state: String): JSONObject? {
        val authStateJSON = Base64Utils.parseBase64EncodedJSON(state)
        val requestCode = authStateJSON?.optInt("requestCode", -1) ?: -1
        if (requestCode != BrowserSwitchRequestCodes.PAYPAL.intValue) {
            // not a card result
            return null
        }
        return authStateJSON
    }
}
