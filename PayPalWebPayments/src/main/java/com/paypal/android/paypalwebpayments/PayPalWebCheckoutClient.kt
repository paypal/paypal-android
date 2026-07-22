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
import com.paypal.android.corepayments.HttpRoundTripTiming
import com.paypal.android.corepayments.LinkType
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.api.CreateShopperSessionWithAppSwitchEligibilityAPI
import com.paypal.android.corepayments.api.PatchCCOWithAppSwitchEligibility
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.linkType
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityParams
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.returnUrl
import com.paypal.android.paypalwebpayments.analytics.AppSwitchAnalyticsEventParams
import com.paypal.android.corepayments.usecase.GetAppLinksCompatibleBrowserUseCase
import com.paypal.android.corepayments.usecase.GetDefaultAppUseCase
import com.paypal.android.corepayments.usecase.GetReturnLinkTypeUseCase
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.CreatePayPalSessionEvent
import com.paypal.android.paypalwebpayments.analytics.LatencyEndpoint
import com.paypal.android.paypalwebpayments.analytics.LatencyFlow
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.PresentationType
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
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
    private val createShopperSessionAPI: CreateShopperSessionWithAppSwitchEligibilityAPI,
    private val getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase,
    private val urlScheme: String? = null,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {

    private var appSwitchEnabled: Boolean = false
    private var appSwitchAnalyticsEventParams = createAppSwitchAnalyticsEventParams()

    // Shopper Session id (v3) — set by createPayPalSession(), awaited by start() / vault()
    @VisibleForTesting
    internal var shopperSessionDeferred: Deferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>? = null
    private var returnToAppUrlConfig: ReturnToAppUrlConfig? = null
    private var sessionTokenType: TokenType? = null

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
        createShopperSessionAPI = CreateShopperSessionWithAppSwitchEligibilityAPI(
            configuration,
            context.applicationContext,
        ),
        getReturnLinkTypeUseCase = buildReturnLinkTypeUseCase(context.applicationContext),
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
     * Pre-warms the shopper session in the background. Must be called before [start] or [vault].
     *
     * Fire and forget — returns immediately.
     *
     * @param tokenType Whether the session is for an order (checkout), a setup token (vault), or
     * a billing agreement token. This also determines the query param name used for the token on
     * the launch uri built by [start]/[vault] (see [appendTokenQueryParam]).
     * @param userIdentity Shopper identity used to pre-identify the payer.
     * @param urlConfig Return-to-app URLs used after checkout completes or is cancelled.
     * @param userAction Controls the call-to-action label on the PayPal checkout page.
     */
    fun createPayPalSession(
        tokenType: TokenType,
        userIdentity: PayPalUserIdentity?,
        urlConfig: ReturnToAppUrlConfig,
        userAction: PayPalUserAction = PayPalUserAction.CONTINUE,
    ) {
        resetAppSwitchAnalyticsEventParams()
        sessionTokenType = tokenType
        returnToAppUrlConfig = urlConfig
        shopperSessionDeferred = applicationScope.async {
            createShopperSessionWithAppSwitchEligibility("ppcp_android", tokenType, urlConfig, userIdentity, userAction)
        }
    }

    /**
     * Initiates PayPal checkout using the session pre-warmed by [createPayPalSession].
     *
     * If the session fetch is still in progress this method awaits its completion before
     * launching checkout. If [createPayPalSession] was never called the callback receives a
     * [PayPalPresentAuthChallengeResult.Failure].
     *
     * @param activity The Activity to launch the PayPal checkout from.
     * @param orderId The id of the order to be approved.
     * @param callback Callback to receive the auth-challenge result.
     */
    @Suppress("TooGenericExceptionCaught")
    fun start(
        activity: Activity,
        orderId: String,
        callback: PayPalWebStartCallback,
    ) {
        val deferred = shopperSessionDeferred
        val startTime = System.currentTimeMillis()
        if (deferred == null) {
            resetAppSwitchAnalyticsEventParams()
            notifyUserPerceivedLatencyError(LatencyFlow.CHECKOUT, startTime)
            notifyCheckoutSessionNotStarted(orderId, callback)
            return
        }
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(
            checkoutOrderId = orderId,
            isVault = false,
        )
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                setShopperSessionAnalyticsParams(shopperSession)
                analytics.notify(CheckoutEvent.STARTED, params = appSwitchAnalyticsEventParams)

                val result = if (shopperSession != null) {
                    launchCheckoutWithShopperSession(
                        activity = activity,
                        shopperSession = shopperSession,
                        orderId = orderId,
                        startTime = startTime,
                    )
                } else {
                    launchCheckoutViaPatchCCOFallback(
                        activity = activity,
                        orderId = orderId,
                    )
                }
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebStartResult(result)
                }
            } catch (e: Exception) {
                analytics.notify(
                    event = CheckoutEvent.FAILED,
                    params = appSwitchAnalyticsEventParams,
                    errorDescription = e.message
                )
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

    private fun notifyCheckoutSessionNotStarted(orderId: String, callback: PayPalWebStartCallback) {
        applicationScope.launch(Dispatchers.Main) {
            analytics.notify(
                CheckoutEvent.SESSION_NOT_STARTED,
                params = appSwitchAnalyticsEventParams.copy(checkoutOrderId = orderId),
                errorDescription = "startPayPalSession() must be called before start()."
            )
            callback.onPayPalWebStartResult(
                PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.sessionNotCreatedError)
            )
        }
    }

    /**
     * Initiates PayPal vault using the session pre-warmed by [createPayPalSession].
     *
     * If the session fetch is still in progress this method awaits its completion before
     * launching the vault flow. If [createPayPalSession] was never called the callback receives a
     * [PayPalPresentAuthChallengeResult.Failure].
     *
     * @param activity The Activity to launch the PayPal vault flow from.
     * @param setupTokenId The setup token id associated with the vault approval.
     * @param callback Callback to receive the vault result.
     */
    @Suppress("TooGenericExceptionCaught")
    fun vault(
        activity: Activity,
        setupTokenId: String,
        callback: PayPalWebVaultCallback,
    ) {
        val startTime = System.currentTimeMillis()
        val deferred = shopperSessionDeferred
        if (deferred == null) {
            resetAppSwitchAnalyticsEventParams()
            notifyUserPerceivedLatencyError(LatencyFlow.VAULT, startTime)
            notifyVaultSessionNotStarted(setupTokenId, callback)
            return
        }
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(
            vaultSetupTokenId = setupTokenId,
            isVault = true,
        )
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                setShopperSessionAnalyticsParams(shopperSession)
                analytics.notify(VaultEvent.STARTED, params = appSwitchAnalyticsEventParams)

                val result = if (shopperSession != null) {
                    launchVaultWithSession(
                        activity = activity,
                        shopperSession = shopperSession,
                        setupTokenId = setupTokenId,
                        startTime = startTime,
                    )
                } else {
                    launchVaultViaPatchCCOFallback(
                        activity = activity,
                        setupTokenId = setupTokenId,
                    )
                }
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebVaultResult(result)
                }
            } catch (e: Exception) {
                analytics.notify(
                    event = VaultEvent.FAILED,
                    params = appSwitchAnalyticsEventParams,
                    errorDescription = e.message
                )
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

    private fun notifyVaultSessionNotStarted(setupTokenId: String, callback: PayPalWebVaultCallback) {
        applicationScope.launch(Dispatchers.Main) {
            analytics.notify(
                VaultEvent.SESSION_NOT_STARTED,
                params = appSwitchAnalyticsEventParams.copy(vaultSetupTokenId = setupTokenId),
                errorDescription = "startPayPalSession() must be called before vault()."
            )
            callback.onPayPalWebVaultResult(
                PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.sessionNotCreatedError)
            )
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
            analytics.notify(CheckoutEvent.HANDLE_RETURN_STARTED, params = appSwitchAnalyticsEventParams)
            val result = payPalWebLauncher.completeCheckoutAuthRequest(intent, authState)
            logCheckoutResult(result)
            if (result != PayPalWebCheckoutFinishStartResult.NoResult) {
                sessionStore.clear()
            }
            result
        }

    private fun logCheckoutResult(result: PayPalWebCheckoutFinishStartResult) {
        val (handleReturnResult, checkoutResult, errorDescription) = when (result) {
            is PayPalWebCheckoutFinishStartResult.Success ->
                Triple(CheckoutEvent.HANDLE_RETURN_SUCCEEDED, CheckoutEvent.SUCCEEDED, null)

            is PayPalWebCheckoutFinishStartResult.Canceled ->
                Triple(CheckoutEvent.HANDLE_RETURN_SUCCEEDED, CheckoutEvent.CANCELED, null)

            is PayPalWebCheckoutFinishStartResult.Failure ->
                Triple(CheckoutEvent.HANDLE_RETURN_FAILED, CheckoutEvent.FAILED, result.error.errorDescription)

            PayPalWebCheckoutFinishStartResult.NoResult -> return // no analytics tracking required at the moment
        }

        analytics.notify(
            event = handleReturnResult,
            params = appSwitchAnalyticsEventParams,
            errorDescription = errorDescription
        )
        analytics.notify(
            event = checkoutResult,
            params = appSwitchAnalyticsEventParams,
            errorDescription = errorDescription
        )
        if (checkoutResult == CheckoutEvent.CANCELED) {
            val event = when (appSwitchEnabled) {
                true -> CheckoutEvent.APP_SWITCH_CANCELED
                else -> CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_CANCELED
            }
            analytics.notify(event, params = appSwitchAnalyticsEventParams)
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
            analytics.notify(VaultEvent.HANDLE_RETURN_STARTED, params = appSwitchAnalyticsEventParams)
            val result = payPalWebLauncher.completeVaultAuthRequest(intent, authState)
            logVaultResult(result)
            if (result != PayPalWebCheckoutFinishVaultResult.NoResult) {
                sessionStore.clear()
            }
            result
        }

    private fun logVaultResult(result: PayPalWebCheckoutFinishVaultResult) {
        val (handleReturnResult, vaultResult, errorDescription) = when (result) {
            is PayPalWebCheckoutFinishVaultResult.Success ->
                Triple(VaultEvent.HANDLE_RETURN_SUCCEEDED, VaultEvent.SUCCEEDED, null)

            PayPalWebCheckoutFinishVaultResult.Canceled ->
                Triple(VaultEvent.HANDLE_RETURN_SUCCEEDED, VaultEvent.CANCELED, null)

            is PayPalWebCheckoutFinishVaultResult.Failure ->
                Triple(VaultEvent.HANDLE_RETURN_FAILED, VaultEvent.FAILED, result.error.errorDescription)

            PayPalWebCheckoutFinishVaultResult.NoResult -> return // no analytics tracking required at the moment
        }

        analytics.notify(
            event = handleReturnResult,
            params = appSwitchAnalyticsEventParams,
            errorDescription = errorDescription
        )
        analytics.notify(
            event = vaultResult,
            params = appSwitchAnalyticsEventParams,
            errorDescription = errorDescription
        )
        if (vaultResult == VaultEvent.CANCELED) {
            val event = when (appSwitchEnabled) {
                true -> VaultEvent.APP_SWITCH_CANCELED
                else -> VaultEvent.AUTH_CHALLENGE_PRESENTATION_CANCELED
            }
            analytics.notify(event, params = appSwitchAnalyticsEventParams)
        }
    }

    /**
     * Launches the PayPal checkout UI after the shopper session has been resolved.
     *
     * Attempts a PayPal app switch (App Link) if the PayPal app is installed and eligible;
     * otherwise falls back to Chrome Custom Tabs.
     *
     * @param activity The Activity needed to launch the checkout UI.
     * @param shopperSession The resolved shopper session containing launch URLs and eligibility.
     * @param orderId The order id to approve.
     */
    private fun launchCheckoutWithShopperSession(
        activity: Activity,
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse,
        orderId: String,
        startTime: Long,
    ): PayPalPresentAuthChallengeResult {
        appSwitchEnabled = shopperSession.appSwitchEligible && canAttemptPayPalAppSwitch()
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(appSwitchEnabled = appSwitchEnabled)
        val launchUri = shopperSession.getLaunchUri(orderId)
        val returnToAppStrategy = resolveReturnLinkStrategy()
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(linkType = returnToAppStrategy.linkType)
        if (appSwitchEnabled) {
            appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(appSwitchUrl = launchUri.toString())
            analytics.notify(CheckoutEvent.APP_SWITCH_STARTED, params = appSwitchAnalyticsEventParams)
        } else {
            analytics.notify(CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_STARTED, params = appSwitchAnalyticsEventParams)
        }
        val endTime = System.currentTimeMillis()

        val result = payPalWebLauncher.launchWithUrl(
            context = activity,
            uri = launchUri,
            token = orderId,
            tokenType = TokenType.ORDER_ID,
            returnToAppStrategy = returnToAppStrategy,
        )
        logCheckoutPresentAuthChallengeResult(result)
        notifyUserPerceivedLatency(LatencyFlow.CHECKOUT, result, startTime, endTime)
        return result
    }

    private fun logCheckoutPresentAuthChallengeResult(result: PayPalPresentAuthChallengeResult) {
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                val event = if (appSwitchEnabled) {
                    CheckoutEvent.APP_SWITCH_SUCCEEDED
                } else {
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED
                }
                analytics.notify(event, params = appSwitchAnalyticsEventParams)
                sessionStore.authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                val event = if (appSwitchEnabled) {
                    CheckoutEvent.APP_SWITCH_FAILED
                } else {
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED
                }
                analytics.notify(
                    event,
                    params = appSwitchAnalyticsEventParams,
                    errorDescription = result.error.errorDescription
                )
            }
        }
    }

    /**
     * Launches the PayPal vault UI after the shopper session has been resolved.
     *
     * Attempts a PayPal app switch (App Link) if the PayPal app is installed and eligible;
     * otherwise falls back to Chrome Custom Tabs.
     *
     * @param activity The Activity needed to launch the vault UI.
     * @param shopperSession The resolved shopper session containing launch URLs and eligibility.
     * @param setupTokenId The setup token id to approve.
     */
    private fun launchVaultWithSession(
        activity: Activity,
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse,
        setupTokenId: String,
        startTime: Long,
    ): PayPalPresentAuthChallengeResult {
        appSwitchEnabled = shopperSession.appSwitchEligible && canAttemptPayPalAppSwitch()
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(appSwitchEnabled = appSwitchEnabled)
        val launchUri = shopperSession.getLaunchUri(setupTokenId)
        val returnToAppStrategy = resolveReturnLinkStrategy()
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(linkType = returnToAppStrategy.linkType)
        val endTime = System.currentTimeMillis()

        if (appSwitchEnabled) {
            appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(appSwitchUrl = launchUri.toString())
            analytics.notify(VaultEvent.APP_SWITCH_STARTED, params = appSwitchAnalyticsEventParams)
        } else {
            analytics.notify(VaultEvent.AUTH_CHALLENGE_PRESENTATION_STARTED, params = appSwitchAnalyticsEventParams)
        }

        val result = payPalWebLauncher.launchWithUrl(
            context = activity,
            uri = launchUri,
            token = setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnToAppStrategy = returnToAppStrategy,
        )
        notifyUserPerceivedLatency(LatencyFlow.VAULT, result, startTime, endTime)
        logVaultPresentAuthChallengeResult(result)
        return result
    }

    private fun logVaultPresentAuthChallengeResult(result: PayPalPresentAuthChallengeResult) {
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                val event = if (appSwitchEnabled) {
                    VaultEvent.APP_SWITCH_SUCCEEDED
                } else {
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED
                }
                analytics.notify(event, params = appSwitchAnalyticsEventParams)
                sessionStore.authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                val event = if (appSwitchEnabled) {
                    VaultEvent.APP_SWITCH_FAILED
                } else {
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED
                }
                analytics.notify(
                    event,
                    params = appSwitchAnalyticsEventParams,
                    errorDescription = result.error.errorDescription
                )
            }
        }
    }

    /**
     * Launches checkout after the Shopper Session fetch failed with a session-creation
     * failure or network timeout.
     *
     * TODO: Re-implement fallback behavior for session-creation failure. This previously
     *  fell back to the legacy patchCCO path via [getLaunchUri] (see git history / PR diff
     *  for the removed implementation). Removed pending a decision on the replacement
     *  behavior — currently unhandled.
     */
    @Suppress("UnusedPrivateMember")
    private suspend fun launchCheckoutViaPatchCCOFallback(
        activity: Activity,
        orderId: String,
    ): PayPalPresentAuthChallengeResult {
        TODO("Re-implement checkout fallback for session-creation failure (patchCCO fallback removed)")
    }

    /**
     * Launches vault after the Shopper Session fetch failed with a session-creation failure
     * or network timeout. See [launchCheckoutViaPatchCCOFallback].
     *
     * TODO: Re-implement fallback behavior for session-creation failure. This previously
     *  fell back to the legacy patchCCO path via [getLaunchUri] (see git history / PR diff
     *  for the removed implementation). Removed pending a decision on the replacement
     *  behavior — currently unhandled.
     */
    @Suppress("UnusedPrivateMember")
    private suspend fun launchVaultViaPatchCCOFallback(
        activity: Activity,
        setupTokenId: String,
    ): PayPalPresentAuthChallengeResult {
        TODO("Re-implement vault fallback for session-creation failure (patchCCO fallback removed)")
    }

    @VisibleForTesting
    internal suspend fun createShopperSessionWithAppSwitchEligibility(
        token: String,
        tokenType: TokenType,
        urlConfig: ReturnToAppUrlConfig,
        userIdentity: PayPalUserIdentity?,
        userAction: PayPalUserAction,
    ): CreateShopperSessionWithAppSwitchEligibilityResponse? {
        val isVaultRequest = tokenType != TokenType.ORDER_ID
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(
            isCachedSession = userIdentity?.existingPayPalSessionId != null,
            userActionValue = userAction.toExternalPaymentType(),
            isVault = isVaultRequest,
            merchantId = coreConfig.merchantId,
            bnCode = coreConfig.bnCode,
            clientId = coreConfig.clientId,
            paypalInstalled = canAttemptPayPalAppSwitch().toString(),
            returnAppUrl = urlConfig.returnAppUrl,
            cancelAppUrl = urlConfig.cancelAppUrl,
            fallbackSchemeUrl = urlConfig.fallbackSchemeUrl,
        )
        analytics.notify(CreatePayPalSessionEvent.STARTED, params = appSwitchAnalyticsEventParams)

        val result = createShopperSessionAPI(
            token = token,
            tokenType = tokenType,
            params = CreateShopperSessionWithAppSwitchEligibilityParams(
                returnAppUrl = urlConfig.returnAppUrl,
                cancelAppUrl = urlConfig.cancelAppUrl,
                fallbackSchemeUrl = urlConfig.fallbackSchemeUrl,
                paymentType = userAction.toExternalPaymentType(),
                paypalNativeAppInstalled = canAttemptPayPalAppSwitch(),
                countryCode = userIdentity?.phone?.countryCode,
                nationalNumber = userIdentity?.phone?.nationalNumber,
                buyerEmailAddressMerchantPassed = userIdentity?.email,
                existingPayPalSessionId = userIdentity?.existingPayPalSessionId,
            ),
        )

        notifyApiRequestLatency(LatencyEndpoint.CREATE_SESSION, result.roundTripTiming)

        return when (result) {
            is APIResult.Success -> {
                appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(
                    shopperSession = result.data,
                )
                analytics.notify(CreatePayPalSessionEvent.SUCCEEDED, params = appSwitchAnalyticsEventParams)
                result.data
            }
            // Session creation failure / network timeout: fall back to patchCCO.
            is APIResult.Failure -> {
                analytics.notify(
                    CreatePayPalSessionEvent.FAILED,
                    params = appSwitchAnalyticsEventParams,
                    errorDescription = result.error.errorDescription,
                )
                null
            }
        }
    }

    // Used by deprecated start() methods
    @VisibleForTesting
    internal suspend fun startAsync(
        activity: Activity,
        request: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

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
            context = activity,
            uri = launchUri,
            token = request.orderId,
            tokenType = TokenType.ORDER_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {}
        }

        return result
    }

    // Used by deprecated start() methods
    @VisibleForTesting
    internal suspend fun vaultAsync(
        activity: Activity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

        val launchUri = withContext(Dispatchers.IO) {
            getLaunchUri(
                context = activity.applicationContext,
                token = request.setupTokenId,
                tokenType = TokenType.VAULT_ID,
                fallbackUri = buildPayPalVaultUri(request.setupTokenId)
            )
        }

        val result = payPalWebLauncher.launchWithUrl(
            context = activity,
            uri = launchUri,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {}
        }

        return result
    }
    // endregion

    // region Private Helpers

    /**
     * Whether an app-switch attempt into the PayPal app is worth making.
     *
     * [DeviceInspector.isPayPalInstalled] alone isn't sufficient: the PayPal app can be
     * installed and enabled but still not be the app-switch URI's default handler if the user
     * has unchecked "Open supported links" for it in Android's App Links settings. In that case
     * the OS resolves the app-switch URI to a browser instead of the app, so choosing the
     * app-switch [redirectUrl][CreateShopperSessionWithAppSwitchEligibilityResponse.redirectUrl]
     * over [checkoutFallbackUrl][CreateShopperSessionWithAppSwitchEligibilityResponse.checkoutFallbackUrl]
     * would open a URL that isn't meant to be loaded standalone, landing on an error page.
     *
     * [DeviceInspector.canResolvePayPalAppSwitch] additionally requires the installed PayPal app
     * to meet a minimum supported version — see its doc for details.
     */
    private fun canAttemptPayPalAppSwitch(): Boolean =
        deviceInspector.isPayPalInstalled && deviceInspector.canResolvePayPalAppSwitch()

    /**
     * Decides, at launch time, whether to return to the merchant app via an App Link or via the
     * custom URL scheme fallback, based on whether an App Link return will actually route on this
     * device (@see [GetReturnLinkTypeUseCase]).
     *
     * Falls back to [ReturnToAppStrategy.AppLink] when no [ReturnToAppUrlConfig.fallbackSchemeUrl]
     * was supplied by the merchant, since there is no custom scheme to switch to. The chosen
     * strategy's [ReturnToAppStrategy.linkType] is reported via the `link_type` analytics param.
     */
    private fun resolveReturnLinkStrategy(): ReturnToAppStrategy {
        val appLinkUrl = returnToAppUrlConfig?.returnAppUrl.orEmpty()
        val fallbackSchemeUrl = returnToAppUrlConfig?.fallbackSchemeUrl
        val appLinkReturnUri = appLinkUrl.takeIf { it.isNotBlank() }?.toUri()

        return when (getReturnLinkTypeUseCase(appLinkReturnUri = appLinkReturnUri)) {
            LinkType.APP_LINK -> ReturnToAppStrategy.AppLink(appLinkUrl)

            LinkType.DEEP_LINK ->
                if (!fallbackSchemeUrl.isNullOrBlank()) {
                    ReturnToAppStrategy.CustomUrlScheme(fallbackSchemeUrl)
                } else {
                    ReturnToAppStrategy.AppLink(appLinkUrl)
                }
        }
    }

    /**
     * Drops a trailing '&' (so appendQueryParameter doesn't produce a double separator) and
     * appends the given token as a query param.
     */
    private fun Uri.appendTokenQueryParam(token: String): Uri {
        val tokenType = requireNotNull(sessionTokenType) {
            "sessionTokenType must be set by createPayPalSession() before appendTokenQueryParam() is called."
        }
        val trimmedUri = if (toString().endsWith("&")) {
            toString().dropLast(1).toUri()
        } else {
            this
        }
        val paramName = when (tokenType) {
            TokenType.ORDER_ID -> "token"
            TokenType.VAULT_ID -> "approval_session_id"
            TokenType.BILLING_TOKEN -> "ba_token"
        }
        return trimmedUri.buildUpon()
            .appendQueryParameter(paramName, token)
            .build()
    }

    private fun CreateShopperSessionWithAppSwitchEligibilityResponse.getLaunchUri(token: String): Uri {
        val launchUri = if (appSwitchEnabled) {
            redirectUrl.toUri()
        } else {
            checkoutFallbackUrl.toUri()
        }.appendTokenQueryParam(token)

        val uriWithSessionId = if (shopperSessionConfig.id.isNotBlank()) {
            launchUri.buildUpon()
                .appendQueryParameter("shopperSessionId", shopperSessionConfig.id)
                .build()
        } else {
            launchUri
        }

        return uriWithSessionId.appendObservabilityQueryParams()
    }

    private fun Uri.appendObservabilityQueryParams(): Uri {
        val tokenType = requireNotNull(sessionTokenType) {
            "sessionTokenType must be set by createPayPalSession() before appendObservabilityQueryParams() is called."
        }
        val flowType = when (tokenType) {
            TokenType.ORDER_ID -> "ecs"
            TokenType.VAULT_ID, TokenType.BILLING_TOKEN -> "va"
        }
        return buildUpon()
            .appendQueryParameter("source", "pda")
            .appendQueryParameter("merchant", coreConfig.merchantId)
            .appendQueryParameter("flow_type", flowType)
            .appendQueryParameter("funding_source", PayPalWebCheckoutFundingSource.PAYPAL.value)
            .appendQueryParameter("switch_initiated_time", System.currentTimeMillis().toString())
            .build()
    }

    private fun buildPayPalCheckoutUri(
        orderId: String?,
        funding: PayPalWebCheckoutFundingSource?,
        returnUrl: String?
    ): Uri {
        val uriBuilder = baseUrl.toUri()
            .buildUpon()
            .appendPath("checkoutnow")
            .appendQueryParameter("token", orderId)
            .appendQueryParameter("redirect_uri", returnUrl)
            .appendQueryParameter("native_xo", "1")
            .appendQueryParameter("integration_artifact", UpdateClientConfigAPI.Defaults.INTEGRATION_ARTIFACT)

        funding?.let { uriBuilder.appendQueryParameter("fundingSource", it.value) }
        return uriBuilder.build()
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
            else -> "https://sandbox.paypal.com/"
        }

    private suspend fun getLaunchUri(
        context: Context,
        token: String,
        tokenType: TokenType,
        fallbackUri: Uri
    ): Uri {
        return if (deviceInspector.isPayPalInstalled) {
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

    private fun notifyUserPerceivedLatency(
        flow: String,
        result: PayPalPresentAuthChallengeResult,
        startTime: Long,
        endTime: Long
    ) {
        val presentationType = when (result) {
            is PayPalPresentAuthChallengeResult.Success ->
                if (appSwitchEnabled) PresentationType.APP_SWITCH else PresentationType.BROWSER

            is PayPalPresentAuthChallengeResult.Failure -> PresentationType.ERROR
        }
        analytics.notifyUserPerceivedLatency(flow, presentationType, startTime, endTime)
    }

    private fun notifyUserPerceivedLatencyError(flow: String, startTime: Long) {
        analytics.notifyUserPerceivedLatency(
            flow = flow,
            presentationType = PresentationType.ERROR,
            startTime = startTime,
            endTime = System.currentTimeMillis()
        )
    }

    private fun notifyApiRequestLatency(endpoint: String, roundTripTiming: HttpRoundTripTiming?) {
        roundTripTiming?.let { analytics.notifyApiRequestLatency(endpoint, it.startTime, it.endTime) }
    }

    private fun setShopperSessionAnalyticsParams(
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse?
    ) {
        appSwitchAnalyticsEventParams = appSwitchAnalyticsEventParams.copy(
            shopperSession = shopperSession,
            paypalInstalled = canAttemptPayPalAppSwitch().toString(),
        )
    }

    private fun resetAppSwitchAnalyticsEventParams() {
        appSwitchAnalyticsEventParams = createAppSwitchAnalyticsEventParams()
    }

    private fun createAppSwitchAnalyticsEventParams() = AppSwitchAnalyticsEventParams(
        merchantId = coreConfig.merchantId,
        bnCode = coreConfig.bnCode,
        clientId = coreConfig.clientId,
    )
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
        createShopperSessionAPI = CreateShopperSessionWithAppSwitchEligibilityAPI(
            configuration,
            context.applicationContext,
        ),
        getReturnLinkTypeUseCase = buildReturnLinkTypeUseCase(context.applicationContext),
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
        appSwitchEnabled = false

        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

        val launchUri = buildPayPalCheckoutUri(
            orderId = request.orderId,
            funding = request.fundingSource,
            returnUrl = returnToAppStrategy.returnUrl
        )

        val result = payPalWebLauncher.launchWithUrl(
            context = activity,
            uri = launchUri,
            token = request.orderId,
            tokenType = TokenType.ORDER_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {}
        }
        return result
    }

    /**
     * Confirm PayPal payment source for an order with callback.
     *
     * @param activity The activity to launch the PayPal web checkout from
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval
     * @param callback [PayPalWebStartCallback] to receive the result
     */
    @Deprecated(
        message = "Use createPayPalSession() followed by start(activity, orderId, callback) instead.",
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
        val returnToAppStrategy = resolveReturnToAppStrategy(request.returnToAppStrategy)
            ?: return PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.noReturnToAppStrategyError)

        val launchUri = buildPayPalVaultUri(request.setupTokenId)

        val result = payPalWebLauncher.launchWithUrl(
            context = activity,
            uri = launchUri,
            token = request.setupTokenId,
            tokenType = TokenType.VAULT_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // update auth state value in session store
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {}
        }

        return result
    }

    /**
     * Vault PayPal as a payment method with callback.
     *
     * @param activity the ComponentActivity to launch the auth challenge from
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method
     * @param callback callback to receive the result
     */
    @Deprecated(
        message = "Use createPayPalSession() followed by vault(activity, setupTokenId, callback) instead.",
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
            is PayPalWebCheckoutFinishStartResult.Success -> { /* Do Nothing. Will remove deprecated methods*/ }

            is PayPalWebCheckoutFinishStartResult.Canceled -> { /* Do Nothing. Will remove deprecated methods*/ }

            is PayPalWebCheckoutFinishStartResult.Failure -> { /* Do Nothing. Will remove deprecated methods*/ }

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
            is PayPalWebCheckoutFinishVaultResult.Success -> { /* Do Nothing. Will remove deprecated methods*/ }

            is PayPalWebCheckoutFinishVaultResult.Failure -> { /* Do Nothing. Will remove deprecated methods*/ }

            PayPalWebCheckoutFinishVaultResult.Canceled -> { /* Do Nothing. Will remove deprecated methods*/ }

            PayPalWebCheckoutFinishVaultResult.NoResult -> {
                // no analytics tracking required at the moment
            }
        }
        return result
    }

    // endregion

    private companion object {
        /**
         * Builds a [GetReturnLinkTypeUseCase] wired with its two collaborators from an application
         * [Context], following the SDK's manual dependency-injection convention.
         */
        private fun buildReturnLinkTypeUseCase(applicationContext: Context): GetReturnLinkTypeUseCase {
            val getDefaultAppUseCase = GetDefaultAppUseCase(applicationContext.packageManager)
            return GetReturnLinkTypeUseCase(
                applicationContext = applicationContext,
                deviceInspector = DeviceInspector(applicationContext),
                getDefaultAppUseCase = getDefaultAppUseCase,
                getAppLinksCompatibleBrowserUseCase = GetAppLinksCompatibleBrowserUseCase(getDefaultAppUseCase),
            )
        }
    }
}

private fun PayPalUserAction.toExternalPaymentType(): String = when (this) {
    PayPalUserAction.CONTINUE -> "CONTINUE"
    PayPalUserAction.PAY_NOW -> "PAY"
    PayPalUserAction.SETUP_NOW -> "COMMIT"
}
