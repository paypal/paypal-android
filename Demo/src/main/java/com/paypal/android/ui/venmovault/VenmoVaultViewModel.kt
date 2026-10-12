package com.paypal.android.ui.venmovault

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.VenmoSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.customenvironment.CustomEnvironmentRepository
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreateVenmoPaymentTokenUseCase
import com.paypal.android.usecase.CreateVenmoSetupTokenUseCase
import com.paypal.android.venmo.VenmoClient
import com.paypal.android.venmo.VenmoFinishVaultResult
import com.paypal.android.venmo.VenmoPresentAuthChallengeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VenmoVaultViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createVenmoSetupTokenUseCase: CreateVenmoSetupTokenUseCase,
    val createVenmoPaymentTokenUseCase: CreateVenmoPaymentTokenUseCase,
    private val customEnvironmentRepository: CustomEnvironmentRepository,
) : ViewModel() {

    private fun buildCoreConfig(): CoreConfig =
        customEnvironmentRepository.getCoreConfig(
            CoreConfig(SDKSampleServerAPI.clientId, SDKSampleServerAPI.merchantId)
        )

    private val venmoClient: VenmoClient = VenmoClient(applicationContext, buildCoreConfig())

    private val _uiState = MutableStateFlow(VenmoVaultUiState())
    val uiState = _uiState.asStateFlow()

    private var createSetupTokenState
        get() = _uiState.value.createSetupTokenState
        set(value) {
            _uiState.update { it.copy(createSetupTokenState = value) }
        }

    private var vaultVenmoState
        get() = _uiState.value.vaultVenmoState
        set(value) {
            _uiState.update { it.copy(vaultVenmoState = value) }
        }

    private var createPaymentTokenState
        get() = _uiState.value.createPaymentTokenState
        set(value) {
            _uiState.update { it.copy(createPaymentTokenState = value) }
        }

    fun createSetupToken() {
        viewModelScope.launch {
            createSetupTokenState = ActionState.Loading
            createSetupTokenState = createVenmoSetupTokenUseCase().mapToActionState()
        }
    }

    private val createdSetupToken: VenmoSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    fun vaultSetupToken(activity: ComponentActivity) {
        val setupTokenId = createdSetupToken?.id

        if (setupTokenId == null) {
            vaultVenmoState = ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            vaultVenmoState = ActionState.Loading

            venmoClient.vault(activity, setupTokenId) { result ->
                when (result) {
                    is VenmoPresentAuthChallengeResult.Success -> {
                        // do nothing; wait for user to authenticate Venmo vault in Chrome Custom Tab
                    }

                    is VenmoPresentAuthChallengeResult.Failure ->
                        vaultVenmoState = ActionState.Failure(result.error)
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
                    createVenmoPaymentTokenUseCase(setupToken).mapToActionState()
            }
        }
    }

    fun completeAuthChallenge(intent: Intent) {
        when (val result = venmoClient.finishVault(intent)) {
            is VenmoFinishVaultResult.Success -> vaultVenmoState = ActionState.Success(result)
            is VenmoFinishVaultResult.Failure -> vaultVenmoState = ActionState.Failure(result.error)
            is VenmoFinishVaultResult.Canceled ->
                vaultVenmoState = ActionState.Failure(Exception("USER CANCELED"))

            VenmoFinishVaultResult.NoResult -> {
                // no result; re-enable Venmo button so user can retry
            }
        }
    }
}
