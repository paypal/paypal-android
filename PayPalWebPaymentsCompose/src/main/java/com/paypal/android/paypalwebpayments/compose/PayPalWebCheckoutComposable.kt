package com.paypal.android.paypalwebpayments.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.paypalwebpayments.compose.internal.OnNewIntentEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class PayPalWebCheckoutState(
    val client: PayPalWebCheckoutClient,
    private val scope: CoroutineScope,
    private val activity: ComponentActivity,
    private val authTabLauncher: ActivityResultLauncher<Intent>,
) {
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)

    /**
     * State flow representing the current checkout flow state.
     * Observe this to react to checkout state changes in your UI.
     */
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    private val _vaultState = MutableStateFlow<VaultState>(VaultState.Idle)

    /**
     * State flow representing the current vault flow state.
     * Observe this to react to vault state changes in your UI.
     */
    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    /**
     * Sealed class representing the state of a PayPal checkout flow.
     */
    internal sealed class CheckoutState {
        /**
         * Initial state, no checkout flow in progress.
         */
        data object Idle : CheckoutState()

        /**
         * Checkout flow is starting, auth challenge is being presented.
         */
        data object Starting : CheckoutState()

        /**
         * Auth challenge was presented successfully, waiting for user to complete.
         */
        data object AuthChallengePresented : CheckoutState()

        /**
         * Checkout completed successfully.
         */
        data class Success(val result: PayPalWebCheckoutFinishStartResult.Success) : CheckoutState()

        /**
         * Checkout was canceled by the user.
         */
        data object Canceled : CheckoutState()

        /**
         * Checkout failed with an error.
         */
        data class Error(val error: Throwable) : CheckoutState()
    }

    /**
     * Sealed class representing the state of a PayPal vault flow.
     */
    internal sealed class VaultState {
        /**
         * Initial state, no vault flow in progress.
         */
        data object Idle : VaultState()

        /**
         * Vault flow is starting, auth challenge is being presented.
         */
        data object Starting : VaultState()

        /**
         * Auth challenge was presented successfully, waiting for user to complete.
         */
        data object AuthChallengePresented : VaultState()

        /**
         * Vault completed successfully.
         */
        data class Success(val result: PayPalWebCheckoutFinishVaultResult.Success) : VaultState()

        /**
         * Vault was canceled by the user.
         */
        data object Canceled : VaultState()

        /**
         * Vault failed with an error.
         */
        data class Error(val error: Throwable) : VaultState()
    }

    fun start(
        activity: ComponentActivity,
        request: PayPalWebCheckoutRequest,
        onResult: (PayPalPresentAuthChallengeResult) -> Unit = {}
    ) {
        _checkoutState.value = CheckoutState.Starting
        client.start(activity, request) { result ->
            when (result) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    _checkoutState.value = CheckoutState.AuthChallengePresented
                    onResult(result)
                }

                is PayPalPresentAuthChallengeResult.Failure -> {
                    _checkoutState.value = CheckoutState.Error(result.error)
                    onResult(result)
                }
            }
        }
    }

    fun vault(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest,
        onResult: (PayPalPresentAuthChallengeResult) -> Unit = {}
    ) {
        _vaultState.value = VaultState.Starting
        client.vault(activity, request) { result ->
            when (result) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    _vaultState.value = VaultState.AuthChallengePresented
                    onResult(result)
                }

                is PayPalPresentAuthChallengeResult.Failure -> {
                    _vaultState.value = VaultState.Error(result.error)
                    onResult(result)
                }
            }
        }
    }

    internal fun handleCheckoutReturn(intent: Intent): PayPalWebCheckoutFinishStartResult? {
        return client.finishStart(intent)?.also { result ->
            _checkoutState.value = when (result) {
                is PayPalWebCheckoutFinishStartResult.Success ->
                    CheckoutState.Success(result)

                is PayPalWebCheckoutFinishStartResult.Canceled ->
                    CheckoutState.Canceled

                is PayPalWebCheckoutFinishStartResult.Failure ->
                    CheckoutState.Error(result.error)

                PayPalWebCheckoutFinishStartResult.NoResult ->
                    CheckoutState.Idle
            }
        }
    }

    /**
     * Handles the return intent from a browser switch for vault flows.
     *
     * **Note**: This is called automatically by the composable when deep links are received.
     * You typically don't need to call this manually unless you have custom deep link handling.
     *
     * @param intent The return intent from the browser
     * @return The finish result, or null if the intent is not a PayPal return
     */
    internal fun handleVaultReturn(intent: Intent): PayPalWebCheckoutFinishVaultResult? {
        return client.finishVault(intent)?.also { result ->
            _vaultState.value = when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success ->
                    VaultState.Success(result)

                PayPalWebCheckoutFinishVaultResult.Canceled ->
                    VaultState.Canceled

                is PayPalWebCheckoutFinishVaultResult.Failure ->
                    VaultState.Error(result.error)

                PayPalWebCheckoutFinishVaultResult.NoResult ->
                    VaultState.Idle
            }
        }
    }

    /**
     * Resets the checkout state to [CheckoutState.Idle].
     * Call this when you want to allow the user to start a new checkout flow.
     */
    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }

    /**
     * Resets the vault state to [VaultState.Idle].
     * Call this when you want to allow the user to start a new vault flow.
     */
    fun resetVaultState() {
        _vaultState.value = VaultState.Idle
    }

    /**
     * Starts a PayPal web checkout flow using AuthTab, which gracefully falls back to custom chrome tab.
     *
     * @param request The checkout request containing order details
     * @param onResult Optional callback for the auth challenge presentation result
     */
    fun launchWithAuthTab(
        context: Context,
        request: PayPalWebCheckoutRequest,
        onResult: (PayPalPresentAuthChallengeResult) -> Unit = {}
    ) {
        _checkoutState.value = CheckoutState.Starting
        scope.launch {
            try {
                client.start(context, request, authTabLauncher)
                _checkoutState.value = CheckoutState.AuthChallengePresented
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Error(e)
            }
        }
    }

    /**
     * Starts a PayPal vault flow using AuthTab, which gracefully falls back to custom chrome tab.
     *
     * @param request The vault request containing setup token details
     * @param onResult Optional callback for the auth challenge presentation result
     */
    fun launchVaultWithAuthTab(
        context: Context,
        request: PayPalWebVaultRequest,
        onResult: (PayPalPresentAuthChallengeResult) -> Unit = {}
    ) {
        _vaultState.value = VaultState.Starting
        scope.launch {
            try {
                client.vault(context, request, authTabLauncher)
                _vaultState.value = VaultState.AuthChallengePresented
            } catch (e: Exception) {
                _vaultState.value = VaultState.Error(e)
            }
        }
    }
}

