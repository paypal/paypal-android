package com.paypal.android.ui.paypalwebvault

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
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
class PayPalWebVaultViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createPayPalSetupTokenUseCase: CreatePayPalSetupTokenUseCase,
    val createPayPalPaymentTokenUseCase: CreatePayPalPaymentTokenUseCase,
) : ViewModel() {

    companion object {
        const val URL_SCHEME = "com.paypal.android.demo"
    }

    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId)
    private val paypalClient = PayPalWebCheckoutClient(applicationContext, coreConfig, URL_SCHEME)

    private val _uiState = MutableStateFlow(PayPalWebVaultUiState())
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

    private fun vaultSetupTokenWithRequest(
        activity: ComponentActivity,
        request: PayPalWebVaultRequest
    ) {
        vaultPayPalState = ActionState.Loading

        when (val result = paypalClient.vault(activity, request)) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // do nothing; wait for Chrome Custom Tab to return control back to the Demo app
            }

            is PayPalPresentAuthChallengeResult.Failure ->
                vaultPayPalState = ActionState.Failure(result.error)
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
                is PayPalWebCheckoutFinishVaultResult.Success -> ActionState.Success(result)
                is PayPalWebCheckoutFinishVaultResult.Failure ->
                    ActionState.Failure(result.error)

                PayPalWebCheckoutFinishVaultResult.Canceled ->
                    ActionState.Failure(Exception("USER CANCELED"))

                // no result; re-enable PayPal button so user can retry
                PayPalWebCheckoutFinishVaultResult.NoResult -> ActionState.Idle
            }
        }
    }
}
