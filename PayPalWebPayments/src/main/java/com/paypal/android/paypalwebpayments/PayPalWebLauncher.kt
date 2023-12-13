package com.paypal.android.paypalwebpayments

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchStatus
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient.Companion.VAULT_CANCEL_URL_PATH
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient.Companion.VAULT_DOMAIN
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import org.json.JSONObject

internal class PayPalWebLauncher(
    private val urlScheme: String,
    private val coreConfig: CoreConfig,
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
) {
    private val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"

    companion object {
        const val METADATA_KEY_ORDER_ID = "order_id"
        const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"
    }

    fun launchPayPalWebCheckout(
        activity: FragmentActivity,
        request: PayPalWebCheckoutRequest,
    ): PayPalSDKError? {
        var error: PayPalSDKError? = null
        try {
            val browserSwitchOptions = request.run {
                configurePayPalBrowserSwitchOptions(orderId, coreConfig, fundingSource)
            }
            browserSwitchClient.start(activity, browserSwitchOptions)
        } catch (e: PayPalSDKError) {
            error = APIClientError.clientIDNotFoundError(e.code, e.correlationId)
        }
        return error
    }

    fun launchPayPalWebVault(activity: FragmentActivity, vaultRequest: PayPalWebVaultRequest) {
        val browserSwitchOptions = vaultRequest.run {
            configurePayPalVaultApproveSwitchOptions(setupTokenId, approveVaultHref)
        }
        browserSwitchClient.start(activity, browserSwitchOptions)
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

    private fun configurePayPalBrowserSwitchOptions(
        orderId: String?,
        config: CoreConfig,
        funding: PayPalWebCheckoutFundingSource
    ): BrowserSwitchOptions {
        val metadata = JSONObject().put(METADATA_KEY_ORDER_ID, orderId)
        return BrowserSwitchOptions()
            .url(buildPayPalCheckoutUri(orderId, config, funding))
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
    }

    private fun configurePayPalVaultApproveSwitchOptions(
        setupTokenId: String?,
        approveOrderHref: String
    ): BrowserSwitchOptions {
        val metadata = JSONObject()
            .put(METADATA_KEY_SETUP_TOKEN_ID, setupTokenId)
        return BrowserSwitchOptions()
            .url(Uri.parse(approveOrderHref))
            .returnUrlScheme(urlScheme)
            .metadata(metadata)
    }

    fun deliverBrowserSwitchResult(activity: FragmentActivity) =
        browserSwitchClient.deliverResult(activity)?.let { browserSwitchResult ->
            val isVaultResult =
                browserSwitchResult.deepLinkUrl?.path?.contains(VAULT_DOMAIN) ?: false
            if (isVaultResult) {
                parseWebCheckoutResult(browserSwitchResult)
            }
            parseVaultResult(browserSwitchResult)
        }

    private fun parseWebCheckoutResult(browserSwitchResult: BrowserSwitchResult) =
        when (browserSwitchResult.status) {
            BrowserSwitchStatus.SUCCESS -> {
                val deepLinkUrl = browserSwitchResult.deepLinkUrl
                val requestMetadata = browserSwitchResult.requestMetadata
                if (deepLinkUrl != null && requestMetadata != null) {
                    val deepLink = PayPalWebCheckoutDeepLink(deepLinkUrl, requestMetadata)
                    if (deepLink.isValid) {
                        val result =
                            deepLink.run { PayPalWebCheckoutResult(orderId, payerId) }
                        PayPalWebStatus.CheckoutSuccess(result)
                    } else {
                        PayPalWebStatus.CheckoutError(PayPalWebCheckoutError.malformedResultError)
                    }
                } else {
                    PayPalWebStatus.CheckoutError(PayPalWebCheckoutError.unknownError)
                }
            }

            BrowserSwitchStatus.CANCELED -> {
                val orderId =
                    browserSwitchResult.requestMetadata?.getString(METADATA_KEY_ORDER_ID)
                PayPalWebStatus.CheckoutCanceled(orderId)
            }

            else -> null
        }

    private fun parseVaultResult(browserSwitchResult: BrowserSwitchResult) =
        when (browserSwitchResult.status) {
            BrowserSwitchStatus.SUCCESS -> {
                val deepLinkUrl = browserSwitchResult.deepLinkUrl
                val requestMetadata = browserSwitchResult.requestMetadata

                if (deepLinkUrl == null || requestMetadata == null) {
                    PayPalWebStatus.VaultError(PayPalWebCheckoutError.malformedResultError)
                } else {
                    val isFailure = deepLinkUrl.path?.contains(VAULT_CANCEL_URL_PATH) ?: false
                    if (isFailure) {
                        PayPalWebStatus.VaultError(PayPalWebCheckoutError.malformedResultError)
                    } else {
                        val deepLink = PayPalVaultDeepLink(deepLinkUrl)
                        if (deepLink.isValid) {
                            val result = PayPalWebCheckoutVaultResult(
                                deepLink.approvalTokenId,
                                deepLink.approvalSessionId
                            )
                            PayPalWebStatus.VaultSuccess(result)
                        } else {
                            PayPalWebStatus.VaultError(PayPalWebCheckoutError.malformedResultError)
                        }
                    }
                }
            }

            BrowserSwitchStatus.CANCELED -> {
                PayPalWebStatus.VaultCanceled
            }

            else -> null
        }
}
