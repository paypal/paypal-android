package com.paypal.android.ui.vaultcard

import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardPresentAuthChallengeResult
import com.paypal.android.cardpayments.CardVaultListener
import com.paypal.android.cardpayments.CardVaultRequest
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.models.TestCard
import com.paypal.android.ui.approveorder.DateString
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreateCardPaymentTokenUseCase
import com.paypal.android.usecase.CreateCardSetupTokenUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import com.paypal.android.usecase.GetSetupTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultCardViewModel @Inject constructor(
    val getClientIdUseCase: GetClientIdUseCase,
    val getSetupTokenUseCase: GetSetupTokenUseCase,
    val createSetupTokenUseCase: CreateCardSetupTokenUseCase,
    val createPaymentTokenUseCase: CreateCardPaymentTokenUseCase
) : ViewModel() {

    private var authState: String? = null
    private var cardClient: CardClient? = null

    private val _uiState = MutableStateFlow(VaultCardUiState())
    val uiState = _uiState.asStateFlow()

    private var createSetupTokenState
        get() = _uiState.value.createSetupTokenState
        set(value) {
            _uiState.update { it.copy(createSetupTokenState = value) }
        }

    private val createdSetupToken: CardSetupToken?
        get() = (createSetupTokenState as? ActionState.Success)?.value

    private var updateSetupTokenState
        get() = _uiState.value.updateSetupTokenState
        set(value) {
            _uiState.update { it.copy(updateSetupTokenState = value) }
        }

    private var authChallengeState
        get() = _uiState.value.authChallengeState
        set(value) {
            _uiState.update { it.copy(authChallengeState = value) }
        }

    private var createPaymentTokenState
        get() = _uiState.value.createPaymentTokenState
        set(value) {
            _uiState.update { it.copy(createPaymentTokenState = value) }
        }

    private var refreshSetupTokenState
        get() = _uiState.value.refreshSetupTokenState
        set(value) {
            _uiState.update { it.copy(refreshSetupTokenState = value) }
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

    var scaOption: SCA
        get() = _uiState.value.scaOption
        set(value) {
            _uiState.update { it.copy(scaOption = value) }
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
            val sca = _uiState.value.scaOption
            createSetupTokenState = createSetupTokenUseCase(sca).mapToActionState()
        }
    }

    fun updateSetupToken(activity: ComponentActivity) {
        val setupToken = createdSetupToken
        if (setupToken == null) {
            updateSetupTokenState =
                ActionState.Failure(Exception("Create a setup token to continue."))
        } else {
            viewModelScope.launch {
                updateSetupTokenWithId(activity, setupToken.id)
            }
        }
    }

    private suspend fun updateSetupTokenWithId(activity: ComponentActivity, setupTokenId: String) {
        updateSetupTokenState = ActionState.Loading

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                updateSetupTokenState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                val clientId = clientIdResult.value
                val configuration = CoreConfig(clientId = clientId)
                cardClient = CardClient(activity, configuration)
                cardClient?.cardVaultListener = object : CardVaultListener {

                    override fun onVaultSuccess(result: CardVaultResult) {
                        updateSetupTokenState = ActionState.Success(result)
                    }

                    override fun onVaultFailure(error: PayPalSDKError) {
                        updateSetupTokenState = ActionState.Failure(error)
                    }
                }

                val card = parseCard(_uiState.value)
                val returnUrl = "com.paypal.android.demo://example.com/returnUrl"
                val cardVaultRequest = CardVaultRequest(setupTokenId, card, returnUrl)
                cardClient?.vault(activity, cardVaultRequest)
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

    fun presentAuthChallenge(activity: ComponentActivity, authChallenge: CardAuthChallenge) {
        authChallengeState = ActionState.Loading

        // change listener behavior to handle auth result
        cardClient?.cardVaultListener = object : CardVaultListener {
            override fun onVaultSuccess(result: CardVaultResult) {
                viewModelScope.launch {
                    refreshSetupTokenState =
                        getSetupTokenUseCase(result.setupTokenId).mapToActionState()
                    authChallengeState = ActionState.Success(result)
                }
            }

            override fun onVaultFailure(error: PayPalSDKError) {
                authChallengeState = ActionState.Failure(error)
            }
        }

        cardClient?.presentAuthChallenge(activity, authChallenge)?.let { result ->
            when (result) {
                is CardPresentAuthChallengeResult.Success -> {
                    authState = result.authState
                }
                is CardPresentAuthChallengeResult.Failure -> {
                    authChallengeState = ActionState.Failure(result.error)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cardClient?.removeObservers()
    }

    fun handleBrowserSwitchResult(activity: ComponentActivity) {
        authState?.let { cardClient?.completeAuthChallenge(activity.intent, it) }
    }
}
