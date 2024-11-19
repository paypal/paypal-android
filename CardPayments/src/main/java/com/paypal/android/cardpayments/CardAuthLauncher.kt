package com.paypal.android.cardpayments

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
        private const val REQUEST_TYPE_APPROVE_ORDER = "approve_order"
        private const val REQUEST_TYPE_VAULT = "vault"

        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"
    }

    fun presentAuthChallenge(
        activity: FragmentActivity,
        authChallenge: CardAuthChallenge
    ): PayPalSDKError? {
        val metadata = when (authChallenge) {
            is CardAuthChallenge.ApproveOrder -> {
                val request = authChallenge.request
                JSONObject()
                    .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_APPROVE_ORDER)
                    .put(METADATA_KEY_ORDER_ID, request.orderId)
            }

            is CardAuthChallenge.Vault -> {
                val request = authChallenge.request
                JSONObject()
                    .put(METADATA_KEY_REQUEST_TYPE, REQUEST_TYPE_VAULT)
                    .put(METADATA_KEY_SETUP_TOKEN_ID, request.setupTokenId)
            }
        }

        // launch the 3DS flow
        val browserSwitchOptions = BrowserSwitchOptions()
            .url(authChallenge.url)
            .returnUrlScheme(authChallenge.returnUrlScheme)
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
                // Assume REQUEST_TYPE_APPROVE_ORDER
                parseApproveOrderResult(browserSwitchResult)
            }
        }

    private fun parseVaultResult(browserSwitchResult: BrowserSwitchResult): CardStatus? {
        val setupTokenId =
            browserSwitchResult.requestMetadata?.optString(METADATA_KEY_SETUP_TOKEN_ID)
        return when (browserSwitchResult.status) {
            BrowserSwitchStatus.SUCCESS -> parseVaultSuccessResult(browserSwitchResult)
            BrowserSwitchStatus.CANCELED -> CardStatus.VaultCanceled(setupTokenId)
            else -> null
        }
    }

    private fun parseApproveOrderResult(browserSwitchResult: BrowserSwitchResult): CardStatus? {
        val orderId = browserSwitchResult.requestMetadata?.optString(METADATA_KEY_ORDER_ID)
        return if (orderId == null) {
            CardStatus.ApproveOrderError(CardError.unknownError, null)
        } else {
            when (browserSwitchResult.status) {
                BrowserSwitchStatus.SUCCESS -> {
                    val result =
                        CardResult(orderId = orderId, didAttemptThreeDSecureAuthentication = true)
                    CardStatus.ApproveOrderSuccess(result)
                }

                BrowserSwitchStatus.CANCELED -> CardStatus.ApproveOrderCanceled(orderId)
                else -> null
            }
        }
    }

    private fun parseVaultSuccessResult(browserSwitchResult: BrowserSwitchResult): CardStatus {
        val requestMetadata = browserSwitchResult.requestMetadata
        val setupTokenId = requestMetadata?.optString(METADATA_KEY_SETUP_TOKEN_ID)
        return if (setupTokenId == null) {
            CardStatus.VaultError(CardError.unknownError)
        } else {
            // TODO: see if there's a way that we can require the merchant to make their
            // return and cancel urls conform to a strict schema
            val result = CardVaultResult(setupTokenId, "SCA_COMPLETE")
            CardStatus.VaultSuccess(result)
        }
    }
}
