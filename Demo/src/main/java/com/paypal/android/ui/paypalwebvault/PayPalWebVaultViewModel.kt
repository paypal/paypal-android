package com.paypal.android.ui.paypalwebvault

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.PayPalSetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultListener
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.usecase.CreatePayPalPaymentTokenUseCase
import com.paypal.android.usecase.CreatePayPalSetupTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalWebVaultViewModel @Inject constructor(
    val createPayPalSetupTokenUseCase: CreatePayPalSetupTokenUseCase,
    val createPayPalPaymentTokenUseCase: CreatePayPalPaymentTokenUseCase,
    val sdkSampleServerAPI: SDKSampleServerAPI
) : ViewModel(), PayPalWebVaultListener {

    companion object {
        const val URL_SCHEME = "com.paypal.android.demo"
    }

    private val _uiState = MutableStateFlow(PayPalWebVaultUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var paypalClient: PayPalWebCheckoutClient
    private lateinit var payPalDataCollector: PayPalDataCollector

    private var isCreateSetupTokenLoading: Boolean
        get() = _uiState.value.isCreateSetupTokenLoading
        set(value) {
            _uiState.update { it.copy(isCreateSetupTokenLoading = value) }
        }

    private var isCreatePaymentTokenLoading: Boolean
        get() = _uiState.value.isCreatePaymentTokenLoading
        set(value) {
            _uiState.update { it.copy(isCreatePaymentTokenLoading = value) }
        }

    private var isVaultPayPalLoading: Boolean
        get() = _uiState.value.isVaultPayPalLoading
        set(value) {
            _uiState.update { it.copy(isVaultPayPalLoading = value) }
        }

    var vaultCustomerId: String
        get() = _uiState.value.vaultCustomerId
        set(value) {
            _uiState.update { it.copy(vaultCustomerId = value) }
        }

    private var setupToken: PayPalSetupToken?
        get() = _uiState.value.setupToken
        set(value) {
            _uiState.update { it.copy(setupToken = value) }
        }

    private var paymentToken: PayPalPaymentToken?
        get() = _uiState.value.paymentToken
        set(value) {
            _uiState.update { it.copy(paymentToken = value) }
        }

    var payPalWebVaultResult: PayPalWebVaultResult?
        get() = _uiState.value.payPalWebVaultResult
        set(value) {
            _uiState.update { it.copy(payPalWebVaultResult = value) }
        }

    var payPalWebVaultError: PayPalSDKError?
        get() = _uiState.value.payPalWebVaultError
        set(value) {
            _uiState.update { it.copy(payPalWebVaultError = value) }
        }

    var isVaultingCanceled: Boolean
        get() = _uiState.value.isVaultingCanceled
        set(value) {
            _uiState.update { it.copy(isVaultingCanceled = value) }
        }

    fun createSetupToken() {
        viewModelScope.launch {
            isCreateSetupTokenLoading = true

            setupToken = createPayPalSetupTokenUseCase()
            isCreateSetupTokenLoading = false
        }
    }

    fun updateSetupToken(activity: AppCompatActivity) {
        viewModelScope.launch {
            isVaultPayPalLoading = true
            val request = setupToken!!.run { PayPalWebVaultRequest(id, approveVaultHref!!) }

            val clientId = sdkSampleServerAPI.fetchClientId()
            val coreConfig = CoreConfig(clientId)
            payPalDataCollector = PayPalDataCollector(coreConfig)

            paypalClient = PayPalWebCheckoutClient(activity, coreConfig, URL_SCHEME)
            paypalClient.vaultListener = this@PayPalWebVaultViewModel

            paypalClient.vault(request)
        }
    }

    fun createPaymentToken() {
        viewModelScope.launch {
            isCreatePaymentTokenLoading = true
            paymentToken = createPayPalPaymentTokenUseCase(setupToken!!)
            isCreatePaymentTokenLoading = false
        }
    }

    override fun onPayPalWebVaultSuccess(result: PayPalWebVaultResult) {
        payPalWebVaultResult = result
        isVaultPayPalLoading = false
    }

    override fun onPayPalWebVaultFailure(error: PayPalSDKError) {
        payPalWebVaultError = error
        isVaultPayPalLoading = false
    }

    override fun onPayPalWebVaultCanceled() {
        isVaultingCanceled = true
        isVaultPayPalLoading = false
    }
}
