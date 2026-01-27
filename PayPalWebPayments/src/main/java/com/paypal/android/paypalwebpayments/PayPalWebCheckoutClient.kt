package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// NEXT MAJOR VERSION: consider renaming this module to PayPalWebClient since
// it now offers both checkout and vaulting

/**
 * Use this client to approve an order with a [PayPalWebCheckoutRequest].
 */
@Suppress("TooManyFunctions") // Necessary due to multiple method variations for backward compatibility
class PayPalWebCheckoutClient internal constructor(
    private val analytics: PayPalWebAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher,
    private val sessionStore: PayPalWebCheckoutSessionStore,
    private val deviceInspector: DeviceInspector,
    private val coreConfig: CoreConfig,
    private val updateClientConfigAPI: UpdateClientConfigAPI,
    private val patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility,
    private val urlScheme: String? = null,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),

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
    @Deprecated(
        message = "Use PayPalWebCheckoutClient(context, configuration) instead.",
        replaceWith = ReplaceWith("PayPalWebCheckoutClient(context, configuration)")
    )
    constructor(
        context: Context,
        configuration: CoreConfig,
        urlScheme: String
    ) : this(
        analytics = PayPalWebAnalytics(AnalyticsService(context.applicationContext, configuration)),
        payPalWebLauncher = PayPalWebLauncher(context),
        sessionStore = PayPalWebCheckoutSessionStore(),
        deviceInspector = DeviceInspector(context),
        coreConfig = configuration,
        urlScheme = urlScheme,
        patchCCOWithAppSwitchEligibility = PatchCCOWithAppSwitchEligibility(configuration),
        updateClientConfigAPI = UpdateClientConfigAPI(context, configuration),
    )

    constructor(
        context: Context,
        configuration: CoreConfig
    ) : this(
        analytics = PayPalWebAnalytics(AnalyticsService(context.applicationContext, configuration)),
        payPalWebLauncher = PayPalWebLauncher(context),
        sessionStore = PayPalWebCheckoutSessionStore(),
        deviceInspector = DeviceInspector(context),
        coreConfig = configuration,
        urlScheme = null,
        patchCCOWithAppSwitchEligibility = PatchCCOWithAppSwitchEligibility(configuration),
        updateClientConfigAPI = UpdateClientConfigAPI(context, configuration),
    )

    /**
     * Check if the device supports auth tabs for Chrome browser.
     * When true, auth tab launcher flow is available. When false, falls back to custom tabs.
     */
    val isAuthTabSupported: Boolean
        get() = deviceInspector.isAuthTabSupported

    /**
     * Capture instance state for later restoration. This can be useful for recovery during a
     * process kill.
     */
    val instanceState: String
        get() = sessionStore.toBase64EncodedJSON()

    /**
     * Restore a feature client using instance state. @see [instanceState]
     */
    fun restore(instanceState: String) {
        sessionStore.restore(instanceState)
    }

    /**
     * Confirm PayPal payment source for an order.
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     */
    @Deprecated(
        message = "Use start(activity, request, callback) for callback-based flows, includes app switching feature",
        replaceWith = ReplaceWith("start(activity, request, callback)")
    )
    fun start(
        activity: Activity,
        request: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        checkoutOrderId = request.orderId
        analytics.notify(CheckoutEvent.STARTED, checkoutOrderId)

        val launchUri = buildPayPalCheckoutUri(
            orderId = request.orderId,
            funding = request.fundingSource,
            redirectUrl = request.appLinkUrl ?: redirectUriPayPalCheckout
        )

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.orderId,
            tokenType = TokenType.ORDER_ID,
            returnUrlScheme = request.fallbackUrlScheme ?: urlScheme,
            appLinkUrl = request.appLinkUrl
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    checkoutOrderId
                )

                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    checkoutOrderId
                )
            }
        }
        return result
    }

    /**
     * Confirm PayPal payment source for an order.
     *
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     * @param activityResultLauncher Optional ActivityResultLauncher to use for browser switching
     */
    @VisibleForTesting
    internal suspend fun startAsync(
        activity: Activity,
        request: PayPalWebCheckoutRequest,
        activityResultLauncher: ActivityResultLauncher<Intent>? = null
    ): PayPalPresentAuthChallengeResult {

        checkoutOrderId = request.orderId
        analytics.notify(CheckoutEvent.STARTED, checkoutOrderId)

        val launchUri = withContext(Dispatchers.IO) {
            // perform updateCCO and getLaunchUri in parallel
            val updateConfigDeferred = async {
                updateClientConfigAPI.updateClientConfig(
                    request.orderId,
                    request.fundingSource.value
                )
            }
            val launchUriDeferred = async {
                getLaunchUri(
                    context = activity.applicationContext,
                    token = request.orderId,
                    tokenType = TokenType.ORDER_ID,
                    appSwitchWhenEligible = request.appSwitchWhenEligible,
                    fallbackUri = buildPayPalCheckoutUri(
                        orderId = request.orderId,
                        funding = request.fundingSource,
                        request.appLinkUrl ?: redirectUriPayPalCheckout
                    )
                )
            }

            updateConfigDeferred.await() // waits for completion, ignores result
            launchUriDeferred.await() // returns launch URI
        }

        val result = if (activityResultLauncher != null) {
            payPalWebLauncher.launchWithUrl(
                uri = launchUri,
                token = request.orderId,
                tokenType = TokenType.ORDER_ID,
                activityResultLauncher = activityResultLauncher,
                returnUrlScheme = request.fallbackUrlScheme ?: urlScheme,
                appLinkUrl = request.appLinkUrl,
                context = activity
            )
        } else {
            payPalWebLauncher.launchWithUrl(
                activity = activity,
                uri = launchUri,
                token = request.orderId,
                tokenType = TokenType.ORDER_ID,
                returnUrlScheme = request.fallbackUrlScheme ?: urlScheme,
                appLinkUrl = request.appLinkUrl
            )
        }

        handleAuthChallengeResult(result)
        return result
    }

    private fun handleAuthChallengeResult(result: PayPalPresentAuthChallengeResult) {
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    checkoutOrderId
                )
                // update auth state value in session store
                sessionStore.authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    checkoutOrderId
                )
            }
        }
    }

    /**
     * Confirm PayPal payment source for an order with callback.
     *
     * @param activity The activity to launch the PayPal web checkout from
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     * @param callback [PayPalWebStartCallback] to receive the result
     */
    fun start(
        activity: Activity,
        request: PayPalWebCheckoutRequest,
        callback: PayPalWebStartCallback
    ) {
        applicationScope.launch {
            val result = startAsync(activity, request)
            withContext(Dispatchers.Main) {
                callback.onPayPalWebStartResult(result)
            }
        }
    }

    /**
     * Confirm PayPal payment source for an order with callback and custom activity result launcher.
     *
     * @param activity The activity to launch the PayPal web checkout from
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     * @param activityResultLauncher The ActivityResultLauncher to use for browser switching. Must be
     *        initialized before the activity, check [registerForActivityResult()](https://developer.android.com/training/basics/intents/result#register).
     * @param callback [PayPalWebStartCallback] to receive the result
     */
    fun start(
        activity: Activity,
        request: PayPalWebCheckoutRequest,
        activityResultLauncher: ActivityResultLauncher<Intent>,
        callback: PayPalWebStartCallback
    ) {
        applicationScope.launch {
            val result = startAsync(activity, request, activityResultLauncher)
            withContext(Dispatchers.Main) {
                callback.onPayPalWebStartResult(result)
            }
        }
    }
    /**
     * Vault PayPal as a payment method.
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    @Deprecated(
        message = "Use vault(activity, request, callback) for callback-based flows, includes app switching feature",
        replaceWith = ReplaceWith("vault(activity, request, callback)")
    )
    fun vault(
        activity: Activity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        checkoutOrderId = request.setupTokenId
        analytics.notify(VaultEvent.STARTED, vaultSetupTokenId)

        val launchUri = buildPayPalVaultUri(request.setupTokenId)

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnUrlScheme = request.fallbackUrlScheme ?: urlScheme,
            appLinkUrl = request.appLinkUrl
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    vaultSetupTokenId
                )

                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    vaultSetupTokenId
                )
            }
        }

        return result
    }

    /**
     * Vault PayPal as a payment method.
     *
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     */
    @VisibleForTesting
    internal suspend fun vaultAsync(
        activity: Activity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        vaultSetupTokenId = request.setupTokenId
        analytics.notify(VaultEvent.STARTED, vaultSetupTokenId)

        val launchUri = withContext(Dispatchers.IO) {
            getLaunchUri(
                context = activity.applicationContext,
                token = request.setupTokenId,
                tokenType = TokenType.VAULT_ID,
                appSwitchWhenEligible = request.appSwitchWhenEligible,
                fallbackUri = buildPayPalVaultUri(request.setupTokenId)
            )
        }

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnUrlScheme = request.fallbackUrlScheme ?: urlScheme,
            appLinkUrl = request.appLinkUrl
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    vaultSetupTokenId
                )

                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    vaultSetupTokenId
                )
            }
        }

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
        applicationScope.launch(Dispatchers.Main) {
            callback.onPayPalWebVaultResult(vaultAsync(activity, request))
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
    @Deprecated(
        message = "Auth state is now captured internally by the SDK. Please migrate to finishStart(intent).",
        replaceWith = ReplaceWith("finishStart(intent)")
    )
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
     * (@see [PayPalWebCheckoutClient.start]), call this method to see if a user has
     * successfully authorized a PayPal account as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishStart(intent: Intent): PayPalWebCheckoutFinishStartResult? {
        return sessionStore.authState?.let { authState ->
            val result = payPalWebLauncher.completeCheckoutAuthRequest(intent, authState)
            when (result) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                    analytics.notify(CheckoutEvent.SUCCEEDED, checkoutOrderId)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    analytics.notify(CheckoutEvent.CANCELED, checkoutOrderId)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    analytics.notify(CheckoutEvent.FAILED, checkoutOrderId)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishStartResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            result
        }
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
    @Deprecated(
        message = "Auth state is now captured internally by the SDK. Please migrate to finishVault(intent).",
        replaceWith = ReplaceWith("finishVault(intent)")
    )
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

    private val redirectUriPayPalCheckout
        get() = urlScheme?.let { "$urlScheme://x-callback-url/paypal-sdk/paypal-checkout" }

    private fun buildPayPalCheckoutUri(
        orderId: String?,
        funding: PayPalWebCheckoutFundingSource,
        redirectUrl: String?
    ): Uri {
        return baseUrl.toUri()
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", redirectUrl)
            .appendQueryParameter("native_xo", "1")
            .appendQueryParameter("fundingSource", funding.value)
            .appendQueryParameter("integration_artifact", UpdateClientConfigAPI.Defaults.INTEGRATION_ARTIFACT)
            .build()
    }

    private fun buildPayPalVaultUri(
        setupTokenId: String
    ): Uri {
        return baseUrl.toUri()
            .buildUpon()
            .appendPath("agreements")
            .appendPath("approve")
            .appendQueryParameter("approval_session_id", setupTokenId)
            .build()
    }

    private val baseUrl: String
        get() = when (coreConfig.environment) {
            Environment.LIVE -> "https://paypal.com/"
            Environment.SANDBOX -> "https://sandbox.paypal.com/"
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
                paypalNativeAppInstalled = true
            )
            when (patchCcoResult) {
                is APIResult.Success -> patchCcoResult.data.launchUrl?.toUri() ?: fallbackUri
                is APIResult.Failure -> fallbackUri
            }
        } else {
            fallbackUri
        }
    }
    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.vault]), call this method to see if a user has
     * successfully authorized a PayPal account for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishVault(intent: Intent): PayPalWebCheckoutFinishVaultResult? =
        sessionStore.authState?.let { authState ->
            val result = payPalWebLauncher.completeVaultAuthRequest(intent, authState)
            when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success -> {
                    analytics.notify(VaultEvent.SUCCEEDED, vaultSetupTokenId)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishVaultResult.Failure -> {
                    analytics.notify(VaultEvent.FAILED, vaultSetupTokenId)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.Canceled -> {
                    analytics.notify(VaultEvent.CANCELED, vaultSetupTokenId)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            return result
        }
}
