package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchFinishResult
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
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
        val pendingState = BrowserSwitchPendingState.fromBase64(authState)
        return if (pendingState == null) {
            val invalidAuthStateError = PayPalSDKError(0, "Auth State Invalid.")
            PayPalWebCheckoutFinishStartResult.Failure(invalidAuthStateError, null)
        } else {
            val requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
            when (val finalResult = pendingState.match(intent, requestCode)) {
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
            when (val finalResult = pendingState.match(intent, requestCode)) {
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
