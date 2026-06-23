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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val urlScheme: String? = null,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {

    // Enable app switch by switching this flag to true
    private val appSwitchWhenEligible: Boolean = true

    // for analytics tracking
    private var checkoutOrderId: String? = null
    private var vaultSetupTokenId: String? = null
    private var appSwitchEnabled: Boolean = false

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
     * The SDK invokes [createOrderHandler] on a background thread to retrieve the order ID,
     * then launches the PayPal checkout web flow.
     *
     * Note: the [activity] parameter will be removed once the Shopper Session ID (SSID)
     * launch path is implemented in a follow-up story.
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
            // TODO: DTPPMOBILE-XXX — execute createOrder and Shopper Session creation in parallel
            val createOrderResult = withContext(Dispatchers.IO) {
                createOrderHandler.createOrder()
            }
            when (createOrderResult) {
                is CreateOrderResponse.Success -> {
                    val orderId = createOrderResult.orderId
                    val result = startWithOrderId(
                        activity = activity,
                        orderId = orderId,
                        returnToAppUrlConfig = request.returnToAppUrlConfig
                    )
                    withContext(Dispatchers.Main) {
                        callback.onPayPalWebStartResult(result)
                    }
                }

                is CreateOrderResponse.Failure -> {
                    val sdkError = PayPalWebCheckoutError.createOrderFailed(createOrderResult.error)
                    withContext(Dispatchers.Main) {
                        callback.onPayPalWebStartResult(
                            PayPalPresentAuthChallengeResult.Failure(sdkError)
                        )
                    }
                }
            }
        }
    }

    /**
     * Vault PayPal as a payment method.
     *
     * The SDK invokes [createSetupTokenHandler] on a background thread to retrieve the setup
     * token ID, then launches the PayPal vault web flow.
     *
     * Note: the [activity] parameter will be removed once the Shopper Session ID (SSID)
     * launch path is implemented in a follow-up story.
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
            // TODO: DTPPMOBILE-XXX — execute createSetupToken and Shopper Session creation in parallel
            val createSetupTokenResult = withContext(Dispatchers.IO) {
                createSetupTokenHandler.createSetupToken()
            }
            when (createSetupTokenResult) {
                is CreateSetupTokenResponse.Success -> {
                    val setupTokenId = createSetupTokenResult.setupTokenId
                    val result = vaultWithSetupTokenId(
                        activity = activity,
                        setupTokenId = setupTokenId,
                        returnToAppUrlConfig = request.returnToAppUrlConfig
                    )
                    withContext(Dispatchers.Main) {
                        callback.onPayPalWebVaultResult(result)
                    }
                }

                is CreateSetupTokenResponse.Failure -> {
                    val sdkError =
                        PayPalWebCheckoutError.createSetupTokenFailed(createSetupTokenResult.error)
                    withContext(Dispatchers.Main) {
                        callback.onPayPalWebVaultResult(
                            PayPalPresentAuthChallengeResult.Failure(sdkError)
                        )
                    }
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

    // region — Internal launch helpers

    @VisibleForTesting
    internal suspend fun startWithOrderId(
        activity: Activity,
        orderId: String,
        returnToAppUrlConfig: ReturnToAppUrlConfig
    ): PayPalPresentAuthChallengeResult {
        checkoutOrderId = orderId
        appSwitchEnabled = false
        analytics.notify(CheckoutEvent.STARTED, orderId, appSwitchEnabled)

        val returnToAppStrategy = ReturnToAppStrategy.AppLink(returnToAppUrlConfig.returnAppUrl)

        val launchUri = withContext(Dispatchers.IO) {
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
    internal suspend fun vaultWithSetupTokenId(
        activity: Activity,
        setupTokenId: String,
        returnToAppUrlConfig: ReturnToAppUrlConfig
    ): PayPalPresentAuthChallengeResult {
        vaultSetupTokenId = setupTokenId
        appSwitchEnabled = false
        analytics.notify(VaultEvent.STARTED, setupTokenId, appSwitchEnabled)

        val returnToAppStrategy = ReturnToAppStrategy.AppLink(returnToAppUrlConfig.returnAppUrl)

        val launchUri = withContext(Dispatchers.IO) {
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

    // region — Private URL builders

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
}
