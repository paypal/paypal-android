package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CaptureDeepLinkResult
import com.paypal.android.corepayments.DeepLink
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import com.paypal.android.corepayments.captureDeepLink
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import org.json.JSONObject

// TODO: consider renaming PayPalWebLauncher to PayPalAuthChallengeLauncher
internal class PayPalWebLauncher(
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
) {

    companion object {
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"

        private const val URL_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }

    fun launchWithUrl(
        activity: Activity,
        uri: Uri,
        token: String,
        tokenType: TokenType,
        returnUrlScheme: String? = null,
        appLinkUrl: String? = null
    ): PayPalPresentAuthChallengeResult {
        val metadata = getMetadata(token, tokenType)
        val options = BrowserSwitchOptions(
            targetUri = uri,
            requestCode = getRequestCode(tokenType),
            returnUrlScheme = returnUrlScheme,
            appLinkUrl = appLinkUrl,
            metadata = metadata
        )
        return launchBrowserSwitch(activity, options)
    }


    fun launchWithUrl(
        uri: Uri,
        token: String,
        tokenType: TokenType,
        activityResultLauncher: ActivityResultLauncher<Intent>,
        returnUrlScheme: String?,
        appLinkUrl: String?
    ): PayPalPresentAuthChallengeResult {
        val metadata = getMetadata(token, tokenType)
        val options = BrowserSwitchOptions(
            targetUri = uri,
            requestCode = getRequestCode(tokenType),
            returnUrlScheme = returnUrlScheme,
            appLinkUrl = appLinkUrl,
            metadata = metadata
        )

        val result = browserSwitchClient.start(activityResultLauncher, options)

        return when (result) {
            is BrowserSwitchStartResult.Success -> {
                val pendingState = BrowserSwitchPendingState(options)
                PayPalPresentAuthChallengeResult.Success(pendingState.toBase64EncodedJSON())
            }

            is BrowserSwitchStartResult.Failure -> {
                val error = PayPalWebCheckoutError.browserSwitchError(result.error)
                PayPalPresentAuthChallengeResult.Failure(error)
            }
        }
    }

    private fun getRequestCode(tokenType: TokenType): Int {
        return when (tokenType) {
            TokenType.ORDER_ID -> BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
            TokenType.VAULT_ID -> BrowserSwitchRequestCodes.PAYPAL_VAULT
            TokenType.BILLING_TOKEN -> BrowserSwitchRequestCodes.PAYPAL_VAULT
        }
    }

    private fun getMetadata(
        token: String,
        tokenType: TokenType
    ) = JSONObject().apply {
        when (tokenType) {
            TokenType.ORDER_ID -> put(METADATA_KEY_ORDER_ID, token)
            TokenType.VAULT_ID -> put(METADATA_KEY_SETUP_TOKEN_ID, token)
            TokenType.BILLING_TOKEN -> put(METADATA_KEY_SETUP_TOKEN_ID, token)
        }
    }

    private fun launchBrowserSwitch(
        activity: Activity,
        options: BrowserSwitchOptions
    ): PayPalPresentAuthChallengeResult =
        when (val startResult = browserSwitchClient.start(activity, options)) {
            is BrowserSwitchStartResult.Success -> {
                val pendingState = BrowserSwitchPendingState(options)
                PayPalPresentAuthChallengeResult.Success(pendingState.toBase64EncodedJSON())
            }

            is BrowserSwitchStartResult.Failure -> {
                val error = PayPalWebCheckoutError.browserSwitchError(startResult.error)
                PayPalPresentAuthChallengeResult.Failure(error)
            }
        }

    fun completeCheckoutAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalWebCheckoutFinishStartResult {
        val requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> parseWebCheckoutSuccessResult(result.deepLink)
            is CaptureDeepLinkResult.Failure ->
                PayPalWebCheckoutFinishStartResult.Failure(result.reason, orderId = null)

            is CaptureDeepLinkResult.Ignore -> PayPalWebCheckoutFinishStartResult.NoResult
        }
    }

    fun completeVaultAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalWebCheckoutFinishVaultResult {
        val requestCode = BrowserSwitchRequestCodes.PAYPAL_VAULT
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> parseVaultSuccessResult(result.deepLink)
            is CaptureDeepLinkResult.Failure ->
                PayPalWebCheckoutFinishVaultResult.Failure(result.reason)

            is CaptureDeepLinkResult.Ignore -> PayPalWebCheckoutFinishVaultResult.NoResult
        }
    }

    private fun parseWebCheckoutSuccessResult(
        deepLink: DeepLink
    ): PayPalWebCheckoutFinishStartResult {
        val metadata = deepLink.originalOptions.metadata
        return if (metadata == null) {
            val unknownError = PayPalWebCheckoutError.unknownError
            PayPalWebCheckoutFinishStartResult.Failure(unknownError, null)
        } else {
            val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
            val opType = deepLink.uri.getQueryParameter("opType")
            if (opType == "cancel") {
                PayPalWebCheckoutFinishStartResult.Canceled(orderId)
            } else {
                val payerId = deepLink.uri.getQueryParameter("PayerID")
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
        deepLink: DeepLink
    ): PayPalWebCheckoutFinishVaultResult {
        val requestMetadata = deepLink.originalOptions.metadata
        return if (requestMetadata == null) {
            PayPalWebCheckoutFinishVaultResult.Failure(PayPalWebCheckoutError.unknownError)
        } else {
            val isCancelUrl = deepLink.uri.path?.contains("cancel") ?: false
            if (isCancelUrl) {
                PayPalWebCheckoutFinishVaultResult.Canceled
            } else {
                val approvalSessionId =
                    deepLink.uri.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
                if (approvalSessionId.isNullOrEmpty()) {
                    PayPalWebCheckoutFinishVaultResult.Failure(PayPalWebCheckoutError.malformedResultError)
                } else {
                    PayPalWebCheckoutFinishVaultResult.Success(approvalSessionId)
                }
            }
        }
    }
}
