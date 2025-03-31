package com.paypal.android.ui.paypalwebvault

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreatePayPalPaymentTokenUseCase
import com.paypal.android.usecase.CreatePayPalSetupTokenUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalWebVaultViewModel @Inject constructor(
    val getClientIdUseCase: GetClientIdUseCase,
    val createPayPalSetupTokenUseCase: CreatePayPalSetupTokenUseCase,
    val createPayPalPaymentTokenUseCase: CreatePayPalPaymentTokenUseCase,
) : ViewModel() {

    companion object {
        const val URL_SCHEME = "com.paypal.android.demo"
    }

    private var authState: String? = null
    private val _uiState = MutableStateFlow(PayPalWebVaultUiState())
    val uiState = _uiState.asStateFlow()

    private var paypalClient: PayPalWebCheckoutClient? = null
    private lateinit var payPalDataCollector: PayPalDataCollector
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

    fun createSetupToken() {
        viewModelScope.launch {
            createSetupTokenState = ActionState.Loading
            createSetupTokenState = createPayPalSetupTokenUseCase().mapToActionState()
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
                val request = PayPalWebVaultRequest(setupTokenId)
                vaultSetupTokenWithRequest(activity, request)
            }
        }
    }

    private suspend fun vaultSetupTokenWithRequest(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ) {
        vaultPayPalState = ActionState.Loading
        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                vaultPayPalState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                val coreConfig = CoreConfig(clientIdResult.value)
                payPalDataCollector = PayPalDataCollector(coreConfig)

                paypalClient = PayPalWebCheckoutClient(activity, coreConfig, URL_SCHEME)
                when (val result = paypalClient?.vault(activity, request)) {
                    is PayPalPresentAuthChallengeResult.Success ->
                        authState = result.authState

                    is PayPalPresentAuthChallengeResult.Failure ->
                        vaultPayPalState = ActionState.Failure(result.error)

                    null -> {
                        // do nothing for now
                    }
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

    private fun checkIfPayPalAuthFinished(intent: Intent): PayPalWebCheckoutFinishVaultResult? =
        authState?.let { paypalClient?.finishVault(intent, it) }

    fun completeAuthChallenge(intent: Intent) {
        checkIfPayPalAuthFinished(intent)?.let { result ->
            when (result) {
                is PayPalWebCheckoutFinishVaultResult.Success -> {
                    vaultPayPalState = ActionState.Success(result)
                    discardAuthState()
                }

                is PayPalWebCheckoutFinishVaultResult.Failure -> {
                    vaultPayPalState = ActionState.Failure(result.error)
                    discardAuthState()
                }

                PayPalWebCheckoutFinishVaultResult.Canceled -> {
                    vaultPayPalState = ActionState.Failure(Exception("USER CANCELED"))
                    discardAuthState()
                }

                PayPalWebCheckoutFinishVaultResult.NoResult -> {
                    // no result; re-enable PayPal button so user can retry
                    vaultPayPalState = ActionState.Idle
                }
            }
        }
    }

    private fun discardAuthState() {
        authState = null
    }
}
