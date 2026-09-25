package com.paypal.android.ui.paypalvault

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.DemoConstants
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.customenvironment.CustomEnvironmentRepository
import com.paypal.android.paypalpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalpayments.PayPalUserAction
import com.paypal.android.paypalpayments.PayPalUserIdentity
import com.paypal.android.paypalpayments.PayPalClient
import com.paypal.android.paypalpayments.PayPalFinishVaultResult
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
        customEnvironmentRepository.getCoreConfig(
            CoreConfig(SDKSampleServerAPI.clientId, SDKSampleServerAPI.merchantId)
        )

    private val paypalClient: PayPalClient = PayPalClient(applicationContext, buildCoreConfig())

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

    var userIdentity: PayPalUserIdentity?
        get() = _uiState.value.userIdentity
        set(value) {
            _uiState.update { it.copy(userIdentity = value) }
        }

    var userAction: PayPalUserAction
        get() = _uiState.value.userAction
        set(value) {
            _uiState.update { it.copy(userAction = value) }
        }

    private fun createPayPalSession() {
        paypalClient.createPayPalSession(
            tokenType = TokenType.VAULT_ID,
            userIdentity = _uiState.value.userIdentity,
            urlConfig = DemoConstants.returnToAppUrlConfig,
            userAction = _uiState.value.userAction
        )
    }

    fun createSetupToken() {
        createPayPalSession()
        viewModelScope.launch {
            createSetupTokenState = ActionState.Loading
            createSetupTokenState = createPayPalSetupTokenUseCase().mapToActionState()
        }
    }

    private val createdSetupToken: PayPalSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    fun vaultSetupToken(activity: Activity) {
        val setupTokenId = createdSetupToken?.id

        if (setupTokenId == null) {
            vaultPayPalState = ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            vaultPayPalState = ActionState.Loading

            paypalClient.vault(activity, setupTokenId) { result ->
                when (result) {
                    is PayPalPresentAuthChallengeResult.Success -> {
                        // do nothing; wait for web vault approval to return to the app
                    }

                    is PayPalPresentAuthChallengeResult.Failure ->
                        vaultPayPalState = ActionState.Failure(result.error)
                }
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
        paypalClient.finishVault(intent)?.let { result ->
            vaultPayPalState = when (result) {
                is PayPalFinishVaultResult.Success -> ActionState.Success(result)
                is PayPalFinishVaultResult.Failure -> ActionState.Failure(result.error)
                PayPalFinishVaultResult.Canceled ->
                    ActionState.Failure(Exception("USER CANCELED"))

                PayPalFinishVaultResult.NoResult -> {
                    // no result; re-enable PayPal button so user can retry
                    ActionState.Idle
                }
            }
        }
    }
}
