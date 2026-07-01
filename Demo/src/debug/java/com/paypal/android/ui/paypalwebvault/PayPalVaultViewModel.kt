package com.paypal.android.ui.paypalwebvault

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.customenvironment.CustomEnvironmentRepository
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
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
import javax.inject.Inject

@HiltViewModel
class PayPalVaultViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createPayPalSetupTokenUseCase: CreatePayPalSetupTokenUseCase,
    val createPayPalPaymentTokenUseCase: CreatePayPalPaymentTokenUseCase,
    private val customEnvironmentRepository: CustomEnvironmentRepository,
) : ViewModel() {

    private fun buildCoreConfig(): CoreConfig =
        customEnvironmentRepository.getConfig().toCoreConfig(
            fallbackConfig = CoreConfig(SDKSampleServerAPI.clientId)
        )

    // Held as a field so completeAuthChallenge uses the same instance that started the vault flow.
    private var paypalClient: PayPalWebCheckoutClient? = null

    private val _uiState = MutableStateFlow(PayPalVaultUiState())
    val uiState = _uiState.asStateFlow()

    private var createSetupTokenState
        get() = _uiState.value.createSetupTokenState
        set(value) {
            _uiState.update { it.copy(createSetupTokenState = value) }
        }

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

    fun createSetupToken() {
        viewModelScope.launch {
            createSetupTokenState = ActionState.Loading
            createSetupTokenState = createPayPalSetupTokenUseCase(
                returnToAppStrategy.toReturnToAppStrategy()
            ).mapToActionState()
        }
    }

    private val createdSetupToken: PayPalSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    fun vaultSetupToken(activity: ComponentActivity) {
        val setupTokenId = createdSetupToken?.id
        if (setupTokenId == null) {
            vaultPayPalState = ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            viewModelScope.launch {
                vaultSetupTokenWithRequest(
                    activity,
                    PayPalWebVaultRequest(setupTokenId, returnToAppStrategy.toReturnToAppStrategy())
                )
            }
        }
    }

    private fun vaultSetupTokenWithRequest(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ) {
        vaultPayPalState = ActionState.Loading

        // Rebuild from the active environment config so any Settings change is picked up.
        val client = PayPalWebCheckoutClient(applicationContext, buildCoreConfig())
            .also { paypalClient = it }

        client.vault(activity, request) { result ->
            when (result) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    // do nothing; wait for user to authenticate in Chrome Custom Tab
                }

                is PayPalPresentAuthChallengeResult.Failure ->
                    vaultPayPalState = ActionState.Failure(result.error)
            }
        }
    }

    fun createPaymentToken() {
        val setupToken = createdSetupToken
        if (setupToken == null) {
            createPaymentTokenState =
                ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            createPaymentTokenState = ActionState.Loading
            viewModelScope.launch {
                createPaymentTokenState =
                    createPayPalPaymentTokenUseCase(setupToken).mapToActionState()
            }
        }
    }

    fun completeAuthChallenge(intent: Intent) {
        val client = paypalClient ?: PayPalWebCheckoutClient(applicationContext, buildCoreConfig())
        client.finishVault(intent)?.let { result ->
            vaultPayPalState = when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success -> ActionState.Success(result)
                is PayPalWebCheckoutFinishVaultResult.Failure -> ActionState.Failure(result.error)
                PayPalWebCheckoutFinishVaultResult.Canceled ->
                    ActionState.Failure(Exception("USER CANCELED"))
                PayPalWebCheckoutFinishVaultResult.NoResult -> ActionState.Idle
            }
        }
    }
}
