package com.paypal.android.ui.paypalwebvault

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebVaultListener
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreatePayPalPaymentTokenUseCase
import com.paypal.android.usecase.CreatePayPalSetupTokenUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import com.paypal.android.usecase.UseCaseResult
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
) : ViewModel(), PayPalWebVaultListener {

    companion object {
        const val URL_SCHEME = "com.paypal.android.demo"
    }

    private val _uiState = MutableStateFlow(PayPalWebVaultUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var paypalClient: PayPalWebCheckoutClient
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
            createSetupTokenState = createPayPalSetupTokenUseCase().asActionState()
        }
    }

    private val createdSetupToken: PayPalSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    fun vaultSetupToken(activity: AppCompatActivity) {
        val setupTokenId = createdSetupToken?.id
        val setupTokenApprovalHref = createdSetupToken?.approveVaultHref
        if (setupTokenId == null || setupTokenApprovalHref == null) {
            vaultPayPalState = ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            val request = PayPalWebVaultRequest(setupTokenId, setupTokenApprovalHref)
            viewModelScope.launch {
                vaultSetupTokenWithRequest(activity, request)
            }
        }
    }

    private suspend fun vaultSetupTokenWithRequest(
        activity: AppCompatActivity,
        request: PayPalWebVaultRequest
    ) {
        vaultPayPalState = ActionState.Loading
        when (val clientIdResult = getClientIdUseCase()) {
            is UseCaseResult.Failure -> {
                vaultPayPalState = clientIdResult.asActionState()
            }

            is UseCaseResult.Success -> {
                val coreConfig = CoreConfig(clientIdResult.value)
                payPalDataCollector = PayPalDataCollector(coreConfig)

                paypalClient = PayPalWebCheckoutClient(activity, coreConfig, URL_SCHEME)
                paypalClient.vaultListener = this@PayPalWebVaultViewModel

                paypalClient.vault(request)
            }
        }
    }

    fun createPaymentToken() {
        createPaymentTokenState = ActionState.Loading
        viewModelScope.launch {
            createPaymentTokenState =
                createPayPalPaymentTokenUseCase(createdSetupToken!!).asActionState()
        }
    }

    override fun onPayPalWebVaultSuccess(result: PayPalWebVaultResult) {
        vaultPayPalState = ActionState.Success(result)
    }

    override fun onPayPalWebVaultFailure(error: PayPalSDKError) {
        vaultPayPalState = ActionState.Failure(error)
    }

    override fun onPayPalWebVaultCanceled() {
        vaultPayPalState = ActionState.Failure(Exception("USER CANCELED"))
    }
}
