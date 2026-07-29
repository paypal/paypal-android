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
import com.paypal.android.corepayments.PayPalSDKError
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
import com.paypal.android.corepayments.usecase.GetReturnToAppStrategyResult
import com.paypal.android.corepayments.usecase.GetReturnToAppStrategyUseCase
import com.paypal.android.paypalwebpayments.analytics.AnalyticsEventParams
import com.paypal.android.paypalwebpayments.analytics.CreatePayPalSessionEvent
import com.paypal.android.paypalwebpayments.analytics.LatencyEndpoint
import com.paypal.android.paypalwebpayments.analytics.LatencyFlow
import com.paypal.android.paypalwebpayments.analytics.PayPalEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.PresentationType
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import com.paypal.android.paypalwebpayments.usecase.GetEffectiveReturnUrlConfigUseCase
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
    "LongParameterList", // Manual constructor injection, one dependency per collaborator (see adr/2)
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
    private val getReturnToAppStrategyUseCase: GetReturnToAppStrategyUseCase,
    private val getEffectiveReturnUrlConfigUseCase: GetEffectiveReturnUrlConfigUseCase,
    private val urlScheme: String? = null,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {

    private var appSwitchEnabled: Boolean = false
    private var analyticsEventParams: AnalyticsEventParams = AnalyticsEventParams()

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
        getReturnToAppStrategyUseCase = GetReturnToAppStrategyUseCase(context.applicationContext),
        getEffectiveReturnUrlConfigUseCase = GetEffectiveReturnUrlConfigUseCase(),
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
        initAnalyticsEventParams()
        sessionTokenType = tokenType
        returnToAppUrlConfig = urlConfig

        // Neither returnAppUrl nor fallbackSchemeUrl is usable — fail fast instead of starting a
        // shopper-session fetch; start()/vault() re-check this to report the error.
        if (!urlConfig.isValid()) {
            shopperSessionDeferred = null
            return
        }

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
        val startTime = System.currentTimeMillis()
        val urlConfig = returnToAppUrlConfig
        val deferred = shopperSessionDeferred
        if (deferred == null) {
            initAnalyticsEventParams()
        }
        analyticsEventParams = analyticsEventParams.copy(
            orderIdOrSetupTokenId = orderId,
            isVault = false,
        )
        if (deferred == null) {
            notifyUserPerceivedLatencyError(LatencyFlow.CHECKOUT, startTime)
            notifyCheckoutSessionNotStarted(callback)
            return
        }
        if (urlConfig != null && !urlConfig.isValid()) {
            notifyUserPerceivedLatencyError(LatencyFlow.CHECKOUT, startTime)
            notifyCheckoutReturnToAppUrlConfigInvalid(orderId, callback)
            return
        }
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                setShopperSessionAnalyticsParams(shopperSession)
                analytics.notify(PayPalEvent.STARTED, params = analyticsEventParams)

                if (shopperSession != null) {
                    val result = launchCheckoutWithShopperSession(
                        activity = activity,
                        shopperSession = shopperSession,
                        orderId = orderId,
                        startTime = startTime,
                    )
                    withContext(Dispatchers.Main) {
                        callback.onPayPalWebStartResult(result)
                    }
                } else {
                    throw PayPalWebCheckoutError.sessionCreationFailedError
                }
            } catch (e: Exception) {
                analytics.notify(
                    event = PayPalEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = e.message
                )
                shopperSessionDeferred = null
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebStartResult(
                        PayPalPresentAuthChallengeResult.Failure(
                            e as? PayPalSDKError ?: PayPalWebCheckoutError.unknownError
                        )
                    )
                }
            }
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
        val urlConfig = returnToAppUrlConfig
        val deferred = shopperSessionDeferred
        if (deferred == null) {
            initAnalyticsEventParams()
        }
        analyticsEventParams = analyticsEventParams.copy(
            orderIdOrSetupTokenId = setupTokenId,
            isVault = true,
        )
        if (deferred == null) {
            notifyUserPerceivedLatencyError(LatencyFlow.VAULT, startTime)
            notifyVaultSessionNotStarted(callback)
            return
        }
        if (urlConfig != null && !urlConfig.isValid()) {
            notifyUserPerceivedLatencyError(LatencyFlow.VAULT, startTime)
            notifyVaultReturnToAppUrlConfigInvalid(setupTokenId, callback)
            return
        }
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                setShopperSessionAnalyticsParams(shopperSession)
                analytics.notify(PayPalEvent.STARTED, params = analyticsEventParams)

                if (shopperSession != null) {
                    val result = launchVaultWithSession(
                        activity = activity,
                        shopperSession = shopperSession,
                        setupTokenId = setupTokenId,
                        startTime = startTime,
                    )
                    withContext(Dispatchers.Main) {
                        callback.onPayPalWebVaultResult(result)
                    }
                } else {
                    throw PayPalWebCheckoutError.sessionCreationFailedError
                }
            } catch (e: Exception) {
                analytics.notify(
                    event = PayPalEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = e.message
                )
                shopperSessionDeferred = null
                withContext(Dispatchers.Main) {
                    callback.onPayPalWebVaultResult(
                        PayPalPresentAuthChallengeResult.Failure(
                            e as? PayPalSDKError ?: PayPalWebCheckoutError.unknownError
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
            analytics.notify(PayPalEvent.HANDLE_RETURN_STARTED, params = analyticsEventParams)
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
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.SUCCEEDED, null)

            is PayPalWebCheckoutFinishStartResult.Canceled ->
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.CANCELED, null)

            is PayPalWebCheckoutFinishStartResult.Failure ->
                Triple(PayPalEvent.HANDLE_RETURN_FAILED, PayPalEvent.FAILED, result.error.errorDescription)

            PayPalWebCheckoutFinishStartResult.NoResult -> return // no analytics tracking required at the moment
        }

        analytics.notify(
            event = handleReturnResult,
            params = analyticsEventParams,
            errorDescription = errorDescription
        )
        analytics.notify(
            event = checkoutResult,
            params = analyticsEventParams,
            errorDescription = errorDescription
        )
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
            analytics.notify(PayPalEvent.HANDLE_RETURN_STARTED, params = analyticsEventParams)
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
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.SUCCEEDED, null)

            PayPalWebCheckoutFinishVaultResult.Canceled ->
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.CANCELED, null)

            is PayPalWebCheckoutFinishVaultResult.Failure ->
                Triple(PayPalEvent.HANDLE_RETURN_FAILED, PayPalEvent.FAILED, result.error.errorDescription)

            PayPalWebCheckoutFinishVaultResult.NoResult -> return // no analytics tracking required at the moment
        }

        analytics.notify(
            event = handleReturnResult,
            params = analyticsEventParams,
            errorDescription = errorDescription
        )
        analytics.notify(
            event = vaultResult,
            params = analyticsEventParams,
            errorDescription = errorDescription
        )
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
        // Shouldn't return null in practice: start() already validates urlConfig before reaching here.
        val returnToAppStrategy = getReturnToAppStrategyOrNull()
            ?: return handleReturnToAppStrategyFailure(startTime = startTime, isVault = false)
        appSwitchEnabled = shopperSession.appSwitchEligible && canAttemptPayPalAppSwitch()
        analyticsEventParams = analyticsEventParams.copy(appSwitchEnabled = appSwitchEnabled, linkType = returnToAppStrategy.linkType)
        val launchUri = shopperSession.getLaunchUri(orderId)
        if (appSwitchEnabled) {
            analyticsEventParams = analyticsEventParams.copy(appSwitchUrl = launchUri.toString())
            analytics.notify(PayPalEvent.APP_SWITCH_STARTED, params = analyticsEventParams)
        } else {
            analytics.notify(PayPalEvent.AUTH_CHALLENGE_PRESENTATION_STARTED, params = analyticsEventParams)
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
                    PayPalEvent.APP_SWITCH_SUCCEEDED
                } else {
                    PayPalEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED
                }
                analytics.notify(event, params = analyticsEventParams)
                sessionStore.authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                val event = if (appSwitchEnabled) {
                    PayPalEvent.APP_SWITCH_FAILED
                } else {
                    PayPalEvent.AUTH_CHALLENGE_PRESENTATION_FAILED
                }
                analytics.notify(
                    event,
                    params = analyticsEventParams,
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
        // Shouldn't return null in practice: vault() already validates urlConfig before reaching here.
        val returnToAppStrategy = getReturnToAppStrategyOrNull()
            ?: return handleReturnToAppStrategyFailure(startTime = startTime, isVault = true)
        appSwitchEnabled = shopperSession.appSwitchEligible && canAttemptPayPalAppSwitch()
        analyticsEventParams = analyticsEventParams.copy(appSwitchEnabled = appSwitchEnabled, linkType = returnToAppStrategy.linkType)
        val launchUri = shopperSession.getLaunchUri(setupTokenId)

        if (appSwitchEnabled) {
            analyticsEventParams = analyticsEventParams.copy(appSwitchUrl = launchUri.toString())
            analytics.notify(PayPalEvent.APP_SWITCH_STARTED, params = analyticsEventParams)
        } else {
            analytics.notify(PayPalEvent.AUTH_CHALLENGE_PRESENTATION_STARTED, params = analyticsEventParams)
        }
        val endTime = System.currentTimeMillis()

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
                    PayPalEvent.APP_SWITCH_SUCCEEDED
                } else {
                    PayPalEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED
                }
                analytics.notify(event, params = analyticsEventParams)
                sessionStore.authState = result.authState
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                val event = if (appSwitchEnabled) {
                    PayPalEvent.APP_SWITCH_FAILED
                } else {
                    PayPalEvent.AUTH_CHALLENGE_PRESENTATION_FAILED
                }
                analytics.notify(
                    event,
                    params = analyticsEventParams,
                    errorDescription = result.error.errorDescription
                )
            }
        }
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

        val returnToAppStrategy = resolveShopperSessionReturnToAppStrategy(urlConfig) ?: return null
        val effectiveUrlConfig = getEffectiveReturnUrlConfigUseCase(
            urlConfig = urlConfig,
            linkType = returnToAppStrategy.linkType
        )

        analyticsEventParams = analyticsEventParams.copy(
            isCachedSession = userIdentity?.existingPayPalSessionId != null,
            userActionValue = userAction.toExternalPaymentType(),
            isVault = isVaultRequest,
            merchantId = coreConfig.merchantId,
            bnCode = coreConfig.bnCode,
            clientId = coreConfig.clientId,
            paypalInstalled = canAttemptPayPalAppSwitch().toString(),
            returnAppUrl = effectiveUrlConfig.returnAppUrl,
            cancelAppUrl = effectiveUrlConfig.cancelAppUrl,
            fallbackSchemeUrl = effectiveUrlConfig.fallbackSchemeUrl,
            linkType = returnToAppStrategy.linkType,
        )
        analytics.notify(CreatePayPalSessionEvent.STARTED, params = analyticsEventParams)

        val result = createShopperSessionAPI(
            token = token,
            tokenType = tokenType,
            params = CreateShopperSessionWithAppSwitchEligibilityParams(
                returnAppUrl = effectiveUrlConfig.returnAppUrl,
                cancelAppUrl = effectiveUrlConfig.cancelAppUrl,
                fallbackSchemeUrl = effectiveUrlConfig.fallbackSchemeUrl,
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
                analyticsEventParams = analyticsEventParams.copy(
                    shopperSession = result.data,
                )
                analytics.notify(CreatePayPalSessionEvent.SUCCEEDED, params = analyticsEventParams)
                result.data
            }
            // Session creation failure / network timeout: fall back to patchCCO.
            is APIResult.Failure -> {
                analytics.notify(
                    CreatePayPalSessionEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = result.error.errorDescription,
                )
                null
            }
        }
    }

    /**
     * Resolves the [ReturnToAppStrategy] to use for the shopper session request, or `null` when
     * [GetReturnToAppStrategyUseCase] returns a [GetReturnToAppStrategyResult.Failure] — this
     * shouldn't happen in practice, since [createPayPalSession] already validates [urlConfig]
     * before starting this fetch. Reports [CreatePayPalSessionEvent.FAILED] on failure.
     */
    private fun resolveShopperSessionReturnToAppStrategy(
        urlConfig: ReturnToAppUrlConfig,
    ): ReturnToAppStrategy? {
        return when (
            val result = getReturnToAppStrategyUseCase(
                appLinkReturnUrl = urlConfig.returnAppUrl,
                fallbackSchemeUrl = urlConfig.fallbackSchemeUrl,
            )
        ) {
            is GetReturnToAppStrategyResult.Success -> result.returnToAppStrategy
            is GetReturnToAppStrategyResult.Failure -> {
                analytics.notify(
                    CreatePayPalSessionEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = result.error,
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
     * would open a URL that isn't meant to be loaded standalone, landing on an error page. This
     * mirrors the `deviceInspector.isPayPalInstalled() && resolvePayPalUseCase()` guard used by
     * the Braintree Android SDK.
     *
     * [DeviceInspector.canResolvePayPalAppSwitch] additionally requires the installed PayPal app
     * to meet a minimum supported version — see its doc for details.
     */
    private fun canAttemptPayPalAppSwitch(): Boolean =
        deviceInspector.isPayPalInstalled && deviceInspector.canResolvePayPalAppSwitch()

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
        analyticsEventParams = analyticsEventParams.copy(
            shopperSession = shopperSession,
            paypalInstalled = canAttemptPayPalAppSwitch().toString(),
        )
    }

    private fun initAnalyticsEventParams() {
        analyticsEventParams = AnalyticsEventParams(
            merchantId = coreConfig.merchantId,
            bnCode = coreConfig.bnCode,
            clientId = coreConfig.clientId,
        )
    }

    /**
     * True when this config has a usable App Link return URL or a fallback scheme, so there is a
     * way to return to the merchant app after checkout/vault.
     */
    private fun ReturnToAppUrlConfig.isValid(): Boolean =
        returnAppUrl.isNotBlank() || fallbackSchemeUrl.isNotBlank()

    private fun notifyCheckoutSessionNotStarted(callback: PayPalWebStartCallback) {
        applicationScope.launch(Dispatchers.Main) {
            analytics.notify(
                PayPalEvent.SESSION_NOT_STARTED,
                params = analyticsEventParams,
                errorDescription = "startPayPalSession() must be called before start()."
            )
            callback.onPayPalWebStartResult(
                PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.sessionNotCreatedError)
            )
        }
    }

    private fun notifyVaultSessionNotStarted(callback: PayPalWebVaultCallback) {
        applicationScope.launch(Dispatchers.Main) {
            analytics.notify(
                PayPalEvent.SESSION_NOT_STARTED,
                params = analyticsEventParams,
                errorDescription = "startPayPalSession() must be called before vault()."
            )
            callback.onPayPalWebVaultResult(
                PayPalPresentAuthChallengeResult.Failure(PayPalWebCheckoutError.sessionNotCreatedError)
            )
        }
    }

    private fun notifyCheckoutReturnToAppUrlConfigInvalid(orderId: String, callback: PayPalWebStartCallback) {
        applicationScope.launch(Dispatchers.Main) {
            val error = PayPalWebCheckoutError.returnToAppUrlConfigMissingError
            analytics.notify(
                PayPalEvent.FAILED,
                params = analyticsEventParams.copy(orderIdOrSetupTokenId = orderId),
                errorDescription = error.errorDescription
            )
            callback.onPayPalWebStartResult(PayPalPresentAuthChallengeResult.Failure(error))
        }
    }

    private fun notifyVaultReturnToAppUrlConfigInvalid(setupTokenId: String, callback: PayPalWebVaultCallback) {
        applicationScope.launch(Dispatchers.Main) {
            val error = PayPalWebCheckoutError.returnToAppUrlConfigMissingError
            analytics.notify(
                PayPalEvent.FAILED,
                params = analyticsEventParams.copy(orderIdOrSetupTokenId = setupTokenId),
                errorDescription = error.errorDescription
            )
            callback.onPayPalWebVaultResult(PayPalPresentAuthChallengeResult.Failure(error))
        }
    }

    /**
     * Builds and logs the [PayPalPresentAuthChallengeResult.Failure] used when
     * [GetReturnToAppStrategyUseCase] fails. This shouldn't happen in practice, since start()/vault()
     * already validate the [ReturnToAppUrlConfig] before checkout/vault launch is attempted.
     *
     * @param startTime Used for latency reporting.
     * @param isVault Selects the vault or checkout flow: which [LatencyFlow] to report latency
     *   against, and whether the failure is logged via [logVaultPresentAuthChallengeResult] or
     *   [logCheckoutPresentAuthChallengeResult].
     */
    private fun handleReturnToAppStrategyFailure(
        startTime: Long,
        isVault: Boolean
    ): PayPalPresentAuthChallengeResult {
        val error = PayPalWebCheckoutError.returnToAppUrlConfigMissingError
        val failureResult = PayPalPresentAuthChallengeResult.Failure(error)
        if (isVault) {
            logVaultPresentAuthChallengeResult(failureResult)
        } else {
            logCheckoutPresentAuthChallengeResult(failureResult)
        }
        val latencyFlow = if (isVault) LatencyFlow.VAULT else LatencyFlow.CHECKOUT
        notifyUserPerceivedLatency(latencyFlow, failureResult, startTime, System.currentTimeMillis())
        return failureResult
    }

    /**
     * Resolves the [ReturnToAppStrategy] to use for launching checkout/vault, or `null` when it
     * can't be resolved — either because [returnToAppUrlConfig] hasn't been set, or because
     * [GetReturnToAppStrategyUseCase] itself returned a [GetReturnToAppStrategyResult.Failure].
     * Callers should treat a `null` result as a failure via [handleReturnToAppStrategyFailure].
     */
    private fun getReturnToAppStrategyOrNull(): ReturnToAppStrategy? {
        val urlConfig = returnToAppUrlConfig ?: return null
        return when (
            val result = getReturnToAppStrategyUseCase(
                appLinkReturnUrl = urlConfig.returnAppUrl,
                fallbackSchemeUrl = urlConfig.fallbackSchemeUrl,
            )
        ) {
            is GetReturnToAppStrategyResult.Success -> result.returnToAppStrategy
            is GetReturnToAppStrategyResult.Failure -> null
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
        createShopperSessionAPI = CreateShopperSessionWithAppSwitchEligibilityAPI(
            configuration,
            context.applicationContext,
        ),
        getReturnToAppStrategyUseCase = GetReturnToAppStrategyUseCase(context.applicationContext),
        getEffectiveReturnUrlConfigUseCase = GetEffectiveReturnUrlConfigUseCase(),
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
}

private fun PayPalUserAction.toExternalPaymentType(): String = when (this) {
    PayPalUserAction.CONTINUE -> "CONTINUE"
    PayPalUserAction.PAY_NOW -> "PAY"
    PayPalUserAction.SETUP_NOW -> "COMMIT"
}
