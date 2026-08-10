package com.paypal.android.paypalpayments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.HttpRoundTripTiming
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.analytics.AnalyticsService
import com.paypal.android.corepayments.api.CreateShopperSessionWithAppSwitchEligibilityAPI
import com.paypal.android.corepayments.common.DeviceInspector
import com.paypal.android.corepayments.linkType
import com.paypal.android.corepayments.model.APIResult
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityResponse
import com.paypal.android.corepayments.model.CreateShopperSessionWithAppSwitchEligibilityParams
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.usecase.GetReturnToAppStrategyResult
import com.paypal.android.corepayments.usecase.GetReturnToAppStrategyUseCase
import com.paypal.android.paypalpayments.analytics.AnalyticsEventParams
import com.paypal.android.paypalpayments.analytics.CreatePayPalSessionEvent
import com.paypal.android.paypalpayments.analytics.LatencyEndpoint
import com.paypal.android.paypalpayments.analytics.LatencyFlow
import com.paypal.android.paypalpayments.analytics.PayPalEvent
import com.paypal.android.paypalpayments.analytics.PayPalAnalytics
import com.paypal.android.paypalpayments.analytics.PresentationType
import com.paypal.android.paypalpayments.errors.PayPalError
import com.paypal.android.paypalpayments.usecase.GetEffectiveReturnUrlConfigUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Use this client to approve an order using PayPal web checkout.
 */
