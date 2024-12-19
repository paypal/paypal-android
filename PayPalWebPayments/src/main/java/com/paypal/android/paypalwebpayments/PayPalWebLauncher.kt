package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import org.json.JSONObject

// TODO: consider renaming PayPalWebLauncher to PayPalAuthChallengeLauncher
internal class PayPalWebLauncher(
    private val urlScheme: String,
    private val coreConfig: CoreConfig,
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
) {
    private val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"

    companion object {
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"

        private const val URL_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }

    fun launchPayPalWebCheckout(
        activity: ComponentActivity,
        request: PayPalWebCheckoutRequest,
    ): PayPalPresentAuthChallengeResult {
        val metadata = JSONObject()
            .put(METADATA_KEY_ORDER_ID, request.orderId)
        val url = request.run { buildPayPalCheckoutUri(orderId, coreConfig, fundingSource) }
        val options = BrowserSwitchOptions()
            .url(url)
            .requestCode(BrowserSwitchRequestCodes.PAYPAL_CHECKOUT)
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
        return launchBrowserSwitch(activity, options)
    }

    fun launchPayPalWebVault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        val metadata = JSONObject()
            .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
        val url = request.run { buildPayPalVaultUri(request.setupTokenId, coreConfig) }
        val options = BrowserSwitchOptions()
            .url(url)
            .requestCode(BrowserSwitchRequestCodes.PAYPAL_VAULT)
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
        return launchBrowserSwitch(activity, options)
    }

    private fun launchBrowserSwitch(
        activity: ComponentActivity,
        options: BrowserSwitchOptions
    ): PayPalPresentAuthChallengeResult =
        when (val startResult = browserSwitchClient.start(activity, options)) {
            is BrowserSwitchStartResult.Started -> {
                PayPalPresentAuthChallengeResult.Success(startResult.pendingRequest)
            }

            is BrowserSwitchStartResult.Failure -> {
                val error = PayPalWebCheckoutError.browserSwitchError(startResult.error)
                PayPalPresentAuthChallengeResult.Failure(error)
            }
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

    fun completeCheckoutAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalWebCheckoutFinishStartResult {
        return when (val finalResult = browserSwitchClient.completeRequest(intent, authState)) {
            is BrowserSwitchFinalResult.Success -> parseWebCheckoutSuccessResult(finalResult)
            is BrowserSwitchFinalResult.Failure -> {
                // TODO: remove error codes and error description from project; the built in
                // Throwable type already has a message property and error codes are only required
                // for iOS Error protocol conformance
                val message = "Browser switch failed"
                val browserSwitchError = PayPalSDKError(0, message, reason = finalResult.error)
                PayPalWebCheckoutFinishStartResult.Failure(browserSwitchError, null)
            }

            BrowserSwitchFinalResult.NoResult -> PayPalWebCheckoutFinishStartResult.NoResult
        }
    }

    fun completeVaultAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalWebCheckoutFinishVaultResult {
        return when (val finalResult = browserSwitchClient.completeRequest(intent, authState)) {
            is BrowserSwitchFinalResult.Success -> parseVaultSuccessResult(finalResult)
            is BrowserSwitchFinalResult.Failure -> {
                // TODO: remove error codes and error description from project; the built in
                // Throwable type already has a message property and error codes are only required
                // for iOS Error protocol conformance
                val message = "Browser switch failed"
                val browserSwitchError = PayPalSDKError(0, message, reason = finalResult.error)
                PayPalWebCheckoutFinishVaultResult.Failure(browserSwitchError)
            }

            BrowserSwitchFinalResult.NoResult -> PayPalWebCheckoutFinishVaultResult.NoResult
        }
    }

    private fun parseWebCheckoutSuccessResult(
        finalResult: BrowserSwitchFinalResult.Success
    ): PayPalWebCheckoutFinishStartResult {
        val deepLinkUrl = finalResult.returnUrl
        val metadata = finalResult.requestMetadata
        return if (finalResult.requestCode == BrowserSwitchRequestCodes.PAYPAL_CHECKOUT) {
            if (metadata == null) {
                val unknownError = PayPalWebCheckoutError.unknownError
                PayPalWebCheckoutFinishStartResult.Failure(unknownError, null)
            } else {
                val payerId = deepLinkUrl.getQueryParameter("PayerID")
                val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
                if (orderId.isNullOrBlank() || payerId.isNullOrBlank()) {
                    val malformedResultError = PayPalWebCheckoutError.malformedResultError
                    PayPalWebCheckoutFinishStartResult.Failure(malformedResultError, orderId)
                } else {
                    PayPalWebCheckoutFinishStartResult.Success(orderId, payerId)
                }
            }
        } else {
            PayPalWebCheckoutFinishStartResult.NoResult
        }
    }

    private fun parseVaultSuccessResult(
        finalResult: BrowserSwitchFinalResult.Success
    ): PayPalWebCheckoutFinishVaultResult {
        val deepLinkUrl = finalResult.returnUrl
        val requestMetadata = finalResult.requestMetadata
        return if (finalResult.requestCode == BrowserSwitchRequestCodes.PAYPAL_VAULT) {
            if (requestMetadata == null) {
                PayPalWebCheckoutFinishVaultResult.Failure(PayPalWebCheckoutError.unknownError)
            } else {
                val approvalSessionId = deepLinkUrl.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
                if (approvalSessionId.isNullOrEmpty()) {
                    PayPalWebCheckoutFinishVaultResult.Failure(PayPalWebCheckoutError.malformedResultError)
                } else {
                    PayPalWebCheckoutFinishVaultResult.Success(approvalSessionId)
                }
            }
        } else {
            PayPalWebCheckoutFinishVaultResult.NoResult
        }
    }
}
