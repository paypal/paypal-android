package com.paypal.android.ui.paypalwebvault

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultAuthResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultListener
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
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
    application: Application,
    val getClientIdUseCase: GetClientIdUseCase,
    val createPayPalSetupTokenUseCase: CreatePayPalSetupTokenUseCase,
    val createPayPalPaymentTokenUseCase: CreatePayPalPaymentTokenUseCase,
) : AndroidViewModel(application), PayPalWebVaultListener {

    companion object {
        const val URL_SCHEME = "com.paypal.android.demo"
    }

    private val _uiState = MutableStateFlow(PayPalWebVaultUiState())
    val uiState = _uiState.asStateFlow()

    private val paypalClient = PayPalWebCheckoutClient(application.applicationContext, URL_SCHEME)
    private val payPalDataCollector = PayPalDataCollector(application.applicationContext)

    private var coreConfig: CoreConfig? = null
    private var authState: String? = null

    private var createSetupTokenState
        get() = _uiState.value.createSetupTokenState
        set(value) {
            _uiState.update { it.copy(createSetupTokenState = value) }
        }

    private var createPaymentTokenState
        get() = _uiState.value.createPaymentTokenState
        set(value) {
            _uiState.update { it.copy(createPaymentTokenState = value) }
        }

    private var authChallengeState
        get() = _uiState.value.authChallengeState
        set(value) {
            _uiState.update { it.copy(authChallengeState = value) }
        }

    fun createSetupToken() {
        viewModelScope.launch {
            createSetupTokenState = ActionState.Loading
            createSetupTokenState = createPayPalSetupTokenUseCase().mapToActionState()
        }
    }

    private val createdSetupToken: PayPalSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    fun vaultSetupToken(activity: AppCompatActivity) {
        val setupTokenId = createdSetupToken?.id
        if (setupTokenId == null) {
            authChallengeState = ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            viewModelScope.launch {
                vaultSetupTokenWithRequest(activity, setupTokenId)
            }
        }
    }

    private suspend fun vaultSetupTokenWithRequest(
        activity: AppCompatActivity,
        setupTokenId: String
    ) {
        authChallengeState = ActionState.Loading

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                authChallengeState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {

                coreConfig = CoreConfig(clientIdResult.value)
                val request = PayPalWebVaultRequest(coreConfig!!, setupTokenId)
                when (val vaultResult = paypalClient.vault(activity, request)) {
                    is PayPalWebCheckoutVaultResult.DidLaunchAuth -> {
                        authState = vaultResult.authState
                    }

                    is PayPalWebCheckoutVaultResult.Failure -> {
                        authChallengeState = ActionState.Failure(vaultResult.error)
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

    override fun onPayPalWebVaultSuccess(result: PayPalWebVaultResult) {
//        vaultPayPalState = ActionState.Success(result)
    }

    override fun onPayPalWebVaultFailure(error: PayPalSDKError) {
//        vaultPayPalState = ActionState.Failure(error)
    }

    override fun onPayPalWebVaultCanceled() {
//        vaultPayPalState = ActionState.Failure(Exception("USER CANCELED"))
    }

    fun checkIntentForResult(intent: Intent) = authState?.let { state ->
        when (val result = paypalClient.checkIfVaultAuthComplete(intent, state)) {
            is PayPalWebVaultAuthResult.Success -> {
                authChallengeState = ActionState.Success(result)
            }

            is PayPalWebVaultAuthResult.Failure -> {
                authChallengeState = ActionState.Failure(result.error)
            }

            PayPalWebVaultAuthResult.NoResult -> {
                // do nothing
            }

            PayPalWebVaultAuthResult.Canceled -> {
                authChallengeState = ActionState.Failure(Exception("User canceled"))
            }
        }
    }
}
