package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.UpdateClientConfigAPI
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
        returnUrlScheme: String
    ): PayPalPresentAuthChallengeResult {
        val metadata = getMetadata(token, tokenType)
        val options = BrowserSwitchOptions()
            .url(uri)
            .requestCode(getRequestCode(tokenType))
            .returnUrlScheme(returnUrlScheme)
            .metadata(metadata)
        return launchBrowserSwitch(activity, options)
    }

    private fun getRequestCode(tokenType: TokenType): Int {
        return when (tokenType) {
            TokenType.ORDER_ID -> BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
            TokenType.VAULT_ID -> BrowserSwitchRequestCodes.PAYPAL_VAULT
            TokenType.CHECKOUT_TOKEN -> BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
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
            TokenType.CHECKOUT_TOKEN -> put(METADATA_KEY_ORDER_ID, token)
            TokenType.BILLING_TOKEN -> put(METADATA_KEY_SETUP_TOKEN_ID, token)
        }
    }

    private fun launchBrowserSwitch(
        activity: Activity,
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
        finalResult: BrowserSwitchFinalResult.Success
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
