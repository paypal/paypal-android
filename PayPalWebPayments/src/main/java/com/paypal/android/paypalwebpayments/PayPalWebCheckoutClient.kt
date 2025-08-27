package com.paypal.android.paypalwebpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
class PayPalWebCheckoutClient internal constructor(
    private val analytics: PayPalWebAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher,
    private val patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility,
    private val deviceInspector: DeviceInspector,
    private val coreConfig: CoreConfig,
    private val urlScheme: String,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {

    // for analytics tracking
    private var checkoutOrderId: String? = null
    private var vaultSetupTokenId: String? = null

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param context an Android context
     * @param configuration a [CoreConfig] object
     * @param urlScheme the custom URl scheme used to return to your app from a browser switch flow
     */
    constructor(context: Context, configuration: CoreConfig, urlScheme: String) : this(
        PayPalWebAnalytics(AnalyticsService(context.applicationContext, configuration)),
        PayPalWebLauncher(),
        PatchCCOWithAppSwitchEligibility(configuration),
        DeviceInspector(context),
        configuration,
        urlScheme
    )

    /**
     * Confirm PayPal payment source for an order.
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    suspend fun start(
        activity: ComponentActivity,
        request: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        checkoutOrderId = request.orderId
        analytics.notify(CheckoutEvent.STARTED, checkoutOrderId)

        val launchUri = getLaunchUri(
            context = activity.applicationContext,
            token = request.orderId,
            tokenType = TokenType.ORDER_ID,
            appSwitchWhenEligible = request.appSwitchWhenEligible,
            fallbackUri = buildPayPalCheckoutUri(request.orderId, request.fundingSource)
        )

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.orderId,
            tokenType = TokenType.ORDER_ID,
            returnUrlScheme = urlScheme
        )

        // Track analytics for auth challenge result
        val analyticsEvent = when (result) {
            is PayPalPresentAuthChallengeResult.Success ->
                CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED
            is PayPalPresentAuthChallengeResult.Failure ->
                CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED
        }
        analytics.notify(analyticsEvent, request.orderId)

        return result
    }

    /**
     * Confirm PayPal payment source for an order with callback.
     * Network operations are handled automatically by the Http layer.
     *
     * @param activity the ComponentActivity to launch the auth challenge from
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     * @param callback callback to receive the result
     */
    fun start(
        activity: ComponentActivity,
        request: PayPalWebCheckoutRequest,
        callback: PayPalWebCheckoutCallback
    ) {
        CoroutineScope(mainDispatcher).launch {
            callback.onPayPalWebCheckoutResult(start(activity, request))
        }
    }

    /**
     * Vault PayPal as a payment method.
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    suspend fun vault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        vaultSetupTokenId = request.setupTokenId
        analytics.notify(VaultEvent.STARTED, vaultSetupTokenId)

        val launchUri = getLaunchUri(
            context = activity.applicationContext,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            appSwitchWhenEligible = request.appSwitchWhenEligible,
            fallbackUri = buildPayPalVaultUri(request.setupTokenId)
        )

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnUrlScheme = urlScheme
        )

        // Track analytics for auth challenge result
        val analyticsEvent = when (result) {
            is PayPalPresentAuthChallengeResult.Success ->
                VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED
            is PayPalPresentAuthChallengeResult.Failure ->
                VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED
        }
        analytics.notify(analyticsEvent, vaultSetupTokenId)

        return result
    }

    /**
     * Vault PayPal as a payment method with callback.
     * Network operations are handled automatically by the Http layer.
     *
     * @param activity the ComponentActivity to launch the auth challenge from
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     * @param callback callback to receive the result
     */
    fun vault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest,
        callback: PayPalWebVaultCallback
    ) {
        CoroutineScope(mainDispatcher).launch {
            callback.onPayPalWebVaultResult(vault(activity, request))
        }
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.start]), call this method to see if a user has
     * successfully authorized a PayPal account as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     * @param [authState] A continuation state received from [PayPalPresentAuthChallengeResult.Success]
     * when calling [PayPalWebCheckoutClient.start]. This is needed to properly verify that an
     * authorization completed successfully.
     */
    fun finishStart(intent: Intent, authState: String): PayPalWebCheckoutFinishStartResult {
        val result = payPalWebLauncher.completeCheckoutAuthRequest(intent, authState)
        when (result) {
            is PayPalWebCheckoutFinishStartResult.Success ->
                analytics.notify(CheckoutEvent.SUCCEEDED, checkoutOrderId)

            is PayPalWebCheckoutFinishStartResult.Canceled ->
                analytics.notify(CheckoutEvent.CANCELED, checkoutOrderId)

            is PayPalWebCheckoutFinishStartResult.Failure ->
                analytics.notify(CheckoutEvent.FAILED, checkoutOrderId)

            PayPalWebCheckoutFinishStartResult.NoResult -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.vault]), call this method to see if a user has
     * successfully authorized a PayPal account for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     * @param [authState] A continuation state received from [PayPalPresentAuthChallengeResult.Success]
     * when calling [PayPalWebCheckoutClient.vault]. This is needed to properly verify that an
     * authorization completed successfully.
     */
    fun finishVault(intent: Intent, authState: String): PayPalWebCheckoutFinishVaultResult {
        val result = payPalWebLauncher.completeVaultAuthRequest(intent, authState)
        // TODO: see if we can get setup token id from somewhere for tracking
        when (result) {
            is PayPalWebCheckoutFinishVaultResult.Success ->
                analytics.notify(VaultEvent.SUCCEEDED, vaultSetupTokenId)

            is PayPalWebCheckoutFinishVaultResult.Failure ->
                analytics.notify(VaultEvent.FAILED, vaultSetupTokenId)

            PayPalWebCheckoutFinishVaultResult.Canceled ->
                analytics.notify(VaultEvent.CANCELED, vaultSetupTokenId)

            PayPalWebCheckoutFinishVaultResult.NoResult -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    private val redirectUriPayPalCheckout = "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout"

    private fun buildPayPalCheckoutUri(
        orderId: String?,
        funding: PayPalWebCheckoutFundingSource
    ): Uri {
        val baseURL = when (coreConfig.environment) {
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
            .build()
    }

    private fun buildPayPalVaultUri(
        setupTokenId: String
    ): Uri {
        return getBaseURL().toUri()
            .buildUpon()
            .appendPath("approve")
            .appendQueryParameter("approval_session_id", setupTokenId)
            .build()
    }

    private fun getBaseURL(): String = when (coreConfig.environment) {
        Environment.LIVE -> "https://paypal.com/agreements/"
        Environment.SANDBOX -> "https://sandbox.paypal.com/agreements"
    }

    private suspend fun getLaunchUri(
        context: Context,
        token: String,
        tokenType: TokenType,
        appSwitchWhenEligible: Boolean,
        fallbackUri: Uri
    ): Uri {
        return if (appSwitchWhenEligible && deviceInspector.isPayPalInstalled) {
            val patchCcoResult = patchCCOWithAppSwitchEligibility(
                context = context,
                orderId = token,
                tokenType = tokenType,
                merchantOptInForAppSwitch = true,
                paypalNativeAppInstalled = true // TODO: implement native app installed check
            )
            when (patchCcoResult) {
                is APIResult.Success -> patchCcoResult.data.launchUrl?.toUri() ?: fallbackUri
                is APIResult.Failure -> fallbackUri
            }
        } else {
            fallbackUri
        }
    }
}
