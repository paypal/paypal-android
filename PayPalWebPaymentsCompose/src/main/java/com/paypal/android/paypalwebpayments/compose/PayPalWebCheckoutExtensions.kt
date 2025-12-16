package com.paypal.android.paypalwebpayments.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Effect that observes the checkout state flow and invokes callbacks based on state changes.
 *
 * This is useful when you want to reactively handle state changes without manually
 * collecting the state flow.
 *
 * Example usage:
 * ```
 * @Composable
 * fun PayPalCheckoutScreen() {
 *     val payPalState = rememberPayPalWebCheckoutClient(coreConfig)
 *
 *     PayPalCheckoutStateEffect(
 *         state = payPalState,
 *         onSuccess = { result ->
 *             // Navigate to success screen
 *         },
 *         onError = { error ->
 *             // Show error dialog
 *         },
 *         onCanceled = {
 *             // Show cancellation message
 *         }
 *     )
 *
 *     // Your UI...
 * }
 * ```
 *
 * @param state The PayPalWebCheckoutState to observe
 * @param onSuccess Callback invoked when checkout succeeds
 * @param onError Callback invoked when checkout fails
 * @param onCanceled Callback invoked when checkout is canceled
 */
@Composable
fun PayPalCheckoutStateEffect(
    state: PayPalWebCheckoutState,
    onSuccess: (PayPalWebCheckoutState.CheckoutState.Success) -> Unit = {},
    onError: (PayPalWebCheckoutState.CheckoutState.Error) -> Unit = {},
    onCanceled: () -> Unit = {}
) {
    LaunchedEffect(state) {
        state.checkoutState.collect { checkoutState ->
            when (checkoutState) {
                is PayPalWebCheckoutState.CheckoutState.Success -> {
                    onSuccess(checkoutState)
                }

                is PayPalWebCheckoutState.CheckoutState.Error -> {
                    onError(checkoutState)
                }

                is PayPalWebCheckoutState.CheckoutState.Canceled -> {
                    onCanceled()
                }

                else -> { /* No action needed for other states */
                }
            }
        }
    }
}

/**
 * Effect that observes the vault state flow and invokes callbacks based on state changes.
 *
 * Example usage:
 * ```
 * @Composable
 * fun PayPalVaultScreen() {
 *     val payPalState = rememberPayPalWebCheckoutClient(coreConfig)
 *
 *     PayPalVaultStateEffect(
 *         state = payPalState,
 *         onSuccess = { result ->
 *             // Navigate to success screen
 *         },
 *         onError = { error ->
 *             // Show error dialog
 *         },
 *         onCanceled = {
 *             // Show cancellation message
 *         }
 *     )
 *
 *     // Your UI...
 * }
 * ```
 *
 * @param state The PayPalWebCheckoutState to observe
 * @param onSuccess Callback invoked when vault succeeds
 * @param onError Callback invoked when vault fails
 * @param onCanceled Callback invoked when vault is canceled
 */
@Composable
fun PayPalVaultStateEffect(
    state: PayPalWebCheckoutState,
    onSuccess: (PayPalWebCheckoutState.VaultState.Success) -> Unit = {},
    onError: (PayPalWebCheckoutState.VaultState.Error) -> Unit = {},
    onCanceled: () -> Unit = {}
) {
    LaunchedEffect(state) {
        state.vaultState.collect { vaultState ->
            when (vaultState) {
                is PayPalWebCheckoutState.VaultState.Success -> {
                    onSuccess(vaultState)
                }

                is PayPalWebCheckoutState.VaultState.Error -> {
                    onError(vaultState)
                }

                is PayPalWebCheckoutState.VaultState.Canceled -> {
                    onCanceled()
                }

                else -> { /* No action needed for other states */
                }
            }
        }
    }
}

