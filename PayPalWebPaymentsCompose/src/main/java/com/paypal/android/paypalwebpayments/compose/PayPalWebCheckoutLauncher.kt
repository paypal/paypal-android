package com.paypal.android.paypalwebpayments.compose

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest

/**
 * Composable effect that invokes a callback when the lifecycle owner resumes.
 */
@Composable
private fun OnLifecycleOwnerResumeEffect(callback: suspend () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            callback()
        }
    }
}

@Stable
class PayPalCheckoutLauncher internal constructor(
    internal val state: PayPalWebCheckoutState,
    private val activity: ComponentActivity
) {
    private var pendingCheckoutCallback: ((PayPalWebCheckoutFinishStartResult) -> Unit)? = null
    private var pendingVaultCallback: ((PayPalWebCheckoutFinishVaultResult) -> Unit)? = null

    fun start(
        request: PayPalWebCheckoutRequest,
        onResult: (PayPalWebCheckoutFinishStartResult) -> Unit
    ) {
        println("Kartik: PayPalCheckoutLauncher.start called with request: $request")
        pendingCheckoutCallback = onResult
        state.launchWithAuthTab(activity.applicationContext, request)
    }

    fun vault(
        request: PayPalWebVaultRequest,
        onResult: (PayPalWebCheckoutFinishVaultResult) -> Unit,
        onPresentationResult: (PayPalPresentAuthChallengeResult) -> Unit = {}
    ) {
        pendingVaultCallback = onResult
        state.launchVaultWithAuthTab(activity.applicationContext, request) { result ->
            onPresentationResult(result)
        }
    }

    internal fun handleCheckoutResult(result: PayPalWebCheckoutFinishStartResult) {
        println("Kartik: handleCheckoutResult called with $result")
        pendingCheckoutCallback?.invoke(result)
        pendingCheckoutCallback = null
        state.resetCheckoutState()
    }

    internal fun handleVaultResult(result: PayPalWebCheckoutFinishVaultResult) {
        pendingVaultCallback?.invoke(result)
        pendingVaultCallback = null
        state.resetVaultState()
    }
}

@Composable
fun rememberPayPalCheckoutLauncher(
    configuration: CoreConfig
): PayPalCheckoutLauncher {
    val state = rememberPayPalWebCheckoutClient(configuration)
    val activity = requireNotNull(LocalActivity.current as? ComponentActivity) {
        "rememberPayPalCheckoutLauncher must be called in the context of a ComponentActivity"
    }

    val launcher = remember(state, activity) {
        PayPalCheckoutLauncher(
            state = state,
            activity = activity
        )
    }

    // Observe checkout state flow and invoke callbacks
    LaunchedEffect(launcher) {
        state.checkoutState.collect { checkoutState ->
            when (checkoutState) {
                is PayPalWebCheckoutState.CheckoutState.Success -> {
                    launcher.handleCheckoutResult(checkoutState.result)
                }

                is PayPalWebCheckoutState.CheckoutState.Canceled -> {
                    launcher.handleCheckoutResult(PayPalWebCheckoutFinishStartResult.Canceled(null))
                }

                is PayPalWebCheckoutState.CheckoutState.Error -> {
                    val sdkError = if (checkoutState.error is PayPalSDKError) {
                        checkoutState.error
                    } else {
                        PayPalSDKError(
                            code = 500,
                            errorDescription = checkoutState.error.message ?: "Unknown error",
                            reason = checkoutState.error
                        )
                    }
                    launcher.handleCheckoutResult(
                        PayPalWebCheckoutFinishStartResult.Failure(
                            sdkError,
                            null
                        )
                    )
                }

                else -> { /* No action for Idle, Starting, AuthChallengePresented */
                }
            }
        }
    }

    // Observe vault state flow and invoke callbacks
    LaunchedEffect(launcher) {
        state.vaultState.collect { vaultState ->
            when (vaultState) {
                is PayPalWebCheckoutState.VaultState.Success -> {
                    launcher.handleVaultResult(vaultState.result)
                }

                is PayPalWebCheckoutState.VaultState.Canceled -> {
                    launcher.handleVaultResult(PayPalWebCheckoutFinishVaultResult.Canceled)
                }

                is PayPalWebCheckoutState.VaultState.Error -> {
                    val sdkError = if (vaultState.error is PayPalSDKError) {
                        vaultState.error
                    } else {
                        PayPalSDKError(
                            code = 500,
                            errorDescription = vaultState.error.message ?: "Unknown error",
                            reason = vaultState.error
                        )
                    }
                    launcher.handleVaultResult(PayPalWebCheckoutFinishVaultResult.Failure(sdkError))
                }

                else -> { /* No action for Idle, Starting, AuthChallengePresented */
                }
            }
        }
    }

    // Handle user closing browser/CCT without completing checkout
    OnLifecycleOwnerResumeEffect {
        // Check if we were waiting for checkout result
        if (state.checkoutState.value is PayPalWebCheckoutState.CheckoutState.AuthChallengePresented) {
            // Activity resumed but we're still in AuthChallengePresented state
            // This means the browser/CCT was closed without a deep link
            // Wait a short time to allow OnNewIntentEffect to process any pending deep links
            kotlinx.coroutines.delay(300)
            // If still in AuthChallengePresented state, user canceled
            if (state.checkoutState.value is PayPalWebCheckoutState.CheckoutState.AuthChallengePresented) {
                launcher.handleCheckoutResult(PayPalWebCheckoutFinishStartResult.Canceled(null))
            }
        }

        // Check if we were waiting for vault result
        if (state.vaultState.value is PayPalWebCheckoutState.VaultState.AuthChallengePresented) {
            kotlinx.coroutines.delay(300)
            if (state.vaultState.value is PayPalWebCheckoutState.VaultState.AuthChallengePresented) {
                launcher.handleVaultResult(PayPalWebCheckoutFinishVaultResult.Canceled)
            }
        }
    }

    return launcher
}
