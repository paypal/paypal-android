package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.ShopperSessionConfig
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.returnUrl
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
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
@Suppress(
    "TooManyFunctions", // Necessary due to multiple method variations for backward compatibility
    "LargeClass", // Necessary due to v3 + v2 + v1 method variants
)
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    // Enable app switch by switching this flag to true
    private val appSwitchWhenEligible: Boolean = true

    // for analytics tracking
    private var checkoutOrderId: String? = null
    private var vaultSetupTokenId: String? = null
    private var appSwitchEnabled: Boolean = false

    // Shopper Session ID (v3) — set by startPayPalSession(), awaited by start() / vault()
    private var shopperSessionDeferred: Deferred<CreateShopperSessionWithAppSwitchEligibilityResponse>? = null
    private var returnToAppUrlConfig: ReturnToAppUrlConfig? = null

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

    // region Active Methods

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
     * Pre-warms the Shopper Session in the background. Must be called before [start] or [vault]
     * in SDK v3 flows.
     *
     * Fire and forget — returns immediately. The GraphQL `createShopperSession` call runs
     * asynchronously and the result is stored internally as a [Deferred].
     *
     * Analytics events fired: `create-paypal-session:start`, `create-paypal-session:success`,
     * or `create-paypal-session:failure`.
     *
     * @param userIdentity Shopper identity used to pre-identify the payer.
     * @param urlConfig Return-to-app URLs used after checkout completes or is cancelled.
     * @param userAction Controls the call-to-action label on the PayPal checkout page.
     */
    fun startPayPalSession(
        userIdentity: PayPalUserIdentity,
        urlConfig: ReturnToAppUrlConfig,
        userAction: PayPalUserAction = PayPalUserAction.CONTINUE,
    ) {
        returnToAppUrlConfig = urlConfig
        shopperSessionDeferred = applicationScope.async {
            createShopperSessionWithAppSwitchEligibility(urlConfig, userIdentity, userAction)
        }
    }

    /**
     * Initiates PayPal checkout using the Shopper Session ID pre-warmed by [startPayPalSession].
     *
     * If the session fetch is still in progress this method awaits its completion before
     * launching checkout. If [startPayPalSession] was never called the callback receives a
     * [PayPalPresentAuthChallengeResult.Failure] with error code `SESSION_NOT_STARTED`.
     *
     * @param activity The activity to launch the PayPal checkout from.
     * @param orderId The ID of the order to be approved.
     * @param callback Callback to receive the auth-challenge result.
     */
    // TODO: Narrow exception type once createShopperSessionWithAppSwitchEligibility is implemented
    @Suppress("TooGenericExceptionCaught")
    fun start(
        activity: Activity,
        orderId: String,
        callback: PayPalWebStartCallback,
    ) {
        val deferred = shopperSessionDeferred
        if (deferred == null) {
            applicationScope.launch(Dispatchers.Main) {
                callback.onPayPalWebStartResult(
                    PayPalPresentAuthChallengeResult.Failure(
                        PayPalWebCheckoutError.sessionNotStartedError
                    )
                )
            }
            return
        }
        checkoutOrderId = orderId
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                analytics.notify(CheckoutEvent.STARTED, checkoutOrderId, appSwitchEnabled)
                val result = launchCheckoutWithShopperSession(
                    activity = activity,
                    shopperSession = shopperSession,
                    orderId = orderId,
                )
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebStartResult(result)
                }
            } catch (e: Exception) {
                analytics.notify(CheckoutEvent.FAILED, checkoutOrderId, appSwitchEnabled)
                shopperSessionDeferred = null
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebStartResult(
                        PayPalPresentAuthChallengeResult.Failure(
                            PayPalWebCheckoutError.browserSwitchError(e)
                        )
                    )
                }
            }
        }
    }

    /**
     * Initiates PayPal vault using the Shopper Session ID pre-warmed by [startPayPalSession].
     *
     * If the session fetch is still in progress this method awaits its completion before
     * launching the vault flow. If [startPayPalSession] was never called the callback receives a
     * [PayPalPresentAuthChallengeResult.Failure] with error code `SESSION_NOT_STARTED`.
     *
     * @param activity The activity to launch the PayPal vault flow from.
     * @param setupTokenId The setup token ID associated with the vault approval.
     * @param callback Callback to receive the vault result.
     */
    // TODO: Narrow exception type once createShopperSessionWithAppSwitchEligibility is implemented
    @Suppress("TooGenericExceptionCaught")
    fun vault(
        activity: ComponentActivity,
        setupTokenId: String,
        callback: PayPalWebVaultCallback,
    ) {
        val deferred = shopperSessionDeferred
        if (deferred == null) {
            applicationScope.launch(Dispatchers.Main) {
                callback.onPayPalWebVaultResult(
                    PayPalPresentAuthChallengeResult.Failure(
                        PayPalWebCheckoutError.sessionNotStartedError
                    )
                )
            }
            return
        }
        vaultSetupTokenId = setupTokenId
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                analytics.notify(VaultEvent.STARTED, vaultSetupTokenId, appSwitchEnabled)
                val result = launchVaultWithSession(
                    activity = activity,
                    shopperSession = shopperSession,
                    setupTokenId = setupTokenId,
                )
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebVaultResult(result)
                }
            } catch (e: Exception) {
                analytics.notify(VaultEvent.FAILED, vaultSetupTokenId, appSwitchEnabled)
                shopperSessionDeferred = null
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebVaultResult(
                        PayPalPresentAuthChallengeResult.Failure(
                            PayPalWebCheckoutError.browserSwitchError(e)
                        )
                    )
                }
            }
        }
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalWebCheckoutClient.start]), call this method to see if a user has
     * successfully authorized a PayPal account as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishStart(intent: Intent): PayPalWebCheckoutFinishStartResult? =
        sessionStore.authState?.let { authState ->
            val result = payPalWebLauncher.completeCheckoutAuthRequest(intent, authState)
            when (result) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                    analytics.notify(
                        CheckoutEvent.SUCCEEDED,
                        checkoutOrderId,
                        appSwitchEnabled
                    )
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    analytics.notify(
                        CheckoutEvent.CANCELED,
                        checkoutOrderId,
                        appSwitchEnabled
                    )
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    analytics.notify(
                        CheckoutEvent.FAILED,
                        checkoutOrderId,
                        appSwitchEnabled
                    )
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishStartResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            result
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
                    analytics.notify(VaultEvent.SUCCEEDED, vaultSetupTokenId, appSwitchEnabled)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishVaultResult.Failure -> {
                    analytics.notify(VaultEvent.FAILED, vaultSetupTokenId, appSwitchEnabled)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.Canceled -> {
                    analytics.notify(VaultEvent.CANCELED, vaultSetupTokenId, appSwitchEnabled)
                    sessionStore.clear()
                }

                PayPalWebCheckoutFinishVaultResult.NoResult -> {
                    // no analytics tracking required at the moment
                }
            }
            return result
        }

    /**
     * Launches the PayPal checkout UI after the shopper session has been resolved.
     *
     * Attempts a PayPal app switch (App Link) if the PayPal app is installed and eligible;
     * otherwise falls back to Chrome Custom Tabs.
     *
     * @param activity The activity context needed to launch the checkout UI.
     * @param shopperSession The shopper session returned by [createShopperSessionWithAppSwitchEligibility].
     * @param orderId The order ID to approve.
     */
    private suspend fun launchCheckoutWithShopperSession(
        activity: Activity,
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse,
        orderId: String,
    ): PayPalPresentAuthChallengeResult {
        val launchUri = withContext(ioDispatcher) {
            getLaunchUri(
                context = activity.applicationContext,
                token = orderId,
                tokenType = TokenType.ORDER_ID,
                fallbackUri = buildPayPalCheckoutUri(
                    orderId = orderId,
                    funding = PayPalWebCheckoutFundingSource.PAYPAL,
                    returnUrl = returnToAppUrlConfig?.returnAppUrl,
                ).buildUpon()
                    .appendQueryParameter(
                        "shopper_session_id",
                        shopperSession.shopperSessionConfig.id
                    )
                    .build()
            )
        }

        // TODO: Remove ReturnToAppStrategy all together.
        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = orderId,
            tokenType = TokenType.ORDER_ID,
            returnToAppStrategy = ReturnToAppStrategy.AppLink(returnToAppUrlConfig?.returnAppUrl ?: ""),
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    checkoutOrderId,
                    appSwitchEnabled,
                )
                sessionStore.authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    checkoutOrderId,
                    appSwitchEnabled,
                )
            }
        }
        return result
    }

    /**
     * Launches the PayPal vault UI after the shopper session has been resolved.
     *
     * Attempts a PayPal app switch (App Link) if the PayPal app is installed and eligible;
     * otherwise falls back to Chrome Custom Tabs.
     *
     * @param activity The activity context needed to launch the checkout UI.
     * @param shopperSession The shopper session returned by [createShopperSessionWithAppSwitchEligibility].
     * @param setupTokenId The setup token ID to approve.
     */
    @Suppress("UnusedPrivateMember") // TODO: shopperSession will be used once GraphQL call is implemented
    private suspend fun launchVaultWithSession(
        activity: Activity,
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse,
        setupTokenId: String,
    ): PayPalPresentAuthChallengeResult {
        val launchUri = withContext(ioDispatcher) {
            getLaunchUri(
                context = activity.applicationContext,
                token = setupTokenId,
                tokenType = TokenType.VAULT_ID,
                fallbackUri = buildPayPalVaultUri(setupTokenId)
            )
        }

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnToAppStrategy = ReturnToAppStrategy.AppLink(returnToAppUrlConfig?.returnAppUrl ?: ""),
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    vaultSetupTokenId,
                    appSwitchEnabled,
                )
                sessionStore.authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    vaultSetupTokenId,
                    appSwitchEnabled,
                )
            }
        }
        return result
    }

    /**
     * Creates a shopper session by calling the PayPal GraphQL `createShopperSession` mutation.
     *
     * Returns the session ID string on success. This is called internally by [startPayPalSession]
     * and runs on a background coroutine.
     *
     * TODO: DTPPMOBILE-530 — implement the actual GraphQL call.
     */
    @VisibleForTesting
    @Suppress("UnusedPrivateMember") // TODO: params will be used once GraphQL call is implemented
    internal suspend fun createShopperSessionWithAppSwitchEligibility(
        urlConfig: ReturnToAppUrlConfig,
        userIdentity: PayPalUserIdentity,
        userAction: PayPalUserAction,
    ): CreateShopperSessionWithAppSwitchEligibilityResponse {
        // TODO: Replace with real GraphQL createShopperSession mutation.
        return CreateShopperSessionWithAppSwitchEligibilityResponse(
            appSwitchEligible = true,
            redirectUrl = "",
            checkoutFallbackUrl = "",
            inEligibleReason = "",
            matchedAuthenticationMethods = emptyList(),
            shopperSessionConfig = ShopperSessionConfig("", "")
        )
    }

    // Used by deprecated start() methods
    @VisibleForTesting
    internal suspend fun startAsync(
        activity: Activity,
        request: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {

        checkoutOrderId = request.orderId
        analytics.notify(CheckoutEvent.STARTED, checkoutOrderId, appSwitchEnabled)

        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

        val launchUri = withContext(ioDispatcher) {
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
                    fallbackUri = buildPayPalCheckoutUri(
                        orderId = request.orderId,
                        funding = request.fundingSource,
                        returnUrl = returnToAppStrategy.returnUrl
                    )
                )
            }

            updateConfigDeferred.await() // waits for completion, ignores result
            launchUriDeferred.await() // returns launch URI
        }

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.orderId,
            tokenType = TokenType.ORDER_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    checkoutOrderId,
                    appSwitchEnabled
                )

                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    checkoutOrderId,
                    appSwitchEnabled
                )
            }
        }

        return result
    }

    // Used by deprecated start() methods
    @VisibleForTesting
    internal suspend fun vaultAsync(
        activity: Activity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        vaultSetupTokenId = request.setupTokenId
        analytics.notify(VaultEvent.STARTED, vaultSetupTokenId, appSwitchEnabled)

        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

        val launchUri = withContext(ioDispatcher) {
            getLaunchUri(
                context = activity.applicationContext,
                token = request.setupTokenId,
                tokenType = TokenType.VAULT_ID,
                fallbackUri = buildPayPalVaultUri(request.setupTokenId)
            )
        }

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    vaultSetupTokenId,
                    appSwitchEnabled
                )

                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    vaultSetupTokenId,
                    appSwitchEnabled
                )
            }
        }

        return result
    }
    // endregion

    // region Private Helpers

    private fun buildPayPalCheckoutUri(
        orderId: String?,
        funding: PayPalWebCheckoutFundingSource,
        returnUrl: String?
    ): Uri {
        return baseUrl.toUri()
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", returnUrl)
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
                is APIResult.Success -> {
                    appSwitchEnabled = patchCcoResult.data.appSwitchEligible
                    patchCcoResult.data.launchUrl?.toUri() ?: fallbackUri
                }

                is APIResult.Failure -> {
                    appSwitchEnabled = false
                    fallbackUri
                }
            }
        } else {
            appSwitchEnabled = false
            fallbackUri
        }
    }

    /**
     * Resolves the return to app strategy from request or falls back to urlScheme.
     * if both are not available then throws error
     */
    private fun resolveReturnToAppStrategy(
        requestStrategy: ReturnToAppStrategy?
    ): ReturnToAppStrategy? {
        return requestStrategy ?: urlScheme?.let {
            ReturnToAppStrategy.CustomUrlScheme(urlScheme)
        }
    }

    // endregion

    // region Deprecated Methods

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
        appSwitchEnabled = false
        analytics.notify(CheckoutEvent.STARTED, checkoutOrderId, appSwitchEnabled)

        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

        val launchUri = buildPayPalCheckoutUri(
            orderId = request.orderId,
            funding = request.fundingSource,
            returnUrl = returnToAppStrategy.returnUrl
        )

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.orderId,
            tokenType = TokenType.ORDER_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    checkoutOrderId,
                    appSwitchEnabled
                )

                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    checkoutOrderId,
                    appSwitchEnabled
                )
            }
        }
        return result
    }

    /**
     * Confirm PayPal payment source for an order with callback.
     *
     * @deprecated Use [startPayPalSession] followed by [start] with only the order ID instead.
     */
    @Deprecated(
        message = "Use startPayPalSession() followed by start(activity, orderId, callback) instead.",
        replaceWith = ReplaceWith("start(activity, request.orderId, callback)")
    )
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
        vaultSetupTokenId = request.setupTokenId
        analytics.notify(VaultEvent.STARTED, vaultSetupTokenId, appSwitchEnabled)

        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

        val launchUri = buildPayPalVaultUri(request.setupTokenId)

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    vaultSetupTokenId,
                    appSwitchEnabled
                )

                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    vaultSetupTokenId,
                    appSwitchEnabled
                )
            }
        }

        return result
    }

    /**
     * Vault PayPal as a payment method with callback.
     *
     * @deprecated Use [startPayPalSession] followed by [vault] with only the setup token ID instead.
     */
    @Deprecated(
        message = "Use startPayPalSession() followed by vault(activity, setupTokenId, callback) instead.",
        replaceWith = ReplaceWith("vault(activity, request.setupTokenId, callback)")
    )
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
                analytics.notify(CheckoutEvent.SUCCEEDED, checkoutOrderId, appSwitchEnabled)

            is PayPalWebCheckoutFinishStartResult.Canceled ->
                analytics.notify(CheckoutEvent.CANCELED, checkoutOrderId, appSwitchEnabled)

            is PayPalWebCheckoutFinishStartResult.Failure ->
                analytics.notify(CheckoutEvent.FAILED, checkoutOrderId, appSwitchEnabled)

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
    @Deprecated(
        message = "Auth state is now captured internally by the SDK. Please migrate to finishVault(intent).",
        replaceWith = ReplaceWith("finishVault(intent)")
    )
    fun finishVault(intent: Intent, authState: String): PayPalWebCheckoutFinishVaultResult {
        val result = payPalWebLauncher.completeVaultAuthRequest(intent, authState)
        // TODO: see if we can get setup token id from somewhere for tracking
        when (result) {
            is PayPalWebCheckoutFinishVaultResult.Success ->
                analytics.notify(VaultEvent.SUCCEEDED, vaultSetupTokenId, appSwitchEnabled)

            is PayPalWebCheckoutFinishVaultResult.Failure ->
                analytics.notify(VaultEvent.FAILED, vaultSetupTokenId, appSwitchEnabled)

            PayPalWebCheckoutFinishVaultResult.Canceled ->
                analytics.notify(VaultEvent.CANCELED, vaultSetupTokenId, appSwitchEnabled)

            PayPalWebCheckoutFinishVaultResult.NoResult -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    // endregion
}
