package com.paypal.android.paypalpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CaptureDeepLinkResult
import com.paypal.android.corepayments.DeepLink
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import com.paypal.android.corepayments.captureDeepLink
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalpayments.errors.PayPalCheckoutError
import org.json.JSONObject

// TODO: consider renaming PayPalLauncher to PayPalAuthChallengeLauncher
internal class PayPalLauncher(
    private val browserSwitchClient: BrowserSwitchClient
) {

    constructor(context: Context) : this(BrowserSwitchClient(context))

    companion object {
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"

        private const val URL_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }

    fun launchWithUrl(
        context: Context,
        uri: Uri,
        token: String,
        tokenType: TokenType,
        returnToAppStrategy: ReturnToAppStrategy,
    ): PayPalPresentAuthChallengeResult {
        val metadata = getMetadata(token, tokenType)
        val options = BrowserSwitchOptions(
            targetUri = uri,
            requestCode = getRequestCode(tokenType),
            returnUrlScheme = (returnToAppStrategy as? ReturnToAppStrategy.CustomUrlScheme)?.urlScheme,
            appLinkUrl = (returnToAppStrategy as? ReturnToAppStrategy.AppLink)?.appLinkUrl,
            metadata = metadata
        )
        return launchBrowserSwitch(context, options)
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
        context: Context,
        options: BrowserSwitchOptions
    ): PayPalPresentAuthChallengeResult =
        when (val startResult = browserSwitchClient.start(context, options)) {
            is BrowserSwitchStartResult.Success -> {
                val pendingState = BrowserSwitchPendingState(options)
                PayPalPresentAuthChallengeResult.Success(pendingState.toBase64EncodedJSON())
            }

            is BrowserSwitchStartResult.Failure -> {
                val error = PayPalCheckoutError.browserSwitchError(startResult.error)
                PayPalPresentAuthChallengeResult.Failure(error)
            }
        }

    fun completeCheckoutAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalCheckoutFinishStartResult {
        val requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> parseWebCheckoutSuccessResult(result.deepLink)
            is CaptureDeepLinkResult.Failure ->
                PayPalCheckoutFinishStartResult.Failure(result.reason, orderId = null)

            is CaptureDeepLinkResult.Ignore -> PayPalCheckoutFinishStartResult.NoResult
        }
    }

    fun completeVaultAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalCheckoutFinishVaultResult {
        val requestCode = BrowserSwitchRequestCodes.PAYPAL_VAULT
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> parseVaultSuccessResult(result.deepLink)
            is CaptureDeepLinkResult.Failure ->
                PayPalCheckoutFinishVaultResult.Failure(result.reason)

            is CaptureDeepLinkResult.Ignore -> PayPalCheckoutFinishVaultResult.NoResult
        }
    }

    private fun parseWebCheckoutSuccessResult(
        deepLink: DeepLink
    ): PayPalCheckoutFinishStartResult {
        val metadata = deepLink.originalOptions.metadata
        return if (metadata == null) {
            val unknownError = PayPalCheckoutError.unknownError
            PayPalCheckoutFinishStartResult.Failure(unknownError, null)
        } else {
            val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
            val payerId = deepLink.uri.getQueryParameter("PayerID")
            val isCancelUrl = deepLink.uri.path?.contains("cancel") ?: false
            if (isCancelUrl) {
                PayPalCheckoutFinishStartResult.Canceled(orderId)
            } else {
                when {
                    !orderId.isNullOrBlank() && !payerId.isNullOrBlank() ->
                        PayPalCheckoutFinishStartResult.Success(orderId, payerId)
                    orderId.isNullOrBlank() ->
                        PayPalCheckoutFinishStartResult.Failure(
                            PayPalCheckoutError.malformedResultError,
                            orderId
                        )
                    else -> PayPalCheckoutFinishStartResult.Canceled(orderId)
                }
            }
        }
    }

    private fun parseVaultSuccessResult(
        deepLink: DeepLink
    ): PayPalCheckoutFinishVaultResult {
        val requestMetadata = deepLink.originalOptions.metadata
        return if (requestMetadata == null) {
            PayPalCheckoutFinishVaultResult.Failure(PayPalCheckoutError.unknownError)
        } else {
            val isCancelUrl = deepLink.uri.path?.contains("cancel") ?: false
            if (isCancelUrl) {
                PayPalCheckoutFinishVaultResult.Canceled
            } else {
                val approvalSessionId =
                    deepLink.uri.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
                if (approvalSessionId.isNullOrEmpty()) {
                    PayPalCheckoutFinishVaultResult.Failure(PayPalCheckoutError.malformedResultError)
                } else {
                    PayPalCheckoutFinishVaultResult.Success(approvalSessionId)
                }
            }
        }
    }
}
