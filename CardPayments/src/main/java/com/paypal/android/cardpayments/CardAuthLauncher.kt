package com.paypal.android.cardpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.corepayments.PayPalSDKError
import org.json.JSONObject

internal class CardAuthLauncher(
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

    fun presentAuthChallenge(
        activity: FragmentActivity,
        authChallenge: CardAuthChallenge
    ): PayPalSDKError? {
        // preform 3DS browser switch
        val vaultRequest = authChallenge.request
        val returnUrlScheme = vaultRequest.run { Uri.parse(returnUrl).scheme!! }
        val metadata = JSONObject()
            .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
            .put(METADATA_KEY_SETUP_TOKEN_ID, vaultRequest.setupTokenId)

        val options = BrowserSwitchOptions()
            .url(authChallenge.url)
            .returnUrlScheme(returnUrlScheme)
            .metadata(metadata)
        return launchBrowserSwitch(activity, options)
    }

    private fun launchBrowserSwitch(
        activity: FragmentActivity,
        options: BrowserSwitchOptions
    ): PayPalSDKError? {
        var error: PayPalSDKError? = null
        try {
            browserSwitchClient.start(activity, options)
        } catch (e: BrowserSwitchException) {
            error = CardError.browserSwitchError(e)
        }
        return error
    }

    fun deliverBrowserSwitchResult(activity: FragmentActivity) =
        browserSwitchClient.deliverResult(activity)?.let { browserSwitchResult ->
            val requestType =
                browserSwitchResult.requestMetadata?.optString(METADATA_KEY_REQUEST_TYPE)
            if (requestType == REQUEST_TYPE_VAULT) {
                parseVaultResult(browserSwitchResult)
            } else {
                // TODO: migrate approve order to use auth challenge launcher pattern internally
//                parseWebCheckoutResult(browserSwitchResult)
            }
        }

    private fun parseVaultResult(browserSwitchResult: BrowserSwitchResult) =
        when (browserSwitchResult.status) {
            BrowserSwitchStatus.SUCCESS -> parseVaultSuccessResult(browserSwitchResult)
            BrowserSwitchStatus.CANCELED -> CardStatus.VaultCanceled
            else -> null
        }

    private fun parseVaultSuccessResult(browserSwitchResult: BrowserSwitchResult): CardStatus {
        val deepLinkUrl = browserSwitchResult.deepLinkUrl
        val requestMetadata = browserSwitchResult.requestMetadata

        return if (deepLinkUrl == null || requestMetadata == null) {
            CardStatus.VaultError(CardError.unknownError)
        } else {
            // TODO: see if there's a way that we can require the merchant to make their
            // return and cancel urls conform to a strict schema

            // NOTE: this assumes that when the merchant created a setup token, they used a
            // return_url with word "success" in it (or a cancel_url with the word "cancel" in it)
            val deepLinkUrlString = deepLinkUrl.toString()
            val didSucceed = deepLinkUrlString.contains("success")
            if (didSucceed) {
                val setupTokenId = browserSwitchResult.requestMetadata?.getString(
                    METADATA_KEY_SETUP_TOKEN_ID
                )
                val result = CardVaultResult(setupTokenId!!, "SCA_COMPLETE")
                CardStatus.VaultSuccess(result)
            } else {
                val didCancel = deepLinkUrlString.contains("cancel")
                if (didCancel) {
                    CardStatus.VaultCanceled
                } else {
                    CardStatus.VaultError(CardError.unknownError)
                }
            }
        }
    }
}
