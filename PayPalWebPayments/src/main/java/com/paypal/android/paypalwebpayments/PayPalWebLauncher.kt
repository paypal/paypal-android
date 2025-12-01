package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchFinishResult
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
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
        activity: Activity,
        request: PayPalWebCheckoutRequest,
    ): PayPalPresentAuthChallengeResult {
        val metadata = JSONObject()
            .put(METADATA_KEY_ORDER_ID, request.orderId)
        val url = request.run { buildPayPalCheckoutUri(orderId, coreConfig, fundingSource) }
        val options = BrowserSwitchOptions(
            targetUri = url,
            requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT,
            returnUrlScheme = urlScheme,
            metadata = metadata
        )
        return launchBrowserSwitch(activity, options)
    }

    fun launchPayPalWebVault(
        activity: Activity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        val metadata = JSONObject()
            .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
        val url = request.run { buildPayPalVaultUri(request.setupTokenId, coreConfig) }
        val options = BrowserSwitchOptions(
            targetUri = url,
            requestCode = BrowserSwitchRequestCodes.PAYPAL_VAULT,
            returnUrlScheme = urlScheme,
            metadata = metadata
        )
        return launchBrowserSwitch(activity, options)
    }

    private fun launchBrowserSwitch(
        activity: Activity,
        options: BrowserSwitchOptions
    ): PayPalPresentAuthChallengeResult =
        when (val startResult = browserSwitchClient.start(activity, options)) {
            is BrowserSwitchStartResult.Success -> {
                val authState = startResult.pendingState.toBase64EncodedJSON()
                PayPalPresentAuthChallengeResult.Success(authState)
            }

            is BrowserSwitchStartResult.Failure -> {
                val error = PayPalWebCheckoutError.browserSwitchError(startResult.error)
                PayPalPresentAuthChallengeResult.Failure(error)
            }
        }

    public fun buildPayPalCheckoutUri(
        orderId: String?,
        config: CoreConfig,
        funding: PayPalWebCheckoutFundingSource
    ): Uri {
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://www.paypal.com"
            Environment.SANDBOX -> "https://www.sandbox.paypal.com"
        }
        return baseURL.toUri()
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", redirectUriPayPalCheckout)
            .appendQueryParameter("native_xo", "1")
            .appendQueryParameter("fundingSource", funding.value)
            .appendQueryParameter(
                "integration_artifact",
                UpdateClientConfigAPI.Defaults.INTEGRATION_ARTIFACT
            )
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
        return baseURL.toUri()
            .buildUpon()
            .appendQueryParameter("approval_session_id", setupTokenId)
            .build()
    }

    fun completeCheckoutAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalWebCheckoutFinishStartResult {
        val pendingState = BrowserSwitchPendingState.fromBase64(authState)
        return if (pendingState == null) {
            val invalidAuthStateError = PayPalSDKError(0, "Auth State Invalid.")
            PayPalWebCheckoutFinishStartResult.Failure(invalidAuthStateError, null)
        } else {
            val requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
            when (val finalResult = browserSwitchClient.finish(intent, requestCode, pendingState)) {
                is BrowserSwitchFinishResult.Success -> parseWebCheckoutSuccessResult(finalResult)
                is BrowserSwitchFinishResult.DeepLinkNotPresent,
                is BrowserSwitchFinishResult.DeepLinkDoesNotMatch,
                is BrowserSwitchFinishResult.RequestCodeDoesNotMatch -> PayPalWebCheckoutFinishStartResult.NoResult
            }
        }
    }

    fun completeVaultAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalWebCheckoutFinishVaultResult {
        val pendingState = BrowserSwitchPendingState.fromBase64(authState)
        return if (pendingState == null) {
            val invalidAuthStateError = PayPalSDKError(0, "Auth State Invalid.")
            PayPalWebCheckoutFinishVaultResult.Failure(invalidAuthStateError)
        } else {
            val requestCode = BrowserSwitchRequestCodes.PAYPAL_VAULT
            when (val finalResult = browserSwitchClient.finish(intent, requestCode, pendingState)) {
                is BrowserSwitchFinishResult.Success -> parseVaultSuccessResult(finalResult)
                is BrowserSwitchFinishResult.DeepLinkNotPresent,
                is BrowserSwitchFinishResult.DeepLinkDoesNotMatch,
                is BrowserSwitchFinishResult.RequestCodeDoesNotMatch -> PayPalWebCheckoutFinishVaultResult.NoResult
            }
        }
    }

    private fun parseWebCheckoutSuccessResult(
        finalResult: BrowserSwitchFinishResult.Success
    ): PayPalWebCheckoutFinishStartResult {
        if (finalResult.requestCode != BrowserSwitchRequestCodes.PAYPAL_CHECKOUT) {
            return PayPalWebCheckoutFinishStartResult.NoResult
        }

        val deepLinkUrl = finalResult.returnUrl
        val metadata = finalResult.requestMetadata
        return if (metadata == null) {
            val unknownError = PayPalWebCheckoutError.unknownError
            PayPalWebCheckoutFinishStartResult.Failure(unknownError, null)
        } else {
            val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
            val opType = deepLinkUrl.getQueryParameter("opType")
            if (opType == "cancel") {
                PayPalWebCheckoutFinishStartResult.Canceled(orderId)
            } else {
                val payerId = deepLinkUrl.getQueryParameter("PayerID")
                if (orderId.isNullOrBlank() || payerId.isNullOrBlank()) {
                    val malformedResultError = PayPalWebCheckoutError.malformedResultError
                    PayPalWebCheckoutFinishStartResult.Failure(malformedResultError, orderId)
                } else {
                    PayPalWebCheckoutFinishStartResult.Success(orderId, payerId)
                }
            }
        }
    }

    private fun parseVaultSuccessResult(
        finalResult: BrowserSwitchFinishResult.Success
    ): PayPalWebCheckoutFinishVaultResult {
        if (finalResult.requestCode != BrowserSwitchRequestCodes.PAYPAL_VAULT) {
            return PayPalWebCheckoutFinishVaultResult.NoResult
        }

        val deepLinkUrl = finalResult.returnUrl
        val requestMetadata = finalResult.requestMetadata
        return if (requestMetadata == null) {
            PayPalWebCheckoutFinishVaultResult.Failure(PayPalWebCheckoutError.unknownError)
        } else {
            val isCancelUrl = deepLinkUrl.path?.contains("cancel") ?: false
            if (isCancelUrl) {
                PayPalWebCheckoutFinishVaultResult.Canceled
            } else {
                val approvalSessionId =
                    deepLinkUrl.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
                if (approvalSessionId.isNullOrEmpty()) {
                    PayPalWebCheckoutFinishVaultResult.Failure(PayPalWebCheckoutError.malformedResultError)
                } else {
                    PayPalWebCheckoutFinishVaultResult.Success(approvalSessionId)
                }
            }
        }
    }
}
