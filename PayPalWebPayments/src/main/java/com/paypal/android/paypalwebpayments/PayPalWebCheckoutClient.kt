package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.corepayments.Base64Utils
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebLauncher.Companion
import org.json.JSONObject

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val urlScheme: String,
    private val payPalAnalytics: PayPalAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher,
) {

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param context an Android [Context]
     * @param urlScheme the custom URl scheme used to return to your app from a browser switch flow
     */
    constructor(context: Context, urlScheme: String) : this(
        urlScheme,
        PayPalAnalytics(context.applicationContext),
        PayPalWebLauncher(urlScheme),
    )

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

        val metadata = JSONObject()
            .put(METADATA_KEY_ORDER_ID, request.orderId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_CHECKOUT)
        val url = request.run { buildPayPalCheckoutUri(orderId, request.config, fundingSource) }
        val options = BrowserSwitchOptions()
            .url(url)
            .requestCode(BrowserSwitchRequestCodes.PAYPAL.intValue)
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
        val authChallenge = PayPalAuthChallenge(options, analytics)
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

        val metadata = JSONObject()
            .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
        val url = request.run { buildPayPalVaultUri(request.setupTokenId, request.config) }
        val options = BrowserSwitchOptions()
            .url(url)
            .requestCode(BrowserSwitchRequestCodes.PAYPAL.intValue)
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
        val authChallenge = PayPalAuthChallenge(options, analytics)
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

    @Suppress("NestedBlockDepth")
    internal fun handleBrowserSwitchResult() {
//        activityReference.get()?.let { activity ->
//            payPalWebLauncher.deliverBrowserSwitchResult(activity)?.let { status ->
//                when (status) {
//                    is PayPalWebStatus.CheckoutSuccess -> notifyWebCheckoutSuccess(status.result)
//                    is PayPalWebStatus.CheckoutError -> status.run {
//                        notifyWebCheckoutFailure(error, orderId)
//                    }
//
//                    is PayPalWebStatus.CheckoutCanceled -> notifyWebCheckoutCancelation(status.orderId)
//                    is PayPalWebStatus.VaultSuccess -> notifyVaultSuccess(status.result)
//                    is PayPalWebStatus.VaultError -> notifyVaultFailure(status.error)
//                    PayPalWebStatus.VaultCanceled -> notifyVaultCancelation()
//                }
//            }
//        }
    }

    /**
     * Call this method at the end of the web checkout flow to clear out all observers and listeners
     */
    fun removeObservers() {
//        activityReference.get()?.let { it.lifecycle.removeObserver(observer) }
//        vaultListener = null
//        listener = null
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

    private fun buildPayPalCheckoutUri(
        orderId: String?,
        config: CoreConfig,
        funding: PayPalWebCheckoutFundingSource
    ): Uri {
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://www.paypal.com"
            Environment.SANDBOX -> "https://www.sandbox.paypal.com"
        }
        val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"
        return Uri.parse(baseURL)
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", redirectUriPayPalCheckout)
            .appendQueryParameter("native_xo", "1")
            .appendQueryParameter("fundingSource", funding.value)
            .build()
    }

    private fun buildPayPalVaultUri(
        setupTokenId: String,
        config: CoreConfig
    ): Uri {
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://paypal.com/agreements/approve"
            Environment.SANDBOX -> "https://sandbox.paypal.com/agreements/approve"
        }
        return Uri.parse(baseURL)
            .buildUpon()
            .appendQueryParameter("approval_session_id", setupTokenId)
            .build()
    }

    companion object {
        private const val METADATA_KEY_REQUEST_TYPE = "request_type"
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"

        private const val REQUEST_TYPE_CHECKOUT = "checkout"
        private const val REQUEST_TYPE_VAULT = "vault"

        private const val URL_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }
}