@Suppress(
    "LongParameterList", // Manual constructor injection, one dependency per collaborator (see adr/2)
    "LargeClass", // Handles start, vault, and shopper-session orchestration for both app-switch link types
)
class PayPalClient internal constructor(
    private val analytics: PayPalAnalytics,
    private val payPalLauncher: PayPalLauncher,
    private val sessionStore: PayPalSessionStore,
    private val deviceInspector: DeviceInspector,
    private val coreConfig: CoreConfig,
    private val createShopperSessionAPI: CreateShopperSessionWithAppSwitchEligibilityAPI,
    private val getReturnToAppStrategyUseCase: GetReturnToAppStrategyUseCase,
    private val getEffectiveReturnUrlConfigUseCase: GetEffectiveReturnUrlConfigUseCase,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {

    private var appSwitchEnabled: Boolean = false
    private var analyticsEventParams: AnalyticsEventParams = AnalyticsEventParams()
    private val finishResultLock = Any()

    // Shopper Session id (v3) — set by createPayPalSession(), awaited by start() / vault()
    @VisibleForTesting
    internal var shopperSessionDeferred: Deferred<CreateShopperSessionWithAppSwitchEligibilityResponse?>? = null
    private var returnToAppUrlConfig: ReturnToAppUrlConfig? = null
    private var sessionTokenType: TokenType? = null

    constructor(
        context: Context,
        configuration: CoreConfig
    ) : this(
        analytics = PayPalAnalytics(AnalyticsService(context.applicationContext, configuration)),
        payPalLauncher = PayPalLauncher(context),
        sessionStore = PayPalSessionStore(),
        deviceInspector = DeviceInspector(context),
        coreConfig = configuration,
        createShopperSessionAPI = CreateShopperSessionWithAppSwitchEligibilityAPI(
            configuration,
            context.applicationContext,
        ),
        getReturnToAppStrategyUseCase = GetReturnToAppStrategyUseCase(context.applicationContext),
        getEffectiveReturnUrlConfigUseCase = GetEffectiveReturnUrlConfigUseCase(),
    )

    // region Public API & Core Flow

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
        callback: PayPalResultCallback,
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
        if (urlConfig != null && !urlConfig.isValid()) {
            notifyCheckoutReturnToAppUrlConfigInvalid(orderId, callback, startTime)
            return
        }
        if (deferred == null) {
            notifyCheckoutSessionNotStarted(callback, startTime)
            return
        }
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                analytics.notify(PayPalEvent.STARTED, params = analyticsEventParams)

                if (shopperSession != null) {
                    val result = launchCheckout(
                        activity = activity,
                        shopperSession = shopperSession,
                        orderId = orderId,
                        startTime = startTime,
                    )
                    withContext(Dispatchers.Main) {
                        callback.onPayPalResult(result)
                    }
                } else {
                    throw PayPalError.sessionCreationFailedError
                }
            } catch (e: Exception) {
                analytics.notify(
                    event = PayPalEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = e.message
                )
                shopperSessionDeferred = null
                withContext(Dispatchers.Main) {
                    callback.onPayPalResult(
                        PayPalPresentAuthChallengeResult.Failure(
                            e as? PayPalSDKError ?: PayPalError.unknownError
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
        callback: PayPalResultCallback,
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
        if (urlConfig != null && !urlConfig.isValid()) {
            notifyVaultReturnToAppUrlConfigInvalid(setupTokenId, callback, startTime)
            return
        }
        if (deferred == null) {
            notifyVaultSessionNotStarted(callback, startTime)
            return
        }
        applicationScope.launch {
            try {
                val shopperSession = deferred.await()
                shopperSessionDeferred = null
                analytics.notify(PayPalEvent.STARTED, params = analyticsEventParams)

                if (shopperSession != null) {
                    val result = launchVault(
                        activity = activity,
                        shopperSession = shopperSession,
                        setupTokenId = setupTokenId,
                        startTime = startTime,
                    )
                    withContext(Dispatchers.Main) {
                        callback.onPayPalResult(result)
                    }
                } else {
                    throw PayPalError.sessionCreationFailedError
                }
            } catch (e: Exception) {
                analytics.notify(
                    event = PayPalEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = e.message
                )
                shopperSessionDeferred = null
                withContext(Dispatchers.Main) {
                    callback.onPayPalResult(
                        PayPalPresentAuthChallengeResult.Failure(
                            e as? PayPalSDKError ?: PayPalError.unknownError
                        )
                    )
                }
            }
        }
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalClient.start]), call this method to see if a user has
     * successfully authorized a PayPal account as a payment source.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishStart(intent: Intent): PayPalFinishStartResult? = synchronized(finishResultLock) {
        val authState = sessionStore.authState ?: return@synchronized null
        val analyticsParams = analyticsEventParams
        analytics.notify(PayPalEvent.HANDLE_RETURN_STARTED, params = analyticsParams)
        val result = payPalLauncher.completeCheckoutAuthRequest(intent, authState)
        if (result != PayPalFinishStartResult.NoResult && clearAuthState(authState)) {
            logCheckoutResult(result, analyticsParams)
        }
        result
    }

    /**
     * After a merchant app has re-entered the foreground following an auth challenge
     * (@see [PayPalClient.vault]), call this method to see if a user has
     * successfully authorized a PayPal account for vaulting.
     *
     * @param [intent] An Android intent that holds the deep link put the merchant app
     * back into the foreground after an auth challenge.
     */
    fun finishVault(intent: Intent): PayPalFinishVaultResult? = synchronized(finishResultLock) {
        val authState = sessionStore.authState ?: return@synchronized null
        val analyticsParams = analyticsEventParams
        analytics.notify(PayPalEvent.HANDLE_RETURN_STARTED, params = analyticsParams)
        val result = payPalLauncher.completeVaultAuthRequest(intent, authState)
        if (result != PayPalFinishVaultResult.NoResult && clearAuthState(authState)) {
            logVaultResult(result, analyticsParams)
        }
        result
    }

    private fun clearAuthState(authState: String): Boolean = synchronized(finishResultLock) {
        if (sessionStore.authState == authState) {
            sessionStore.clear()
            true
        } else {
            false
        }
    }

    private fun publishAuthState(authState: String) = synchronized(finishResultLock) {
        sessionStore.authState = authState
    }

    /**
     * Launches the PayPal checkout UI after the shopper session has been resolved. @see [launch].
     */
    private suspend fun launchCheckout(
        activity: Activity,
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse,
        orderId: String,
        startTime: Long,
    ): PayPalPresentAuthChallengeResult =
        launch(activity, shopperSession, orderId, TokenType.ORDER_ID, startTime)

    /**
     * Launches the PayPal vault UI after the shopper session has been resolved. @see [launch].
     */
    private suspend fun launchVault(
        activity: Activity,
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse,
        setupTokenId: String,
        startTime: Long,
    ): PayPalPresentAuthChallengeResult =
        launch(activity, shopperSession, setupTokenId, TokenType.VAULT_ID, startTime)

    /**
     * Launches the PayPal checkout/vault UI after the shopper session has been resolved.
     *
     * Attempts a PayPal app switch (App Link) if the PayPal app is installed and eligible;
     * otherwise falls back to Chrome Custom Tabs.
     *
     * @param activity The Activity needed to launch the checkout/vault UI.
     * @param shopperSession The resolved shopper session containing launch URLs and eligibility.
     * @param token The order id (checkout) or setup token id (vault) to approve.
     * @param tokenType Whether this is a checkout ([TokenType.ORDER_ID]) or vault
     *   ([TokenType.VAULT_ID]) launch — determines the [LatencyFlow] reported for latency.
     */
    private suspend fun launch(
        activity: Activity,
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse,
        token: String,
        tokenType: TokenType,
        startTime: Long,
    ): PayPalPresentAuthChallengeResult {
        val isVault = tokenType == TokenType.VAULT_ID

        // Shouldn't return null in practice: start()/vault() already validate urlConfig before here.
        val returnToAppStrategy = getReturnToAppStrategyOrNull()
            ?: return handleReturnToAppStrategyFailure(startTime = startTime, isVault = isVault)
        appSwitchEnabled = shopperSession.appSwitchEligible && canAttemptPayPalAppSwitch()
        analyticsEventParams = analyticsEventParams.copy(
            appSwitchEnabled = appSwitchEnabled,
            linkType = returnToAppStrategy.linkType,
        )
        val launchUri = shopperSession.getLaunchUri(token)
        if (appSwitchEnabled) {
            analyticsEventParams = analyticsEventParams.copy(appSwitchUrl = launchUri.toString())
            analytics.notify(PayPalEvent.APP_SWITCH_STARTED, params = analyticsEventParams)
        } else {
            analytics.notify(PayPalEvent.AUTH_CHALLENGE_PRESENTATION_STARTED, params = analyticsEventParams)
        }
        val endTime = System.currentTimeMillis()

        val result = if (appSwitchEnabled) {
            payPalLauncher.launchWithUrl(
                context = activity,
                uri = launchUri,
                token = token,
                tokenType = tokenType,
                returnToAppStrategy = returnToAppStrategy,
                onAuthStateCreated = ::publishAuthState,
                onLaunchFailed = ::clearAuthState,
            )
        } else {
            val effectiveCancelUrl = returnToAppUrlConfig?.let {
                getEffectiveReturnUrlConfigUseCase(it, returnToAppStrategy.linkType).cancelAppUrl
            }.orEmpty()
            payPalLauncher.launchWithUrlAndSessionTracking(
                context = activity,
                uri = launchUri,
                token = token,
                tokenType = tokenType,
                returnToAppStrategy = returnToAppStrategy,
                cancelUrl = effectiveCancelUrl,
                onAuthStateCreated = ::publishAuthState,
                onLaunchFailed = ::clearAuthState,
            )
        }
        logPresentAuthChallengeResult(result, isVault, startTime, endTime)
        return result
    }

    /**
     * Creates the shopper session used to launch checkout/vault.
     *
     * @param token The order id (checkout) or setup token id (vault) to request a session for.
     * @param tokenType Whether this is a checkout ([TokenType.ORDER_ID]) or vault
     *   ([TokenType.VAULT_ID]) session request.
     * @param urlConfig Return-to-app URLs used after checkout/vault completes or is canceled.
     * @param userIdentity Shopper identity used to pre-identify the payer, if available.
     * @param userAction Controls the call-to-action label on the PayPal checkout page.
     * @return The resolved session response, or `null` if the return-to-app strategy couldn't be
     *   resolved or the session request itself failed.
     */
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

        val shopperSessionWithAppSwitchEligibility = when (result) {
            is APIResult.Success -> {
                setShopperSessionAnalyticsParams(result.data)
                analyticsEventParams = analyticsEventParams.copy(
                    shopperSession = result.data,
                )
                analytics.notify(CreatePayPalSessionEvent.SUCCEEDED, params = analyticsEventParams)
                result.data
            }
            // Session creation failed (or timed out); start()/vault() report this as a failure.
            is APIResult.Failure -> {
                analytics.notify(
                    CreatePayPalSessionEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = result.error.errorDescription,
                )
                null
            }
        }
        logApiRequestLatency(LatencyEndpoint.CREATE_SESSION, result.roundTripTiming)
        return shopperSessionWithAppSwitchEligibility
    }
    // endregion

    // region Private Helpers
    private fun initAnalyticsEventParams() {
        analyticsEventParams = AnalyticsEventParams(
            merchantId = coreConfig.merchantId,
            bnCode = coreConfig.bnCode,
            clientId = coreConfig.clientId,
        )
    }

    private fun setShopperSessionAnalyticsParams(
        shopperSession: CreateShopperSessionWithAppSwitchEligibilityResponse?
    ) {
        analyticsEventParams = analyticsEventParams.copy(
            shopperSession = shopperSession,
            paypalInstalled = canAttemptPayPalAppSwitch().toString(),
        )
    }

    /**
     * Whether an app-switch attempt into the PayPal app is worth making — requires not just that
     * the app is installed, but that it will actually resolve the app-switch URI
     */
    private fun canAttemptPayPalAppSwitch(): Boolean =
        deviceInspector.isPayPalInstalled && deviceInspector.canResolvePayPalAppSwitch()

    private fun CreateShopperSessionWithAppSwitchEligibilityResponse.getLaunchUri(token: String): Uri {
        val launchUrl = if (appSwitchEnabled) redirectUrl else checkoutFallbackUrl
        // Drop a trailing '&' so appendQueryParameter doesn't produce a double separator.
        val baseUri = launchUrl.removeSuffix("&").toUri()
        return baseUri.buildUpon().apply {
            appendTokenQueryParam(token)
            appendShopperSessionIdQueryParam(shopperSessionConfig.id)
            appendObservabilityQueryParams()
        }.build()
    }

    /**
     * Appends the given token as a query param.
     */
    private fun Uri.Builder.appendTokenQueryParam(token: String) {
        val tokenType = requireNotNull(sessionTokenType) {
            "sessionTokenType must be set by createPayPalSession() before appendTokenQueryParam() is called."
        }
        val paramName = when (tokenType) {
            TokenType.ORDER_ID -> "token"
            TokenType.VAULT_ID -> "approval_session_id"
            TokenType.BILLING_TOKEN -> "ba_token"
        }
        appendQueryParameter(paramName, token)
    }

    /**
     * Appends the given shopperSessionId as a query param, if it's not blank
     */
    private fun Uri.Builder.appendShopperSessionIdQueryParam(shopperSessionId: String) {
        if (shopperSessionId.isNotBlank()) {
            appendQueryParameter("shopperSessionId", shopperSessionId)
        }
    }

    /**
     * Appends query params that are required for PayPal observability
     */
    private fun Uri.Builder.appendObservabilityQueryParams() {
        val tokenType = requireNotNull(sessionTokenType) {
            "sessionTokenType must be set by createPayPalSession() before appendObservabilityQueryParams() is called."
        }
        val flowType = when (tokenType) {
            TokenType.ORDER_ID -> "ecs"
            TokenType.VAULT_ID, TokenType.BILLING_TOKEN -> "va"
        }
        appendQueryParameter("source", "pda")
        appendQueryParameter("merchant", coreConfig.merchantId)
        appendQueryParameter("flow_type", flowType)
        appendQueryParameter("switch_initiated_time", System.currentTimeMillis().toString())
    }

    /**
     * True when this config has a usable App Link return URL or a fallback scheme, so there is a
     * way to return to the merchant app after checkout/vault.
     */
    private fun ReturnToAppUrlConfig.isValid(): Boolean =
        returnAppUrl.isNotBlank() || fallbackSchemeUrl.isNotBlank()

    /**
     * Builds and logs the [PayPalPresentAuthChallengeResult.Failure] used when
     * [GetReturnToAppStrategyUseCase] fails. This shouldn't happen in practice, since start()/vault()
     * already validate the [ReturnToAppUrlConfig] before checkout/vault launch is attempted.
     *
     * @param startTime Used for latency reporting.
     * @param isVault Selects the vault or checkout flow: which [LatencyFlow] to report latency
     *   against, and whether the failure is logged via [logPresentAuthChallengeResult]
     */
    private fun handleReturnToAppStrategyFailure(
        startTime: Long,
        isVault: Boolean
    ): PayPalPresentAuthChallengeResult {
        val error = PayPalError.returnToAppUrlConfigMissingError
        val failureResult = PayPalPresentAuthChallengeResult.Failure(error)
        logPresentAuthChallengeResult(failureResult, isVault, startTime, System.currentTimeMillis())
        return failureResult
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

    private fun PayPalUserAction.toExternalPaymentType(): String = when (this) {
        PayPalUserAction.CONTINUE -> "CONTINUE"
        PayPalUserAction.PAY_NOW -> "PAY"
        PayPalUserAction.SETUP_NOW -> "COMMIT"
    }
    // endregion

    // region analytics methods
    /**
     * Logs the handle-return event and the corresponding checkout outcome event for a [finishStart] result.
     */
    private fun logCheckoutResult(
        result: PayPalFinishStartResult,
        analyticsParams: AnalyticsEventParams,
    ) {
        val (handleReturnResult, checkoutResult, errorDescription) = when (result) {
            is PayPalFinishStartResult.Success ->
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.SUCCEEDED, null)

            is PayPalFinishStartResult.Canceled ->
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.CANCELED, null)

            is PayPalFinishStartResult.Failure ->
                Triple(PayPalEvent.HANDLE_RETURN_FAILED, PayPalEvent.FAILED, result.error.errorDescription)

            PayPalFinishStartResult.NoResult -> return // no analytics tracking required at the moment
        }

        analytics.notify(
            event = handleReturnResult,
            params = analyticsParams,
            errorDescription = errorDescription
        )
        analytics.notify(
            event = checkoutResult,
            params = analyticsParams,
            errorDescription = errorDescription
        )
    }

    /**
     * Logs the handle-return event and the corresponding vault outcome event for a [finishVault] result.
     */
    private fun logVaultResult(
        result: PayPalFinishVaultResult,
        analyticsParams: AnalyticsEventParams,
    ) {
        val (handleReturnResult, vaultResult, errorDescription) = when (result) {
            is PayPalFinishVaultResult.Success ->
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.SUCCEEDED, null)

            PayPalFinishVaultResult.Canceled ->
                Triple(PayPalEvent.HANDLE_RETURN_SUCCEEDED, PayPalEvent.CANCELED, null)

            is PayPalFinishVaultResult.Failure ->
                Triple(PayPalEvent.HANDLE_RETURN_FAILED, PayPalEvent.FAILED, result.error.errorDescription)

            PayPalFinishVaultResult.NoResult -> return // no analytics tracking required at the moment
        }

        analytics.notify(
            event = handleReturnResult,
            params = analyticsParams,
            errorDescription = errorDescription
        )
        analytics.notify(
            event = vaultResult,
            params = analyticsParams,
            errorDescription = errorDescription
        )
    }

    /**
     * Logs the app-switch/auth-challenge success or failure event for a launch [result].
     */
    private fun logPresentAuthChallengeResult(
        result: PayPalPresentAuthChallengeResult,
        isVault: Boolean,
        startTime: Long,
        endTime: Long
    ) {
        val flowType = if (isVault) LatencyFlow.VAULT else LatencyFlow.CHECKOUT
        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                val event = if (appSwitchEnabled) {
                    PayPalEvent.APP_SWITCH_SUCCEEDED
                } else {
                    PayPalEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED
                }
                analytics.notify(event, params = analyticsEventParams)
                logUserPerceivedLatency(flowType, result, startTime, endTime)
            }
            is PayPalPresentAuthChallengeResult.Failure -> {
                val event = if (appSwitchEnabled) {
                    PayPalEvent.APP_SWITCH_FAILED
                } else {
                    PayPalEvent.AUTH_CHALLENGE_PRESENTATION_FAILED
                }
                val errorDescription = result.error.errorDescription
                analytics.notify(
                    event,
                    params = analyticsEventParams,
                    errorDescription = errorDescription
                )
                analytics.notify(
                    PayPalEvent.FAILED,
                    params = analyticsEventParams,
                    errorDescription = errorDescription
                )
                logUserPerceivedLatencyError(flowType, startTime, errorDescription, endTime)
            }
        }
    }

    /**
     * Logs the user-perceived latency for a completed launch [result].
     */
    private fun logUserPerceivedLatency(
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
        analytics.notifyUserPerceivedLatency(flow, presentationType, startTime, endTime, params = analyticsEventParams)
    }

    /**
     * Logs the user-perceived latency for a launch that failed before a [PayPalPresentAuthChallengeResult] was reached.
     */
    private fun logUserPerceivedLatencyError(
        flow: String,
        startTime: Long,
        errorDescription: String,
        endTime: Long = System.currentTimeMillis(),
    ) {
        analytics.notifyUserPerceivedLatency(
            flow = flow,
            presentationType = PresentationType.ERROR,
            startTime = startTime,
            endTime = endTime,
            errorDescription = errorDescription,
            params = analyticsEventParams,
        )
    }

    /**
     * Logs the API round-trip latency for [endpoint], if [roundTripTiming] was captured.
     */
    private fun logApiRequestLatency(endpoint: String, roundTripTiming: HttpRoundTripTiming?) {
        roundTripTiming?.let {
            analytics.notifyApiRequestLatency(endpoint, it.startTime, it.endTime, analyticsEventParams)
        }
    }

    /**
     * Logs [PayPalEvent.SESSION_NOT_STARTED] and notifies the merchant that [start] was called
     * before [createPayPalSession].
     */
    private fun notifyCheckoutSessionNotStarted(callback: PayPalResultCallback, startTime: Long) {
        val errorDescription = "startPayPalSession() must be called before start()."
        applicationScope.launch(Dispatchers.Main) {
            analytics.notify(
                PayPalEvent.SESSION_NOT_STARTED,
                params = analyticsEventParams,
                errorDescription = errorDescription
            )
            logUserPerceivedLatencyError(LatencyFlow.CHECKOUT, startTime, errorDescription)
            callback.onPayPalResult(
                PayPalPresentAuthChallengeResult.Failure(PayPalError.sessionNotCreatedError)
            )
        }
    }

    /**
     * Logs [PayPalEvent.SESSION_NOT_STARTED] and notifies the merchant that [vault] was called
     * before [createPayPalSession].
     */
    private fun notifyVaultSessionNotStarted(callback: PayPalResultCallback, startTime: Long) {
        val errorDescription = "startPayPalSession() must be called before vault()."
        applicationScope.launch(Dispatchers.Main) {
            analytics.notify(
                PayPalEvent.SESSION_NOT_STARTED,
                params = analyticsEventParams,
                errorDescription = errorDescription
            )
            logUserPerceivedLatencyError(LatencyFlow.VAULT, startTime, errorDescription)
            callback.onPayPalResult(
                PayPalPresentAuthChallengeResult.Failure(PayPalError.sessionNotCreatedError)
            )
        }
    }

    /**
     * Logs [PayPalEvent.FAILED] and notifies the merchant that [start] was called with a [returnToAppUrlConfig]
     * that has neither a usable return app URL nor a fallback scheme.
     */
    private fun notifyCheckoutReturnToAppUrlConfigInvalid(
        orderId: String,
        callback: PayPalResultCallback,
        startTime: Long
    ) {
        applicationScope.launch(Dispatchers.Main) {
            val error = PayPalError.returnToAppUrlConfigMissingError
            analytics.notify(
                PayPalEvent.FAILED,
                params = analyticsEventParams.copy(orderIdOrSetupTokenId = orderId),
                errorDescription = error.errorDescription
            )
            logUserPerceivedLatencyError(LatencyFlow.CHECKOUT, startTime, error.errorDescription)
            callback.onPayPalResult(PayPalPresentAuthChallengeResult.Failure(error))
        }
    }

    /**
     * Logs [PayPalEvent.FAILED] and notifies the merchant that [vault] was called with a [returnToAppUrlConfig]
     * that has neither a usable return app URL nor a fallback scheme.
     */
    private fun notifyVaultReturnToAppUrlConfigInvalid(
        setupTokenId: String,
        callback: PayPalResultCallback,
        startTime: Long
    ) {
        applicationScope.launch(Dispatchers.Main) {
            val error = PayPalError.returnToAppUrlConfigMissingError
            analytics.notify(
                PayPalEvent.FAILED,
                params = analyticsEventParams.copy(orderIdOrSetupTokenId = setupTokenId),
                errorDescription = error.errorDescription
            )
            logUserPerceivedLatencyError(LatencyFlow.VAULT, startTime, error.errorDescription)
            callback.onPayPalResult(PayPalPresentAuthChallengeResult.Failure(error))
        }
    }
    // endregion
}
