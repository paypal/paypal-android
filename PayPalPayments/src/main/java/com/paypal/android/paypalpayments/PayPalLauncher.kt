package com.paypal.android.paypalpayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.paypal.android.corepayments.BrowserSwitchRequestCodes
import com.paypal.android.corepayments.CaptureDeepLinkResult
import com.paypal.android.corepayments.DeepLink
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.browserswitch.BrowserSwitchClient
import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions
import com.paypal.android.corepayments.browserswitch.BrowserSwitchPendingState
import com.paypal.android.corepayments.browserswitch.BrowserSwitchStartResult
import com.paypal.android.corepayments.captureDeepLink
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.paypalpayments.errors.PayPalError
import java.util.UUID
import org.json.JSONObject

// TODO: consider renaming PayPalLauncher to PayPalAuthChallengeLauncher
internal class PayPalLauncher(
    private val browserSwitchClient: BrowserSwitchClient,
    returnToAppLauncher: PayPalReturnToAppLauncher = PayPalReturnToAppLauncher()
) {

    private val cancellationTracker = PayPalLaunchCancellationTracker(returnToAppLauncher)

    constructor(context: Context) : this(BrowserSwitchClient(context))

    companion object {
        private const val METADATA_KEY_ORDER_ID = "order_id"
        private const val METADATA_KEY_SETUP_TOKEN_ID = "setup_token_id"
        private const val METADATA_KEY_LAUNCH_ID = "sdk_launch_id"

        private const val URL_PARAM_APPROVAL_SESSION_ID = "approval_session_id"
    }

    fun launchWithUrl(
        context: Context,
        uri: Uri,
        token: String,
        tokenType: TokenType,
        returnToAppStrategy: ReturnToAppStrategy,
        onAuthStateCreated: (String) -> Unit = {},
        onLaunchFailed: (String) -> Unit = {},
    ): PayPalPresentAuthChallengeResult {
        cancellationTracker.clear()
        val options = createBrowserSwitchOptions(uri, token, tokenType, returnToAppStrategy)
        val authState = BrowserSwitchPendingState(options).toBase64EncodedJSON()
        return launchBrowserSwitch(
            context,
            options,
            authState,
            onAuthStateCreated,
            onLaunchFailed
        )
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun launchWithUrlAndSessionTracking(
        context: Context,
        uri: Uri,
        token: String,
        tokenType: TokenType,
        returnToAppStrategy: ReturnToAppStrategy,
        cancelUrl: String,
        onAuthStateCreated: (String) -> Unit = {},
        onLaunchFailed: (String) -> Unit = {},
    ): PayPalPresentAuthChallengeResult {
        val options = createBrowserSwitchOptions(uri, token, tokenType, returnToAppStrategy)
        val authState = BrowserSwitchPendingState(options).toBase64EncodedJSON()
        val launch = cancellationTracker.startTracking(
            token,
            getRequestCode(tokenType),
            authState,
            context.applicationContext ?: context,
            cancelUrl
        )
        cancellationTracker.armCancellationReturn(authState)
        var authStatePublished = false
        val result = try {
            browserSwitchClient.startWithSessionTracking(
                context,
                options,
                onTabShown = { cancellationTracker.markTabShown(launch) },
                onSessionEnded = { cancellationTracker.markSessionEnded(launch) },
                onBeforeLaunch = {
                    authStatePublished = true
                    onAuthStateCreated(authState)
                }
            )
        } catch (error: Exception) {
            cancellationTracker.clear(authState)
            if (authStatePublished) onLaunchFailed(authState)
            return PayPalPresentAuthChallengeResult.Failure(
                PayPalError.browserSwitchError(error)
            )
        }
        return when (val startResult = result.startResult) {
            BrowserSwitchStartResult.Success -> {
                cancellationTracker.retainSession(launch, result.session)
                PayPalPresentAuthChallengeResult.Success(authState)
            }
            is BrowserSwitchStartResult.Failure -> {
                cancellationTracker.clear(authState)
                if (authStatePublished) onLaunchFailed(authState)
                PayPalPresentAuthChallengeResult.Failure(
                    PayPalError.browserSwitchError(startResult.error)
                )
            }
        }
    }

    private fun createBrowserSwitchOptions(
        uri: Uri,
        token: String,
        tokenType: TokenType,
        returnToAppStrategy: ReturnToAppStrategy
    ) = BrowserSwitchOptions(
        targetUri = uri,
        requestCode = getRequestCode(tokenType),
        returnUrlScheme = (returnToAppStrategy as? ReturnToAppStrategy.CustomUrlScheme)?.urlScheme,
        appLinkUrl = (returnToAppStrategy as? ReturnToAppStrategy.AppLink)?.appLinkUrl,
        metadata = getMetadata(token, tokenType)
    )

    private fun getRequestCode(tokenType: TokenType): Int {
        return when (tokenType) {
            TokenType.ORDER_ID -> BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
            TokenType.VAULT_ID -> BrowserSwitchRequestCodes.PAYPAL_VAULT
            TokenType.BILLING_TOKEN -> BrowserSwitchRequestCodes.PAYPAL_VAULT
        }
    }

    private fun getMetadata(
        token: String,
        tokenType: TokenType
    ) = JSONObject().apply {
        put(METADATA_KEY_LAUNCH_ID, UUID.randomUUID().toString())
        when (tokenType) {
            TokenType.ORDER_ID -> put(METADATA_KEY_ORDER_ID, token)
            TokenType.VAULT_ID -> put(METADATA_KEY_SETUP_TOKEN_ID, token)
            TokenType.BILLING_TOKEN -> put(METADATA_KEY_SETUP_TOKEN_ID, token)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun launchBrowserSwitch(
        context: Context,
        options: BrowserSwitchOptions,
        authState: String,
        onAuthStateCreated: (String) -> Unit,
        onLaunchFailed: (String) -> Unit
    ): PayPalPresentAuthChallengeResult {
        var authStatePublished = false
        val startResult = try {
            browserSwitchClient.start(context, options) {
                authStatePublished = true
                onAuthStateCreated(authState)
            }
        } catch (error: Exception) {
            if (authStatePublished) onLaunchFailed(authState)
            return PayPalPresentAuthChallengeResult.Failure(
                PayPalError.browserSwitchError(error)
            )
        }
        return when (startResult) {
            is BrowserSwitchStartResult.Success -> {
                PayPalPresentAuthChallengeResult.Success(authState)
            }

            is BrowserSwitchStartResult.Failure -> {
                if (authStatePublished) onLaunchFailed(authState)
                val error = PayPalError.browserSwitchError(startResult.error)
                PayPalPresentAuthChallengeResult.Failure(error)
            }
        }
    }

    fun completeCheckoutAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalFinishStartResult {
        val requestCode = BrowserSwitchRequestCodes.PAYPAL_CHECKOUT
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> {
                cancellationTracker.clear(authState)
                parseWebCheckoutSuccessResult(result.deepLink)
            }
            is CaptureDeepLinkResult.Failure -> {
                cancellationTracker.clear(authState)
                PayPalFinishStartResult.Failure(result.reason, orderId = null)
            }

            is CaptureDeepLinkResult.Ignore ->
                when (val cancellation = cancellationTracker.handleReturnToApp(
                    authState,
                    requestCode
                )) {
                    PayPalLaunchCancellationTracker.Result.NoResult ->
                        PayPalFinishStartResult.NoResult
                    is PayPalLaunchCancellationTracker.Result.Canceled ->
                        PayPalFinishStartResult.Canceled(cancellation.token)
                }
        }
    }

    fun completeVaultAuthRequest(
        intent: Intent,
        authState: String
    ): PayPalFinishVaultResult {
        val requestCode = BrowserSwitchRequestCodes.PAYPAL_VAULT
        return when (val result = captureDeepLink(requestCode, intent, authState)) {
            is CaptureDeepLinkResult.Success -> {
                cancellationTracker.clear(authState)
                parseVaultSuccessResult(result.deepLink)
            }
            is CaptureDeepLinkResult.Failure -> {
                cancellationTracker.clear(authState)
                PayPalFinishVaultResult.Failure(result.reason)
            }

            is CaptureDeepLinkResult.Ignore ->
                when (cancellationTracker.handleReturnToApp(authState, requestCode)) {
                    PayPalLaunchCancellationTracker.Result.NoResult ->
                        PayPalFinishVaultResult.NoResult
                    is PayPalLaunchCancellationTracker.Result.Canceled ->
                        PayPalFinishVaultResult.Canceled
                }
        }
    }

    private fun parseWebCheckoutSuccessResult(
        deepLink: DeepLink
    ): PayPalFinishStartResult {
        val metadata = deepLink.originalOptions.metadata
        return if (metadata == null) {
            val unknownError = PayPalError.unknownError
            PayPalFinishStartResult.Failure(unknownError, null)
        } else {
            val orderId = metadata.optString(METADATA_KEY_ORDER_ID)
            val payerId = deepLink.uri.getQueryParameter("PayerID")
            val isCancelUrl = deepLink.uri.path?.contains("cancel") == true ||
                deepLink.uri.getQueryParameter(
                    PayPalReturnToAppLauncher.CANCELLATION_QUERY_PARAM
                ) == "true"
            if (isCancelUrl) {
                PayPalFinishStartResult.Canceled(orderId)
            } else {
                when {
                    !orderId.isNullOrBlank() && !payerId.isNullOrBlank() ->
                        PayPalFinishStartResult.Success(orderId, payerId)
                    orderId.isNullOrBlank() ->
                        PayPalFinishStartResult.Failure(
                            PayPalError.malformedResultError,
                            orderId
                        )
                    else -> PayPalFinishStartResult.Canceled(orderId)
                }
            }
        }
    }

    private fun parseVaultSuccessResult(
        deepLink: DeepLink
    ): PayPalFinishVaultResult {
        val requestMetadata = deepLink.originalOptions.metadata
        return if (requestMetadata == null) {
            PayPalFinishVaultResult.Failure(PayPalError.unknownError)
        } else {
            val isCancelUrl = deepLink.uri.path?.contains("cancel") == true ||
                deepLink.uri.getQueryParameter(
                    PayPalReturnToAppLauncher.CANCELLATION_QUERY_PARAM
                ) == "true"
            if (isCancelUrl) {
                PayPalFinishVaultResult.Canceled
            } else {
                val approvalSessionId =
                    deepLink.uri.getQueryParameter(URL_PARAM_APPROVAL_SESSION_ID)
                if (approvalSessionId.isNullOrEmpty()) {
                    PayPalFinishVaultResult.Failure(PayPalError.malformedResultError)
                } else {
                    PayPalFinishVaultResult.Success(approvalSessionId)
                }
            }
        }
    }
}
