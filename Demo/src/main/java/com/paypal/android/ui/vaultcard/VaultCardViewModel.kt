package com.paypal.android.ui.vaultcard

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardFinishVaultResult
import com.paypal.android.cardpayments.CardPresentAuthChallengeResult
import com.paypal.android.cardpayments.CardVaultRequest
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.ui.approveorder.SetupTokenInfo
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.models.TestCard
import com.paypal.android.ui.approveorder.DateString
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreateCardPaymentTokenUseCase
import com.paypal.android.usecase.CreateCardSetupTokenUseCase
import com.paypal.android.usecase.GetClientIdUseCase
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

                val card = parseCard(_uiState.value)
                val returnUrl = "com.paypal.android.demo://example.com/returnUrl"
                val cardVaultRequest = CardVaultRequest(setupTokenId, card, returnUrl)
                cardClient?.vault(cardVaultRequest) { result ->
                    when (result) {
                        is CardVaultResult.Success -> {
                            val setupTokenInfo = result.run {
                                SetupTokenInfo(
                                    setupTokenId,
                                    status,
                                    didAttemptThreeDSecureAuthentication
                                )
                            }
                            updateSetupTokenState = ActionState.Success(setupTokenInfo)
                        }

                        is CardVaultResult.AuthorizationRequired ->
                            presentAuthChallenge(activity, result.authChallenge)

                        is CardVaultResult.Failure ->
                            updateSetupTokenState = ActionState.Failure(result.error)
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
        cardClient?.presentAuthChallenge(activity, authChallenge)?.let { result ->
            when (result) {
                is CardPresentAuthChallengeResult.Success -> authState = result.authState
                is CardPresentAuthChallengeResult.Failure ->
                    updateSetupTokenState = ActionState.Failure(result.error)
            }
        }
    }

    private fun checkIfVaultFinished(intent: Intent): CardFinishVaultResult? =
        authState?.let { cardClient?.finishVault(intent, it) }

    fun completeAuthChallenge(intent: Intent) {
        checkIfVaultFinished(intent)?.let { vaultResult ->
            when (vaultResult) {
                is CardFinishVaultResult.Success -> {
                    val setupTokenInfo = vaultResult.run {
                        SetupTokenInfo(
                            setupTokenId,
                            status,
                            didAttemptThreeDSecureAuthentication
                        )
                    }
                    updateSetupTokenState = ActionState.Success(setupTokenInfo)
                    discardAuthState()
                }

                CardFinishVaultResult.Canceled -> {
                    updateSetupTokenState = ActionState.Failure(Exception("USER CANCELED"))
                    discardAuthState()
                }

                is CardFinishVaultResult.Failure -> {
                    updateSetupTokenState = ActionState.Failure(vaultResult.error)
                    discardAuthState()
                }

                CardFinishVaultResult.NoResult -> {
                    // no result; re-enable vault button so user can retry
                    updateSetupTokenState = ActionState.Idle
                }
            }
        }
    }

    private fun discardAuthState() {
        authState = null
    }
}
