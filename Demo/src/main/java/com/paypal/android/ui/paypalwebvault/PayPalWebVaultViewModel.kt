package com.paypal.android.ui.paypalwebvault

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PayPalPaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutVaultListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutVaultResult
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
) : ViewModel() {

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
            _uiState.update { it.copy(isCreateSetupTokenLoading = value) }
        }

    private var isUpdateSetupTokenLoading: Boolean
        get() = _uiState.value.isUpdateSetupTokenLoading
        set(value) {
            _uiState.update { it.copy(isUpdateSetupTokenLoading = value) }
        }

    var vaultCustomerId: String
        get() = _uiState.value.vaultCustomerId
        set(value) {
            _uiState.update { it.copy(vaultCustomerId = value) }
        }

    private var setupToken: SetupToken?
        get() = _uiState.value.setupToken
        set(value) {
            _uiState.update { it.copy(setupToken = value) }
        }

    private var paymentToken: PayPalPaymentToken?
        get() = _uiState.value.paymentToken
        set(value) {
            _uiState.update { it.copy(paymentToken = value) }
        }

    var payPalWebCheckoutVaultResult: PayPalWebCheckoutVaultResult?
        get() = _uiState.value.payPalWebCheckoutVaultResult
        set(value) {
            _uiState.update { it.copy(payPalWebCheckoutVaultResult = value) }
        }

    var payPalWebCheckoutVaultError: PayPalSDKError?
        get() = _uiState.value.payPalWebCheckoutVaultError
        set(value) {
            _uiState.update { it.copy(payPalWebCheckoutVaultError = value) }
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
            isUpdateSetupTokenLoading = true
            val clientId = sdkSampleServerAPI.fetchClientId()
            val coreConfig = CoreConfig(clientId)
            payPalDataCollector = PayPalDataCollector(coreConfig)

            paypalClient = PayPalWebCheckoutClient(
                activity,
                coreConfig,
                "com.paypal.android.demo"
            )
            paypalClient.vaultListener = object : PayPalWebCheckoutVaultListener {
                override fun onPayPalWebVaultSuccess(result: PayPalWebCheckoutVaultResult) {
                    payPalWebCheckoutVaultResult = result
                    isUpdateSetupTokenLoading = false
                }

                override fun onPayPalWebVaultFailure(error: PayPalSDKError) {
                    payPalWebCheckoutVaultError = error
                    isUpdateSetupTokenLoading = false
                }

                override fun onPayPalWebVaultCanceled() {
                    isVaultingCanceled = true
                    isUpdateSetupTokenLoading = false
                }
            }

            paypalClient.vault(activity, setupToken!!.id, setupToken!!.approveVaultHref!!)
        }
    }

    fun createPaymentToken() {
        viewModelScope.launch {
            isCreatePaymentTokenLoading = true
            paymentToken = createPayPalPaymentTokenUseCase(setupToken!!)
            isCreatePaymentTokenLoading = false
        }
    }
}
