package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.browserswitch.BrowserSwitchRequestCode
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import org.json.JSONObject


// TODO: consider renaming PayPalWebLauncher to PayPalAuthChallengeLauncher
internal class PayPalWebLauncher(
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
) {
    companion object {
        private const val METADATA_KEY_REQUEST_TYPE = "request_type"
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"

        private const val REQUEST_TYPE_CHECKOUT = "checkout"
        private const val REQUEST_TYPE_VAULT = "vault"

        private const val URL_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }

    fun createAuthChallenge(
        request: PayPalWebCheckoutRequest,
        analytics: PayPalAnalyticsContext
    ): PayPalAuthChallenge {
        val metadata = JSONObject()
            .put(METADATA_KEY_ORDER_ID, request.orderId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_CHECKOUT)
        val url = buildPayPalCheckoutUri(request)
        val options = BrowserSwitchOptions()
            .url(url)
            .requestCode(BrowserSwitchRequestCode.PAYPAL.intValue)
            .returnUrlScheme(request.urlScheme)
            .metadata(metadata)
        return PayPalAuthChallenge(options, analytics)
    }

    fun createAuthChallenge(
        request: PayPalWebVaultRequest,
        analytics: PayPalAnalyticsContext
    ): PayPalAuthChallenge {
        val metadata = JSONObject()
            .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
        val url = request.run { buildPayPalVaultUri(request) }
        val options = BrowserSwitchOptions()
            .url(url)
            .requestCode(BrowserSwitchRequestCode.PAYPAL.intValue)
            .returnUrlScheme(request.urlScheme)
            .metadata(metadata)
        return PayPalAuthChallenge(options, analytics)
    }

    fun presentAuthChallenge(
        activity: FragmentActivity,
        authChallenge: PayPalAuthChallenge,
    ): PayPalAuthChallengeResult {
        val analytics = authChallenge.analytics
        return when (val result = browserSwitchClient.start(activity, authChallenge.options)) {
            is BrowserSwitchStartResult.Failure -> {
                analytics.notifyWebCheckoutFailure()
                val message = "auth challenge failed"
                PayPalAuthChallengeResult.Failure(
                    PayPalSDKError(
                        123,
                        message,
                        reason = result.error
                    )
                )
            }

            is BrowserSwitchStartResult.Started -> {
                analytics.notifyWebCheckoutStarted()
                PayPalAuthChallengeResult.Success(result.pendingRequest)
            }
        }

    }

    fun launchPayPalWebCheckout(
        activity: FragmentActivity,
        request: PayPalWebCheckoutRequest,
    ): PayPalSDKError? {
        val metadata = JSONObject()
            .put(METADATA_KEY_ORDER_ID, request.orderId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_CHECKOUT)
        val url = request.run { buildPayPalCheckoutUri(request) }
        val browserSwitchOptions = BrowserSwitchOptions()
            .url(url)
            .requestCode(BrowserSwitchRequestCode.PAYPAL.intValue)
            .returnUrlScheme(request.urlScheme)
            .metadata(metadata)

        return launchBrowserSwitch(activity, browserSwitchOptions)
    }

    fun launchPayPalWebVault(
        activity: FragmentActivity,
        request: PayPalWebVaultRequest
    ): PayPalSDKError? {
        val metadata = JSONObject()
            .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
        val url = request.run { buildPayPalVaultUri(request) }
        val browserSwitchOptions = BrowserSwitchOptions()
            .url(url)
            .returnUrlScheme(request.urlScheme)
            .metadata(metadata)
        return launchBrowserSwitch(activity, browserSwitchOptions)
    }

    private fun launchBrowserSwitch(
        activity: FragmentActivity,
        options: BrowserSwitchOptions
    ): PayPalSDKError? {
        var error: PayPalSDKError? = null
        try {
            browserSwitchClient.start(activity, options)
        } catch (e: BrowserSwitchException) {
            error = PayPalWebCheckoutError.browserSwitchError(e)
        }
        return error
    }

    private fun buildPayPalCheckoutUri(request: PayPalWebCheckoutRequest): Uri {
        val config = request.config
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://www.paypal.com"
            Environment.SANDBOX -> "https://www.sandbox.paypal.com"
        }
        val urlScheme = request.urlScheme
        val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"
        return Uri.parse(baseURL)
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", request.orderId)
            .appendQueryParameter("redirect_uri", redirectUriPayPalCheckout)
            .appendQueryParameter("native_xo", "1")
            .appendQueryParameter("fundingSource", request.fundingSource.value)
            .build()
    }

    private fun buildPayPalVaultUri(request: PayPalWebVaultRequest): Uri {
        val config = request.config
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://paypal.com/agreements/approve"
            Environment.SANDBOX -> "https://sandbox.paypal.com/agreements/approve"
        }
        return Uri.parse(baseURL)
            .buildUpon()
            .appendQueryParameter("approval_session_id", request.setupTokenId)
            .build()
    }

    private fun parseWebCheckoutSuccessResult(
        browserSwitchResult: BrowserSwitchFinalResult.Success,
        orderId: String?
    ): PayPalWebCheckoutAuthResult {
        val deepLinkUrl = browserSwitchResult.returnUrl
        val metadata = browserSwitchResult.requestMetadata

        return if (metadata == null) {
            PayPalWebCheckoutAuthResult.Failure(PayPalWebCheckoutError.unknownError, orderId)
        } else {
            // TODO: check for canceled status
            val payerId = deepLinkUrl.getQueryParameter("PayerID")
            val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
            if (orderId.isNullOrBlank() || payerId.isNullOrBlank()) {
                val error = PayPalWebCheckoutError.malformedResultError
                PayPalWebCheckoutAuthResult.Failure(error, orderId)
            } else {
                PayPalWebCheckoutAuthResult.Success(orderId, payerId)
            }
        }
    }

    fun checkIfCheckoutAuthComplete(intent: Intent, state: String): PayPalWebCheckoutAuthResult =
        when (val result = browserSwitchClient.completeRequest(intent, state)) {
            is BrowserSwitchFinalResult.Success -> {
                val requestType = result.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_CHECKOUT) {
                    val orderId = result.requestMetadata?.optString(METADATA_KEY_ORDER_ID)
                    parseWebCheckoutSuccessResult(result, orderId)
                } else {
                    PayPalWebCheckoutAuthResult.NoResult
                }
            }

            is BrowserSwitchFinalResult.Failure -> PayPalWebCheckoutAuthResult.Failure(
                PayPalSDKError(123, "browser switch error", reason = result.error)
            )

            BrowserSwitchFinalResult.NoResult -> PayPalWebCheckoutAuthResult.NoResult
        }

    fun checkIfVaultAuthComplete(intent: Intent, state: String): PayPalWebVaultAuthResult =
        when (val result = browserSwitchClient.completeRequest(intent, state)) {
            is BrowserSwitchFinalResult.Success -> {
                val requestType = result.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_VAULT) {
                    parseVaultSuccessResult(result)
                } else {
                    PayPalWebVaultAuthResult.NoResult
                }
            }

            is BrowserSwitchFinalResult.Failure -> PayPalWebVaultAuthResult.Failure(
                PayPalSDKError(123, "browser switch error", reason = result.error)
            )

            BrowserSwitchFinalResult.NoResult -> PayPalWebVaultAuthResult.NoResult
        }

    private fun parseVaultSuccessResult(browserSwitchResult: BrowserSwitchFinalResult.Success): PayPalWebVaultAuthResult {
        val deepLinkUrl = browserSwitchResult.returnUrl
        val requestMetadata = browserSwitchResult.requestMetadata
        return if (requestMetadata == null) {
            PayPalWebVaultAuthResult.Failure(PayPalWebCheckoutError.unknownError)
        } else {
            val approvalSessionId = deepLinkUrl.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
            if (approvalSessionId.isNullOrEmpty()) {
                PayPalWebVaultAuthResult.Failure(PayPalWebCheckoutError.malformedResultError)
            } else {
                PayPalWebVaultAuthResult.Success(approvalSessionId)
            }
        }
    }
}
