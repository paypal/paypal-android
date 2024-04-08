package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchParseResult
import com.braintreepayments.api.BrowserSwitchStartResult
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
    ): BrowserSwitchStartResult {
        val metadata = JSONObject()
            .put(METADATA_KEY_ORDER_ID, request.orderId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_CHECKOUT)
        val url = request.run { buildPayPalCheckoutUri(orderId, coreConfig, fundingSource) }
        val browserSwitchOptions = BrowserSwitchOptions()
            .url(url)
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
        return browserSwitchClient.start(activity, browserSwitchOptions)
    }

    fun launchPayPalWebVault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ): BrowserSwitchStartResult {
        val metadata = JSONObject()
            .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
        val browserSwitchOptions = BrowserSwitchOptions()
            .url(Uri.parse(request.approveVaultHref))
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
        return browserSwitchClient.start(activity, browserSwitchOptions)
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

    fun parseBrowserSwitchResult(intent: Intent, pendingRequestState: String) =
        when (val result = browserSwitchClient.parseResult(intent, pendingRequestState)) {
            BrowserSwitchParseResult.NoResult -> {/* do nothing for now */
                null
            }

            is BrowserSwitchParseResult.Failure -> {/* do nothing for now */
                null
            }

            is BrowserSwitchParseResult.Success -> {
                val requestType = result.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
                if (requestType == REQUEST_TYPE_VAULT) {
                    parseVaultResult(result)
                } else {
                    parseWebCheckoutResult(result)
                }
            }
        }

//    fun deliverBrowserSwitchResult(activity: ComponentActivity) =
//        browserSwitchClient.deliverResult(activity)?.let { browserSwitchResult ->
//            val requestType =
//                browserSwitchResult.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
//            if (requestType == REQUEST_TYPE_VAULT) {
//                parseVaultResult(browserSwitchResult)
//            } else {
//                parseWebCheckoutResult(browserSwitchResult)
//            }
//        }

    private fun parseWebCheckoutResult(browserSwitchResult: BrowserSwitchParseResult.Success): PayPalWebStatus {
        val deepLinkUrl = browserSwitchResult.deepLinkUrl
        val metadata = browserSwitchResult.requestMetadata

        return if (deepLinkUrl == null || metadata == null) {
            val orderId = metadata?.optString(METADATA_KEY_ORDER_ID)
            PayPalWebStatus.CheckoutError(PayPalWebCheckoutError.unknownError, orderId)
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


//        when (browserSwitchResult.status) {
//            BrowserSwitchStatus.SUCCESS -> parseWebCheckoutSuccessResult(browserSwitchResult)
//            BrowserSwitchStatus.CANCELED -> {
//                val orderId =
//                    browserSwitchResult.requestMetadata?.optString(METADATA_KEY_ORDER_ID)
//                PayPalWebStatus.CheckoutCanceled(orderId)
//            }
//
//            else -> null
//        }

//    private fun parseWebCheckoutSuccessResult(browserSwitchResult: BrowserSwitchResult): PayPalWebStatus {
//        val deepLinkUrl = browserSwitchResult.deepLinkUrl
//        val metadata = browserSwitchResult.requestMetadata
//
//        return if (deepLinkUrl == null || metadata == null) {
//            val orderId = metadata?.optString(METADATA_KEY_ORDER_ID)
//            PayPalWebStatus.CheckoutError(PayPalWebCheckoutError.unknownError, orderId)
//        } else {
//            val payerId = deepLinkUrl.getQueryParameter("PayerID")
//            val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
//            if (orderId.isNullOrBlank() || payerId.isNullOrBlank()) {
//                PayPalWebStatus.CheckoutError(PayPalWebCheckoutError.malformedResultError, orderId)
//            } else {
//                PayPalWebStatus.CheckoutSuccess(PayPalWebCheckoutResult(orderId, payerId))
//            }
//        }
//    }

    private fun parseVaultResult(browserSwitchResult: BrowserSwitchParseResult.Success): PayPalWebStatus {
        val deepLinkUrl = browserSwitchResult.deepLinkUrl
        val requestMetadata = browserSwitchResult.requestMetadata

        return if (deepLinkUrl == null || requestMetadata == null) {
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
//        when (browserSwitchResult.status) {
//            BrowserSwitchStatus.SUCCESS -> parseVaultSuccessResult(browserSwitchResult)
//            BrowserSwitchStatus.CANCELED -> PayPalWebStatus.VaultCanceled
//            else -> null
//        }

//    private fun parseVaultSuccessResult(browserSwitchResult: BrowserSwitchResult): PayPalWebStatus {
//        val deepLinkUrl = browserSwitchResult.deepLinkUrl
//        val requestMetadata = browserSwitchResult.requestMetadata
//
//        return if (deepLinkUrl == null || requestMetadata == null) {
//            PayPalWebStatus.VaultError(PayPalWebCheckoutError.unknownError)
//        } else {
//            val approvalSessionId = deepLinkUrl.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
//            if (approvalSessionId.isNullOrEmpty()) {
//                PayPalWebStatus.VaultError(PayPalWebCheckoutError.malformedResultError)
//            } else {
//                val result = PayPalWebVaultResult(approvalSessionId)
//                PayPalWebStatus.VaultSuccess(result)
//            }
//        }
//    }
}
