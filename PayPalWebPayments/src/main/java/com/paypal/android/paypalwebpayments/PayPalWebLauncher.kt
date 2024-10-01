package com.paypal.android.paypalwebpayments

import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.browserswitch.BrowserSwitchRequestCode
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchLaunchResult
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStatus
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import org.json.JSONObject


// TODO: consider renaming PayPalWebLauncher to PayPalAuthChallengeLauncher
internal class PayPalWebLauncher(
    private val analytics: PayPalAnalytics,
    private val browserSwitchClient: BrowserSwitchClient = BrowserSwitchClient(),
) {

    fun createAuthChallenge(
        request: PayPalWebCheckoutRequest,
        trackingId: String
    ): PayPalAuthChallenge {
        val metadata = request.run {
            PayPalAuthMetadata.Checkout(
                config = config,
                trackingId = trackingId,
                orderId = orderId
            )
        }
        val url = buildPayPalCheckoutUri(request)
        val options = BrowserSwitchOptions(
            code = BrowserSwitchRequestCode.PAYPAL_CHECKOUT,
            urlToOpen = url,
            returnUrl = request.urlScheme,
            metadata = metadata.encodeToString()
        )
        return PayPalAuthChallenge(options)
    }

    fun createAuthChallenge(
        request: PayPalWebVaultRequest,
        trackingId: String
    ): PayPalAuthChallenge {
        val metadata = request.run {
            PayPalAuthMetadata.Vault(
                config = config,
                trackingId = trackingId,
                setupTokenId = setupTokenId
            )
        }
        val url = request.run { buildPayPalVaultUri(request) }
        val options = BrowserSwitchOptions(
            code = BrowserSwitchRequestCode.PAYPAL_VAULT,
            urlToOpen = url,
            returnUrl = request.urlScheme,
            metadata = metadata.encodeToString()
        )
        return PayPalAuthChallenge(options)
    }

    fun presentAuthChallenge(
        activity: FragmentActivity,
        authChallenge: PayPalAuthChallenge,
    ): PayPalAuthChallengeResult {
        val analytics = analytics.restoreFromAuthChallenge(authChallenge)
        val requestCode = authChallenge.options.code

        return when (val result = browserSwitchClient.launch(activity, authChallenge.options)) {
            BrowserSwitchLaunchResult.Success -> {
//                when (requestCode) {
//                    BrowserSwitchRequestCode.CARD_APPROVE_ORDER -> analytics?.notifyConfirmPaymentSourceSCADidLaunch()
//                    BrowserSwitchRequestCode.CARD_VAULT -> analytics?.notifyVaultSCADidLaunch()
//                    else -> {
//                        // do nothing
//                    }
//                }
                PayPalAuthChallengeResult.Success(authChallenge.options.encodeToString())
            }

            is BrowserSwitchLaunchResult.Failure -> {
//                when (requestCode) {
//                    BrowserSwitchRequestCode.CARD_APPROVE_ORDER -> analytics?.notifyConfirmPaymentSourceSCALaunchFailed()
//                    BrowserSwitchRequestCode.CARD_VAULT -> analytics?.notifyVaultSCALaunchFailed()
//                    else -> {
//                        // do nothing
//                    }
//                }
                val error = PayPalSDKError(123, "auth challenge failed", reason = result.error)
                PayPalAuthChallengeResult.Failure(error)
            }
        }
    }

    private fun buildPayPalCheckoutUri(request: PayPalWebCheckoutRequest): String {
        val config = request.config
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://www.paypal.com"
            Environment.SANDBOX -> "https://www.sandbox.paypal.com"
        }
        val urlScheme = request.urlScheme
        val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"
        return Uri.parse(baseURL)
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", request.orderId)
            .appendQueryParameter("redirect_uri", redirectUriPayPalCheckout)
            .appendQueryParameter("native_xo", "1")
            .appendQueryParameter("fundingSource", request.fundingSource.value)
            .build()
            .toString()
    }

    private fun buildPayPalVaultUri(request: PayPalWebVaultRequest): String {
        val config = request.config
        val baseURL = when (config.environment) {
            Environment.LIVE -> "https://paypal.com/agreements/approve"
            Environment.SANDBOX -> "https://sandbox.paypal.com/agreements/approve"
        }
        return Uri.parse(baseURL)
            .buildUpon()
            .appendQueryParameter("approval_session_id", request.setupTokenId)
            .build()
            .toString()
    }

    fun getCheckoutAuthResult(intent: Intent, state: String): PayPalWebCheckoutAuthResult {
        val requestCode = BrowserSwitchRequestCode.PAYPAL_CHECKOUT
        val options = BrowserSwitchOptions.decodeIfRequestCodeMatches(state, requestCode)
            ?: return PayPalWebCheckoutAuthResult.NoResult

        return when (val metadata = PayPalAuthMetadata.decodeFromString(options.metadata)) {
            is PayPalAuthMetadata.Checkout -> {
                when (val status = browserSwitchClient.parseStatus(intent, options)) {
                    is BrowserSwitchStatus.Complete ->
                        parseCheckoutAuthResultFromDeepLink(status, metadata)

                    else -> PayPalWebCheckoutAuthResult.NoResult
                }
            }

            else -> PayPalWebCheckoutAuthResult.NoResult
        }
    }

    private fun parseCheckoutAuthResultFromDeepLink(
        status: BrowserSwitchStatus.Complete,
        metadata: PayPalAuthMetadata.Checkout
    ): PayPalWebCheckoutAuthResult {
        val deepLinkUrl = status.deepLinkUri

        // TODO: check for canceled status
        val payerId = deepLinkUrl.getQueryParameter("PayerID")
        val orderId = metadata.orderId
        return if (orderId.isBlank() || payerId.isNullOrBlank()) {
            val error = PayPalWebCheckoutError.malformedResultError
            PayPalWebCheckoutAuthResult.Failure(error, orderId)
        } else {
            PayPalWebCheckoutAuthResult.Success(orderId, payerId)
        }
    }

    fun getVaultAuthResult(intent: Intent, state: String): PayPalWebVaultAuthResult {
        val requestCode = BrowserSwitchRequestCode.PAYPAL_VAULT
        val options = BrowserSwitchOptions.decodeIfRequestCodeMatches(state, requestCode)
            ?: return PayPalWebVaultAuthResult.NoResult

        return when (val metadata = PayPalAuthMetadata.decodeFromString(options.metadata)) {
            is PayPalAuthMetadata.Vault -> {
                when (val status = browserSwitchClient.parseStatus(intent, options)) {
                    is BrowserSwitchStatus.Complete ->
                        parseVaultAuthResultFromDeepLink(status, metadata)

                    else -> PayPalWebVaultAuthResult.NoResult
                }
            }

            else -> PayPalWebVaultAuthResult.NoResult
        }
    }

    private fun parseVaultAuthResultFromDeepLink(
        status: BrowserSwitchStatus.Complete,
        metadata: PayPalAuthMetadata.Vault
    ): PayPalWebVaultAuthResult {
        val deepLinkUrl = status.deepLinkUri
        return if (metadata == null) {
            PayPalWebVaultAuthResult.Failure(PayPalWebCheckoutError.unknownError)
        } else {
            val approvalSessionId = deepLinkUrl.getQueryParameter("approval_session_id")
            if (approvalSessionId.isNullOrEmpty()) {
                PayPalWebVaultAuthResult.Failure(PayPalWebCheckoutError.malformedResultError)
            } else {
                PayPalWebVaultAuthResult.Success(approvalSessionId)
            }
        }
    }
}
