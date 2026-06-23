package com.paypal.android.ui.paypalwebvault

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.CreateSetupTokenHandler
import com.paypal.android.paypalwebpayments.CreateSetupTokenResponse
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreatePayPalPaymentTokenUseCase
import com.paypal.android.usecase.CreatePayPalSetupTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class PayPalVaultViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createPayPalSetupTokenUseCase: CreatePayPalSetupTokenUseCase,
    val createPayPalPaymentTokenUseCase: CreatePayPalPaymentTokenUseCase,
) : ViewModel() {
    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId)
    private val paypalClient = PayPalWebCheckoutClient(applicationContext, coreConfig)

    private val _uiState = MutableStateFlow(PayPalVaultUiState())
    val uiState = _uiState.asStateFlow()

    private var vaultPayPalState
        get() = _uiState.value.vaultPayPalState
        set(value) {
            _uiState.update { it.copy(vaultPayPalState = value) }
        }

    private var createPaymentTokenState
        get() = _uiState.value.createPaymentTokenState
        set(value) {
            _uiState.update { it.copy(createPaymentTokenState = value) }
        }

    var returnToAppStrategy: ReturnToAppStrategyOption
        get() = _uiState.value.returnToAppStrategy
        set(value) {
            _uiState.update { it.copy(returnToAppStrategy = value) }
        }

    // Kept so createPaymentToken() can look up the token after vault completes.
    private var lastSetupToken: PayPalSetupToken? = null

    fun vaultSetupToken(activity: ComponentActivity) {
        vaultPayPalState = ActionState.Loading

        val vaultRequest = PayPalWebVaultRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = returnToAppStrategy.toReturnToAppUrlConfig()
        )

        // createSetupToken is invoked by the SDK on a background thread; runBlocking is safe here.
        val createSetupTokenHandler = CreateSetupTokenHandler {
            when (val result = runBlocking {
                createPayPalSetupTokenUseCase(returnToAppStrategy.toReturnToAppStrategy())
            }) {
                is SDKSampleServerResult.Success -> {
                    lastSetupToken = result.value
                    CreateSetupTokenResponse.Success(result.value.id ?: "")
                }

                is SDKSampleServerResult.Failure ->
                    CreateSetupTokenResponse.Failure(result.value)
            }
        }

        paypalClient.vault(activity, vaultRequest, createSetupTokenHandler) { result ->
            when (result) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    // Wait for the buyer to authenticate in Chrome Custom Tab.
                }

                is PayPalPresentAuthChallengeResult.Failure ->
                    vaultPayPalState = ActionState.Failure(result.error)
            }
        }
    }

    fun createPaymentToken() {
        val setupToken = lastSetupToken
        if (setupToken == null) {
            createPaymentTokenState =
                ActionState.Failure(Exception("Vault a setup token to continue."))
        } else {
            createPaymentTokenState = ActionState.Loading
            viewModelScope.launch {
                createPaymentTokenState =
                    createPayPalPaymentTokenUseCase(setupToken).mapToActionState()
            }
        }
    }

    fun completeAuthChallenge(intent: Intent) {
        paypalClient.finishVault(intent)?.let { result ->
            vaultPayPalState = when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success -> ActionState.Success(result)
                is PayPalWebCheckoutFinishVaultResult.Failure -> ActionState.Failure(result.error)
                PayPalWebCheckoutFinishVaultResult.Canceled ->
                    ActionState.Failure(Exception("USER CANCELED"))

                PayPalWebCheckoutFinishVaultResult.NoResult -> {
                    // No result; re-enable button so user can retry.
                    ActionState.Idle
                }
            }
        }
    }
}
