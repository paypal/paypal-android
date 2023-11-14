package com.paypal.android.ui.vault

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardVaultListener
import com.paypal.android.cardpayments.CardVaultRequest
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.models.TestCard
import com.paypal.android.ui.approveorder.DateString
import com.paypal.android.usecase.CreatePaymentTokenUseCase
import com.paypal.android.usecase.CreateSetupTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    val sdkSampleServerAPI: SDKSampleServerAPI,
    val createSetupTokenUseCase: CreateSetupTokenUseCase,
    val createPaymentTokenUseCase: CreatePaymentTokenUseCase
) : ViewModel() {

    private lateinit var cardClient: CardClient

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState = _uiState.asStateFlow()

    var setupToken: SetupToken?
        get() = _uiState.value.setupToken
        set(value) {
            _uiState.update { it.copy(setupToken = value) }
        }

    var paymentToken: PaymentToken?
        get() = _uiState.value.paymentToken
        set(value) {
            _uiState.update { it.copy(paymentToken = value) }
        }

    var isCreateSetupTokenLoading: Boolean
        get() = _uiState.value.isCreateSetupTokenLoading
        set(value) {
            _uiState.update { it.copy(isCreateSetupTokenLoading = value) }
        }

    var isUpdateSetupTokenLoading: Boolean
        get() = _uiState.value.isUpdateSetupTokenLoading
        set(value) {
            _uiState.update { it.copy(isUpdateSetupTokenLoading = value) }
        }

    var isCreatePaymentTokenLoading: Boolean
        get() = _uiState.value.isCreatePaymentTokenLoading
        set(value) {
            _uiState.update { it.copy(isCreatePaymentTokenLoading = value) }
        }

    var customerId: String
        get() = _uiState.value.customerId
        set(value) {
            _uiState.update { it.copy(customerId = value) }
        }

    var cardNumber: String
        get() = _uiState.value.cardNumber
        set(value) {
            _uiState.update { it.copy(cardNumber = value) }
        }

    var cardExpirationDate: String
        get() = _uiState.value.cardExpirationDate
        set(value) {
            _uiState.update { it.copy(cardExpirationDate = value) }
        }

    var cardSecurityCode: String
        get() = _uiState.value.cardSecurityCode
        set(value) {
            _uiState.update { it.copy(cardSecurityCode = value) }
        }

    var cardVaultResult: CardVaultResult?
        get() = _uiState.value.cardVaultResult
        set(value) {
            _uiState.update { it.copy(cardVaultResult = value) }
        }

    fun prefillCard(testCard: TestCard) {
        val card = testCard.card
        _uiState.update { currentState ->
            currentState.copy(
                cardNumber = card.number,
                cardExpirationDate = card.run { "$expirationMonth$expirationYear" },
                cardSecurityCode = card.securityCode
            )
        }
    }

    fun createSetupToken() {
        viewModelScope.launch {
            isCreateSetupTokenLoading = true
            setupToken = createSetupTokenUseCase(customerId)
            isCreateSetupTokenLoading = false
        }
    }

    fun updateSetupToken(activity: AppCompatActivity) {
        viewModelScope.launch {
            isUpdateSetupTokenLoading = true
            val clientId = sdkSampleServerAPI.fetchClientId()

            val configuration = CoreConfig(clientId = clientId)
            cardClient = CardClient(activity, configuration)
            cardClient.cardVaultListener = object : CardVaultListener {
                override fun onVaultSuccess(result: CardVaultResult) {
                    isUpdateSetupTokenLoading = false
                    cardVaultResult = result
                }

                override fun onVaultFailure(error: PayPalSDKError) {
                    isUpdateSetupTokenLoading = false
                    // TODO: handle error
                }
            }

            val card = parseCard(_uiState.value)
            val cardVaultRequest = CardVaultRequest(setupToken!!.id, card)
            cardClient.vault(activity, cardVaultRequest)
        }
    }

    fun createPaymentToken() {
        viewModelScope.launch {
            isCreatePaymentTokenLoading = true
            paymentToken = createPaymentTokenUseCase(setupToken!!)
            isCreatePaymentTokenLoading = false
        }
    }

    private fun parseCard(uiState: VaultUiState): Card {
        // expiration date in UI State needs to be formatted because it uses a visual transformation
        val dateString = DateString(uiState.cardExpirationDate)
        return Card(
            number = uiState.cardNumber,
            expirationMonth = dateString.formattedMonth,
            expirationYear = dateString.formattedYear,
            securityCode = uiState.cardSecurityCode
        )
    }
}
