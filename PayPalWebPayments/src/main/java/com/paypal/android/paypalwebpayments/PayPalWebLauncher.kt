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
        private const val METADATA_KEY_REQUEST_TYPE = "request_type"
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"

        private const val REQUEST_TYPE_CHECKOUT = "checkout"
        private const val REQUEST_TYPE_VAULT = "vault"

        private const val URL_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }

    fun launchPayPalWebCheckout(
        activity: ComponentActivity,
        request: PayPalWebCheckoutRequest,
    ): PayPalPresentAuthChallengeResult {
        val metadata = JSONObject()
            .put(METADATA_KEY_ORDER_ID, request.orderId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_CHECKOUT)
        val url = request.run { buildPayPalCheckoutUri(orderId, coreConfig, fundingSource) }
        val options = BrowserSwitchOptions()
            .url(url)
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
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
        val url = request.run { buildPayPalVaultUri(request.setupTokenId, coreConfig) }
        val options = BrowserSwitchOptions()
            .url(url)
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

    fun completeAuthRequest(intent: Intent, authState: String): PayPalWebStatus {
        val requestCode = BrowserSwitchRequestCodes.PAY_PAL
        return when (val finalResult = browserSwitchClient.completeRequest(intent, requestCode, authState)) {
            is BrowserSwitchFinalResult.Success -> {
                val requestType =
                    finalResult.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_VAULT) {
                    parseVaultSuccessResult(finalResult)
                } else {
                    parseWebCheckoutSuccessResult(finalResult)
                }
            }

            is BrowserSwitchFinalResult.Failure -> {
                val error = PayPalWebCheckoutError.browserSwitchError(finalResult.error)
                // TODO: fix this bug; this could also be a vault error but we don't have access
                // to metadata to check
                PayPalWebStatus.CheckoutError(error, null)
            }

            BrowserSwitchFinalResult.NoResult -> PayPalWebStatus.NoResult
        }
    }

    private fun parseWebCheckoutSuccessResult(finalResult: BrowserSwitchFinalResult.Success): PayPalWebStatus {
        val deepLinkUrl = finalResult.returnUrl
        val metadata = finalResult.requestMetadata

        return if (metadata == null) {
            PayPalWebStatus.CheckoutError(PayPalWebCheckoutError.unknownError, null)
        } else {
            val payerId = deepLinkUrl.getQueryParameter("PayerID")
            val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
            if (orderId.isNullOrBlank() || payerId.isNullOrBlank()) {
                PayPalWebStatus.CheckoutError(PayPalWebCheckoutError.malformedResultError, orderId)
            } else {
                PayPalWebStatus.CheckoutSuccess(PayPalWebCheckoutResult(orderId, payerId))
            }
        }
    }

    private fun parseVaultSuccessResult(finalResult: BrowserSwitchFinalResult.Success): PayPalWebStatus {
        val deepLinkUrl = finalResult.returnUrl
        val requestMetadata = finalResult.requestMetadata

        return if (requestMetadata == null) {
            PayPalWebStatus.VaultError(PayPalWebCheckoutError.unknownError)
        } else {
            val approvalSessionId = deepLinkUrl.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
            if (approvalSessionId.isNullOrEmpty()) {
                PayPalWebStatus.VaultError(PayPalWebCheckoutError.malformedResultError)
            } else {
                val result = PayPalWebVaultResult(approvalSessionId)
                PayPalWebStatus.VaultSuccess(result)
            }
        }
    }
}
