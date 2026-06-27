package com.paypal.android.paypalwebpayments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.corepayments.returnUrl
import com.paypal.android.paypalwebpayments.analytics.CheckoutEvent
import com.paypal.android.paypalwebpayments.analytics.PayPalWebAnalytics
import com.paypal.android.paypalwebpayments.analytics.VaultEvent
import com.paypal.android.paypalwebpayments.errors.PayPalWebCheckoutError
import kotlinx.coroutines.CoroutineDispatcher
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
class PayPalWebCheckoutClient internal constructor(
    private val analytics: PayPalWebAnalytics,
    private val payPalWebLauncher: PayPalWebLauncher,
    private val sessionStore: PayPalWebCheckoutSessionStore,
    private val deviceInspector: DeviceInspector,
    private val coreConfig: CoreConfig,
    private val updateClientConfigAPI: UpdateClientConfigAPI,
    private val patchCCOWithAppSwitchEligibility: PatchCCOWithAppSwitchEligibility,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // Disable app switch by switching this flag to false
    private val appSwitchWhenEligible: Boolean = true

    // For analytics tracking
    private var checkoutOrderId: String? = null
    private var vaultSetupTokenId: String? = null
    private var appSwitchEnabled: Boolean = false

    constructor(
        context: Context,
        configuration: CoreConfig
    ) : this(
        analytics = PayPalWebAnalytics(AnalyticsService(context.applicationContext, configuration)),
        payPalWebLauncher = PayPalWebLauncher(context),
        sessionStore = PayPalWebCheckoutSessionStore(),
        deviceInspector = DeviceInspector(context),
        coreConfig = configuration,
        patchCCOWithAppSwitchEligibility = PatchCCOWithAppSwitchEligibility(configuration),
        updateClientConfigAPI = UpdateClientConfigAPI(context, configuration),
    )

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

    // region Active Methods

    /**
     * Confirm PayPal payment source for an order.
     *
     * The SDK invokes [createOrderHandler] on a background thread to retrieve the order ID,
     * then launches the PayPal checkout web flow.
     *
     * @param activity The activity to launch the PayPal web checkout from.
     * @param request [PayPalWebCheckoutRequest] containing buyer identity and URL config.
     * @param createOrderHandler [CreateOrderHandler] invoked by the SDK to create the order.
     * @param callback [PayPalWebStartCallback] to receive the result.
     */
    fun start(
        activity: Activity,
        request: PayPalWebCheckoutRequest,
        createOrderHandler: CreateOrderHandler,
        callback: PayPalWebStartCallback
    ) {
        applicationScope.launch {
            val result = startAsync(activity, createOrderHandler, request)
            withContext(Dispatchers.Main) {
                callback.onPayPalWebStartResult(result)
            }
        }
    }

    /**
     * Vault PayPal as a payment method.
     *
     * The SDK invokes [createSetupTokenHandler] on a background thread to retrieve the setup
     * token ID, then launches the PayPal vault web flow.
     *
     * @param activity The activity to launch the PayPal vault flow from.
     * @param request [PayPalWebVaultRequest] containing buyer identity and URL config.
     * @param createSetupTokenHandler [CreateSetupTokenHandler] invoked by the SDK to create the setup token.
     * @param callback [PayPalWebVaultCallback] to receive the result.
     */
    fun vault(
        activity: Activity,
        request: PayPalWebVaultRequest,
        createSetupTokenHandler: CreateSetupTokenHandler,
        callback: PayPalWebVaultCallback
    ) {
        applicationScope.launch {
            val result = vaultAsync(activity, createSetupTokenHandler, request.payPalURLConfig)
            withContext(Dispatchers.Main) {
                callback.onPayPalWebVaultResult(result)
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
                    analytics.notify(CheckoutEvent.SUCCEEDED, checkoutOrderId, appSwitchEnabled)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    analytics.notify(CheckoutEvent.CANCELED, checkoutOrderId, appSwitchEnabled)
                    sessionStore.clear()
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    analytics.notify(CheckoutEvent.FAILED, checkoutOrderId, appSwitchEnabled)
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

    @VisibleForTesting
    internal suspend fun startAsync(
        activity: Activity,
        createOrderHandler: CreateOrderHandler,
        payPalWebCheckoutRequest: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        val shopperSessionDeferred = applicationScope.async(ioDispatcher) {
            null as String?
        }
        val createOrderResult = withContext(ioDispatcher) {
            createOrderHandler.createOrder()
        }

        return when (createOrderResult) {
            is CreateOrderResponse.Success -> {
                val orderId = createOrderResult.orderId
                val shopperSessionId = shopperSessionDeferred.await()
                launchCheckout(activity, orderId, shopperSessionId, payPalWebCheckoutRequest)
            }
            is CreateOrderResponse.Failure -> PayPalPresentAuthChallengeResult.Failure(
                PayPalWebCheckoutError.createOrderFailed(createOrderResult.error)
            )
        }
    }

    private suspend fun launchCheckout(
        activity: Activity,
        orderId: String,
        shopperSessionId: String?,
        payPalWebCheckoutRequest: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        // TODO: Use the shopperSessionId
        checkoutOrderId = orderId
        appSwitchEnabled = false
        analytics.notify(CheckoutEvent.STARTED, orderId, appSwitchEnabled)

        val returnToAppStrategy = ReturnToAppStrategy.AppLink(payPalWebCheckoutRequest.payPalURLConfig.returnAppUrl)

        val launchUri = withContext(ioDispatcher) {
            // Run updateClientConfig and getLaunchUri in parallel
            val updateConfigDeferred = async {
                updateClientConfigAPI.updateClientConfig(
                    orderId,
                    payPalWebCheckoutRequest.fundingSource.value
                )
            }
            val launchUriDeferred = async {
                getLaunchUri(
                    context = activity.applicationContext,
                    token = orderId,
                    tokenType = TokenType.ORDER_ID,
                    fallbackUri = buildPayPalCheckoutUri(
                        orderId = orderId,
                        funding = PayPalWebCheckoutFundingSource.PAYPAL,
                        returnUrl = returnToAppStrategy.returnUrl
                    )
                )
            }
            updateConfigDeferred.await()
            launchUriDeferred.await()
        }

        val result = payPalWebLauncher.launchWithUrl(
            activity = activity,
            uri = launchUri,
            token = orderId,
            tokenType = TokenType.ORDER_ID,
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    orderId,
                    appSwitchEnabled
                )
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    CheckoutEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    orderId,
                    appSwitchEnabled
                )
            }
        }

        return result
    }

    @VisibleForTesting
    internal suspend fun vaultAsync(
        activity: Activity,
        createSetupTokenHandler: CreateSetupTokenHandler,
        payPalURLConfig: PayPalURLConfig
    ): PayPalPresentAuthChallengeResult {
        // TODO: execute createSetupToken and Shopper Session creation in parallel
        val shopperSessionDeferred = applicationScope.async(ioDispatcher) {
            null as String?
        }

        val createSetupTokenResult = withContext(ioDispatcher) {
            createSetupTokenHandler.createSetupToken()
        }
        return when (createSetupTokenResult) {
            is CreateSetupTokenResponse.Success -> {
                val setupTokenId = createSetupTokenResult.setupTokenId
                val shopperSessionId = shopperSessionDeferred.await()
                launchVault(activity, setupTokenId, shopperSessionId, payPalURLConfig)
            }
            is CreateSetupTokenResponse.Failure -> PayPalPresentAuthChallengeResult.Failure(
                PayPalWebCheckoutError.createSetupTokenFailed(createSetupTokenResult.error)
            )
        }
    }

    private suspend fun launchVault(
        activity: Activity,
        setupTokenId: String,
        shopperSessionId: String?,
        payPalURLConfig: PayPalURLConfig
    ): PayPalPresentAuthChallengeResult {
        // TODO: Use the shopperSessionId
        vaultSetupTokenId = setupTokenId
        appSwitchEnabled = false
        analytics.notify(VaultEvent.STARTED, setupTokenId, appSwitchEnabled)

        val returnToAppStrategy = ReturnToAppStrategy.AppLink(payPalURLConfig.returnAppUrl)

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
            returnToAppStrategy = returnToAppStrategy
        )

        when (result) {
            is PayPalPresentAuthChallengeResult.Success -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED,
                    setupTokenId,
                    appSwitchEnabled
                )
                sessionStore.authState = result.authState
            }

            is PayPalPresentAuthChallengeResult.Failure -> {
                analytics.notify(
                    VaultEvent.AUTH_CHALLENGE_PRESENTATION_FAILED,
                    setupTokenId,
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
            .appendQueryParameter(
                "integration_artifact",
                UpdateClientConfigAPI.Defaults.INTEGRATION_ARTIFACT
            )
            .build()
    }

    private fun buildPayPalVaultUri(setupTokenId: String): Uri {
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

    // endregion

    // region Deprecated Methods

    /**
     * Create a new instance of [PayPalWebCheckoutClient].
     *
     * @param context an Android context
     * @param configuration a [CoreConfig] object
     * @param urlScheme the custom URL scheme used to return to your app from a browser switch flow
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
        patchCCOWithAppSwitchEligibility = PatchCCOWithAppSwitchEligibility(configuration),
        updateClientConfigAPI = UpdateClientConfigAPI(context, configuration),
    )

    /**
     * Confirm PayPal payment source for an order.
     *
     * @param activity The activity to launch the PayPal web checkout from.
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval.
     */
    @Deprecated(
        message = "Use start(activity, request, createOrderHandler, callback) instead.",
        replaceWith = ReplaceWith("start(activity, request, createOrderHandler, callback)")
    )
    fun start(
        activity: Activity,
        request: PayPalWebCheckoutRequest
    ): PayPalPresentAuthChallengeResult {
        return PayPalPresentAuthChallengeResult.Failure(
            PayPalWebCheckoutError.createOrderFailed(
                Exception("Deprecated. Migrate to start(activity, request, createOrderHandler, callback).")
            )
        )
    }

    /**
     * Confirm PayPal payment source for an order.
     *
     * @param activity The activity to launch the PayPal web checkout from.
     * @param request [PayPalWebCheckoutRequest] for requesting an order approval.
     * @param callback [PayPalWebStartCallback] to receive the result.
     */
    @Deprecated(
        message = "Use start(activity, request, createOrderHandler, callback) instead.",
        replaceWith = ReplaceWith("start(activity, request, createOrderHandler, callback)")
    )
    fun start(
        activity: Activity,
        request: PayPalWebCheckoutRequest,
        callback: PayPalWebStartCallback
    ) {
        applicationScope.launch {
            withContext(Dispatchers.Main) {
                callback.onPayPalWebStartResult(
                    PayPalPresentAuthChallengeResult.Failure(
                        PayPalWebCheckoutError.createOrderFailed(
                            Exception("Deprecated. Migrate to start(activity, request, createOrderHandler, callback).")
                        )
                    )
                )
            }
        }
    }

    /**
     * Vault PayPal as a payment method.
     *
     * @param activity The activity to launch the PayPal vault flow from.
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method.
     */
    @Deprecated(
        message = "Use vault(activity, request, createSetupTokenHandler, callback) instead.",
        replaceWith = ReplaceWith("vault(activity, request, createSetupTokenHandler, callback)")
    )
    fun vault(
        activity: Activity,
        request: PayPalWebVaultRequest
    ): PayPalPresentAuthChallengeResult {
        return PayPalPresentAuthChallengeResult.Failure(
            PayPalWebCheckoutError.createSetupTokenFailed(
                Exception("Deprecated. Migrate to vault(activity, request, createSetupTokenHandler, callback).")
            )
        )
    }

    /**
     * Vault PayPal as a payment method.
     *
     * @param activity The activity to launch the PayPal vault flow from.
     * @param request [PayPalWebVaultRequest] for vaulting PayPal as a payment method.
     * @param callback [PayPalWebVaultCallback] to receive the result.
     */
    @Deprecated(
        message = "Use vault(activity, request, createSetupTokenHandler, callback) instead.",
        replaceWith = ReplaceWith("vault(activity, request, createSetupTokenHandler, callback)")
    )
    fun vault(
        activity: Activity,
        request: PayPalWebVaultRequest,
        callback: PayPalWebVaultCallback
    ) {
        applicationScope.launch {
            withContext(Dispatchers.Main) {
                callback.onPayPalWebVaultResult(
                    PayPalPresentAuthChallengeResult.Failure(
                        PayPalWebCheckoutError.createSetupTokenFailed(
                            Exception("Deprecated. Migrate to vault(activity, request, createSetupTokenHandler, callback).")
                        )
                    )
                )
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
