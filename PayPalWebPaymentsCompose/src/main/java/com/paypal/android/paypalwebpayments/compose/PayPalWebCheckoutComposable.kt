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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
) {
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)

    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    private val _vaultState = MutableStateFlow<VaultState>(VaultState.Idle)

    val vaultState: StateFlow<VaultState> = _vaultState.asStateFlow()

    /**
     * Sealed class representing the state of a PayPal checkout flow.
     */
    internal sealed class CheckoutState {
        data object Idle : CheckoutState()

        data object Starting : CheckoutState()

        data object AuthChallengePresented : CheckoutState()

        data class Success(val result: PayPalWebCheckoutFinishStartResult.Success) : CheckoutState()

        data object Canceled : CheckoutState()

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

    fun resetCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }
    fun resetVaultState() {
        _vaultState.value = VaultState.Idle
    }

    fun launchWithAuthTab(
        context: Context,
        request: PayPalWebCheckoutRequest,
        activityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        _checkoutState.value = CheckoutState.Starting
        scope.launch {
            try {
                client.start(context, request, activityResultLauncher)
                _checkoutState.value = CheckoutState.AuthChallengePresented
            } catch (e: Exception) {
                _checkoutState.value = CheckoutState.Error(e)
            }
        }
    }

    fun launchVaultWithAuthTab(
        context: Context,
        request: PayPalWebVaultRequest,
        activityResultLauncher: ActivityResultLauncher<Intent>
    ) {
        _vaultState.value = VaultState.Starting
        scope.launch {
            try {
                client.vault(context, request, activityResultLauncher)
                _vaultState.value = VaultState.AuthChallengePresented
            } catch (e: Exception) {
                _vaultState.value = VaultState.Error(e)
            }
        }
    }
}

internal data class PayPalWebCheckoutClientWithLauncher(
    val state: PayPalWebCheckoutState,
    val launcher: ActivityResultLauncher<Intent>
)

@Composable
internal fun rememberPayPalWebCheckoutClient(
    configuration: CoreConfig
): PayPalWebCheckoutClientWithLauncher {
    val context = LocalContext.current
    // Validate we're in a ComponentActivity context
    requireNotNull(LocalActivity.current as? ComponentActivity) {
        "rememberPayPalWebCheckoutClient must be called in the context of a ComponentActivity"
    }
    val scope = rememberCoroutineScope()

    // Remember instance state across compositions, config changes and process kill & restore
    val client = rememberSaveable(
        saver = Saver(
            save = { it.instanceState },
            restore = { savedState ->
                PayPalWebCheckoutClient(
                    context = context.applicationContext,
                    configuration = configuration
                ).apply { restore(savedState) }
            }
        )
    ) {
        PayPalWebCheckoutClient(
            context = context.applicationContext,
            configuration = configuration
        )
    }

    val state: PayPalWebCheckoutState = remember(client, scope) {
        PayPalWebCheckoutState(client, scope)
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { intent ->
                    state.handleCheckoutReturn(intent)
                }
            }
        }

    OnNewIntentEffect { intent ->
        state.handleCheckoutReturn(intent)
        state.handleVaultReturn(intent)
    }

    return PayPalWebCheckoutClientWithLauncher(state, launcher)
}
