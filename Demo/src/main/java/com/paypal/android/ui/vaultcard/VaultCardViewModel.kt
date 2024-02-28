package com.paypal.android.ui.vaultcard

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardVaultListener
import com.paypal.android.cardpayments.CardVaultRequest
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.models.TestCard
import com.paypal.android.ui.approveorder.DateString
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreateCardPaymentTokenUseCase
import com.paypal.android.usecase.CreateCardSetupTokenUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import com.paypal.android.api.services.SDKSampleServerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultCardViewModel @Inject constructor(
    val getClientIdUseCase: GetClientIdUseCase,
    val createSetupTokenUseCase: CreateCardSetupTokenUseCase,
    val createPaymentTokenUseCase: CreateCardPaymentTokenUseCase
) : ViewModel() {

    private lateinit var cardClient: CardClient

    private val _uiState = MutableStateFlow(VaultCardUiState())
    val uiState = _uiState.asStateFlow()

    private var createSetupTokenState
        get() = _uiState.value.createSetupTokenState
        set(value) {
            _uiState.update { it.copy(createSetupTokenState = value) }
        }

    private val createdSetupToken: CardSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    private var vaultCardState
        get() = _uiState.value.vaultCardState
        set(value) {
            _uiState.update { it.copy(vaultCardState = value) }
        }

    private var createPaymentTokenState
        get() = _uiState.value.createPaymentTokenState
        set(value) {
            _uiState.update { it.copy(createPaymentTokenState = value) }
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

    var shouldRequest3DS: Boolean
        get() = _uiState.value.shouldRequest3DS
        set(value) {
            _uiState.update { it.copy(shouldRequest3DS = value) }
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
            createSetupTokenState = ActionState.Loading
            val perform3DS = _uiState.value.shouldRequest3DS
            createSetupTokenState = createSetupTokenUseCase(perform3DS).mapToActionState()
        }
    }

    fun updateSetupToken(activity: AppCompatActivity) {
        val setupToken = createdSetupToken
        if (setupToken == null) {
            vaultCardState = ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            viewModelScope.launch {
                updateSetupTokenWithId(activity, setupToken.id)
            }
        }
    }

    private suspend fun updateSetupTokenWithId(activity: AppCompatActivity, setupTokenId: String) {
        vaultCardState = ActionState.Loading

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                vaultCardState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                val clientId = clientIdResult.value
                val configuration = CoreConfig(clientId = clientId)
                cardClient = CardClient(activity, configuration)
                cardClient.cardVaultListener = object : CardVaultListener {
                    override fun onVaultSuccess(result: CardVaultResult) {
                        vaultCardState = ActionState.Success(result)
                    }

                    override fun onVaultFailure(error: PayPalSDKError) {
                        vaultCardState = ActionState.Failure(error)
                    }
                }

                val card = parseCard(_uiState.value)
                val cardVaultRequest = CardVaultRequest(setupTokenId, card)
                cardClient.vault(activity, cardVaultRequest)
            }
        }
    }

    fun createPaymentToken() {
        val setupToken = createdSetupToken
        if (setupToken == null) {
            vaultCardState = ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            viewModelScope.launch {
                createPaymentTokenState = createPaymentTokenUseCase(setupToken).mapToActionState()
            }
        }
    }

    private fun parseCard(uiState: VaultCardUiState): Card {
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
