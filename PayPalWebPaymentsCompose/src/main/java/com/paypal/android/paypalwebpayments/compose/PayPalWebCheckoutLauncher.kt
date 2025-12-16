package com.paypal.android.paypalwebpayments.compose

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.paypal.android.corepayments.CoreConfig
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

/**
 * A launcher for PayPal checkout flows with simplified callback handling.
 *
 * This class provides a higher-level API for launching PayPal checkout and vault flows
 * with automatic state management and result callbacks.
 *
 * Example usage:
 * ```
 * @Composable
 * fun PayPalCheckoutScreen() {
 *     val launcher = rememberPayPalCheckoutLauncher(
 *         configuration = coreConfig,
 *         onCheckoutSuccess = { result ->
 *             // Handle successful checkout
 *             println("Order approved: ${result.orderId}")
 *         },
 *         onCheckoutCanceled = {
 *             // Handle user cancellation
 *             println("User canceled checkout")
 *         },
 *         onCheckoutError = { error ->
 *             // Handle error
 *             println("Checkout failed: ${error.message}")
 *         }
 *     )
 *
 *     Button(onClick = {
 *         val request = PayPalWebCheckoutRequest(
 *             orderId = "ORDER-123",
 *             fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
 *         )
 *         launcher.launchCheckout(request)
 *     }) {
 *         Text("Pay with PayPal")
 *     }
 * }
 * ```
 *
 * @property state The underlying PayPalWebCheckoutState
 */
@Stable
class PayPalCheckoutLauncher internal constructor(
    val state: PayPalWebCheckoutState,
    private val activity: ComponentActivity,
    private val onCheckoutSuccess: (PayPalWebCheckoutFinishStartResult.Success) -> Unit,
    private val onCheckoutCanceled: () -> Unit,
    private val onCheckoutError: (Throwable) -> Unit,
    private val onVaultSuccess: (PayPalWebCheckoutFinishVaultResult.Success) -> Unit,
    private val onVaultCanceled: () -> Unit,
    private val onVaultError: (Throwable) -> Unit
) {

    /**
     * Launches a PayPal checkout flow.
     *
     * @param request The checkout request containing order details
     * @param onPresentationResult Optional callback for the auth challenge presentation result.
     *        This is called immediately after the browser/app is launched, before the user
     *        completes the flow.
     */
    fun launchCheckout(
        request: PayPalWebCheckoutRequest,
        onPresentationResult: (PayPalPresentAuthChallengeResult) -> Unit = {}
    ) {
        state.launchWithAuthTab(activity.applicationContext, request) { result ->
            onPresentationResult(result)
        }
    }

    /**
     * Launches a PayPal vault flow.
     *
     * @param request The vault request containing setup token details
     * @param onPresentationResult Optional callback for the auth challenge presentation result.
     *        This is called immediately after the browser/app is launched, before the user
     *        completes the flow.
     */
    fun launchVault(
        request: PayPalWebVaultRequest,
        onPresentationResult: (PayPalPresentAuthChallengeResult) -> Unit = {}
    ) {
        state.vault(activity, request) { result ->
            onPresentationResult(result)
        }
    }

    /**
     * Resets the checkout state to allow launching a new checkout flow.
     */
    fun resetCheckoutState() {
        state.resetCheckoutState()
    }

    /**
     * Resets the vault state to allow launching a new vault flow.
     */
    fun resetVaultState() {
        state.resetVaultState()
    }

    internal fun handleCheckoutResult(result: PayPalWebCheckoutFinishStartResult) {
        when (result) {
            is PayPalWebCheckoutFinishStartResult.Success -> onCheckoutSuccess(result)
            is PayPalWebCheckoutFinishStartResult.Canceled -> onCheckoutCanceled()
            is PayPalWebCheckoutFinishStartResult.Failure -> onCheckoutError(result.error)
            PayPalWebCheckoutFinishStartResult.NoResult -> { /* No-op */
            }
        }
    }

    internal fun handleVaultResult(result: PayPalWebCheckoutFinishVaultResult) {
        when (result) {
            is PayPalWebCheckoutFinishVaultResult.Success -> onVaultSuccess(result)
            PayPalWebCheckoutFinishVaultResult.Canceled -> onVaultCanceled()
            is PayPalWebCheckoutFinishVaultResult.Failure -> onVaultError(result.error)
            PayPalWebCheckoutFinishVaultResult.NoResult -> { /* No-op */
            }
        }
    }
}

