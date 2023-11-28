package com.paypal.android.ui.paypalwebvault

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.SetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.models.PaymentMethod
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import com.paypal.android.usecase.CreateSetupTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PayPalWebVaultViewModel @Inject constructor(
    val createSetupTokenUseCase: CreateSetupTokenUseCase,
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

    var setupToken: SetupToken?
        get() = _uiState.value.setupToken
        set(value) {
            _uiState.update { it.copy(setupToken = value) }
        }

    fun createSetupToken() {
        viewModelScope.launch {
            isCreateSetupTokenLoading = true
            setupToken = createSetupTokenUseCase(PaymentMethod.PAYPAL, vaultCustomerId)
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
            paypalClient.listener = object : PayPalWebCheckoutListener {
                override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
                    TODO("Not yet implemented")
                }

                override fun onPayPalWebFailure(error: PayPalSDKError) {
                    TODO("Not yet implemented")
                }

                override fun onPayPalWebCanceled() {
                    TODO("Not yet implemented")
                }

            }
            // TODO: implement
            paypalClient.vault(activity, setupToken!!.id, setupToken!!.approveVaultHref!!)
            isUpdateSetupTokenLoading = false
        }
    }
}