@Composable
internal fun rememberPayPalWebCheckoutClient(
    configuration: CoreConfig
): PayPalWebCheckoutState {
    val context = LocalContext.current
    val activity = requireNotNull(LocalActivity.current as? ComponentActivity) {
        "rememberPayPalWebCheckoutClient must be called in the context of a ComponentActivity"
    }
    val scope = rememberCoroutineScope()

    // Remember the client instance across recompositions
    val client = remember(configuration) {
        PayPalWebCheckoutClient(
            context = context.applicationContext,
            configuration = configuration
        )
    }

    // Save and restore instance state across process death
    var instanceState by rememberSaveable(
        stateSaver = Saver(
            save = { client.instanceState },
            restore = { it }
        )
    ) { mutableStateOf("") }

    // Create the state wrapper first (without launcher)
    val stateRef = remember { mutableStateOf<PayPalWebCheckoutState?>(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            println("Karthik: Launcher called back with result : $result")
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    stateRef.value?.handleCheckoutReturn(intent)
                }
            }
        }

    // Create the state wrapper
    val state = remember(client, scope, activity, launcher) {
        PayPalWebCheckoutState(client, scope, activity, launcher).also {
            stateRef.value = it
        }
    }

    // Register onNewIntent listener for automatic deep link handling
    OnNewIntentEffect { intent ->
        // Try to handle as checkout return
        state.handleCheckoutReturn(intent)
        // Try to handle as vault return
        state.handleVaultReturn(intent)
    }

    // Restore instance state when client is created
    DisposableEffect(client) {
        if (instanceState.isNotEmpty()) {
            client.restore(instanceState)
        }
        onDispose {
            // Save state before disposal
            instanceState = client.instanceState
        }
    }

    return state
}