/**
 * Remember a [PayPalCheckoutLauncher] with callbacks for checkout flow results.
 *
 * This function creates a launcher that automatically handles the PayPal checkout flow
 * lifecycle and invokes the appropriate callback based on the result.
 *
 * Example usage:
 * ```
 * @Composable
 * fun PayPalCheckoutScreen(
 *     orderId: String,
 *     onPaymentComplete: (String) -> Unit
 * ) {
 *     val launcher = rememberPayPalCheckoutLauncher(
 *         configuration = remember { CoreConfig("your-client-id") },
 *         onCheckoutSuccess = { result ->
 *             onPaymentComplete(result.orderId)
 *         },
 *         onCheckoutCanceled = {
 *             // Show cancellation message
 *         },
 *         onCheckoutError = { error ->
 *             // Show error message
 *         }
 *     )
 *
 *     Button(onClick = {
 *         launcher.launchCheckout(
 *             PayPalWebCheckoutRequest(
 *                 orderId = orderId,
 *                 fundingSource = PayPalWebCheckoutFundingSource.PAYPAL
 *             )
 *         )
 *     }) {
 *         Text("Pay with PayPal")
 *     }
 * }
 * ```
 *
 * **Note**: Deep link handling is automatic. No Activity-level integration required.
 *
 * @param configuration The CoreConfig for the PayPal SDK
 * @param onCheckoutSuccess Callback invoked when checkout completes successfully
 * @param onCheckoutCanceled Callback invoked when user cancels checkout
 * @param onCheckoutError Callback invoked when checkout fails
 * @param onVaultSuccess Callback invoked when vault completes successfully
 * @param onVaultCanceled Callback invoked when user cancels vault
 * @param onVaultError Callback invoked when vault fails
 * @return A [PayPalCheckoutLauncher] for launching PayPal flows
 */
@Composable
fun rememberPayPalCheckoutLauncher(
    configuration: CoreConfig,
    onCheckoutSuccess: (PayPalWebCheckoutFinishStartResult.Success) -> Unit = {},
    onCheckoutCanceled: () -> Unit = {},
    onCheckoutError: (Throwable) -> Unit = {},
    onVaultSuccess: (PayPalWebCheckoutFinishVaultResult.Success) -> Unit = {},
    onVaultCanceled: () -> Unit = {},
    onVaultError: (Throwable) -> Unit = {}
): PayPalCheckoutLauncher {
    val state = rememberPayPalWebCheckoutClient(configuration)
    val activity = LocalContext.current as ComponentActivity

    val launcher = remember(
        state,
        activity,
        onCheckoutSuccess,
        onCheckoutCanceled,
        onCheckoutError,
        onVaultSuccess,
        onVaultCanceled,
        onVaultError
    ) {
        PayPalCheckoutLauncher(
            state = state,
            activity = activity,
            onCheckoutSuccess = onCheckoutSuccess,
            onCheckoutCanceled = onCheckoutCanceled,
            onCheckoutError = onCheckoutError,
            onVaultSuccess = onVaultSuccess,
            onVaultCanceled = onVaultCanceled,
            onVaultError = onVaultError
        )
    }

    // Observe checkout state flow and invoke callbacks
    LaunchedEffect(launcher) {
        state.checkoutState.collect { checkoutState ->
            when (checkoutState) {
                is PayPalWebCheckoutState.CheckoutState.Success -> {
                    // Pass the existing result object which has orderId and payerId
                    launcher.handleCheckoutResult(checkoutState.result)
                }

                is PayPalWebCheckoutState.CheckoutState.Canceled -> {
                    // Invoke canceled callback directly since we don't have orderId
                    onCheckoutCanceled()
                }

                is PayPalWebCheckoutState.CheckoutState.Error -> {
                    // Invoke error callback directly since we don't have orderId or PayPalSDKError
                    onCheckoutError(checkoutState.error)
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
                    // Pass the existing result object which has approvalSessionId
                    launcher.handleVaultResult(vaultState.result)
                }

                is PayPalWebCheckoutState.VaultState.Canceled -> {
                    // Invoke canceled callback directly
                    onVaultCanceled()
                }

                is PayPalWebCheckoutState.VaultState.Error -> {
                    // Invoke error callback directly
                    onVaultError(vaultState.error)
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
                state.resetCheckoutState()
                onCheckoutCanceled()
            }
        }

        // Check if we were waiting for vault result
        if (state.vaultState.value is PayPalWebCheckoutState.VaultState.AuthChallengePresented) {
            kotlinx.coroutines.delay(300)
            if (state.vaultState.value is PayPalWebCheckoutState.VaultState.AuthChallengePresented) {
                state.resetVaultState()
                onVaultCanceled()
            }
        }
    }

    return launcher
}
