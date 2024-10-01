package com.paypal.android.ui.approveorder

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardApproveOrderAuthResult
import com.paypal.android.cardpayments.CardApproveOrderRequest
import com.paypal.android.cardpayments.CardApproveOrderResult
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardAuthChallengeResult
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.models.TestCard
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApproveOrderViewModel @Inject constructor(
    application: Application,
    private val createOrderUseCase: CreateOrderUseCase,
    private val getClientIdUseCase: GetClientIdUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
) : AndroidViewModel(application) {

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    private val cardClient = CardClient(application.applicationContext)
    private val payPalDataCollector = PayPalDataCollector(application.applicationContext)

    private val _uiState = MutableStateFlow(ApproveOrderUiState())
    val uiState = _uiState.asStateFlow()

    private var coreConfig: CoreConfig? = null
    private var authState: String? = null

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = uiState.value.run {
                OrderRequest(intentOption, shouldVault == StoreInVaultOption.ON_SUCCESS)
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun approveOrder() {
        val orderId = createdOrder?.id
        if (orderId == null) {
            approveOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                approveOrderWithId(orderId)
            }
        }
    }

    private suspend fun approveOrderWithId(orderId: String) {
        approveOrderState = ActionState.Loading

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                approveOrderState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                val clientId = clientIdResult.value
                coreConfig = CoreConfig(clientId = clientId)
                val cardRequest = mapUIStateToCardRequestWithOrderId(orderId, coreConfig!!)

                when (val result = cardClient.approveOrder(cardRequest)) {
                    is CardApproveOrderResult.Success -> {
                        approveOrderState = ActionState.Success(result)
                    }

                    is CardApproveOrderResult.AuthorizationRequired -> {
                        authChallenge = result.authChallenge
                        approveOrderState =
                            ActionState.Failure(Exception("Authorization Required."))
                    }

                    is CardApproveOrderResult.Failure -> {
                        approveOrderState = ActionState.Failure(result.error)
                    }
                }
            }
        }
    }

    fun checkIntentForResult(intent: Intent) {
        if (authChallengeState.isComplete) return
        authState?.let { state ->
            when (val result = cardClient.checkIfApproveOrderAuthComplete(intent, state)) {
                is CardApproveOrderAuthResult.Success -> {
                    authChallengeState = ActionState.Success(result)
                    authState = null
                }

                is CardApproveOrderAuthResult.Failure -> {
                    authChallengeState = ActionState.Failure(result.error)
                    authState = null
                }

                is CardApproveOrderAuthResult.NoResult -> {
                    // reset loader
                    authChallengeState = ActionState.Idle
                }
            }
        }
    }

    fun completeOrder() {
        val orderId = createdOrder?.id
        if (orderId == null) {
            completeOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                completeOrderState = ActionState.Loading
                val dataCollectorRequest =
                    PayPalDataCollectorRequest(
                        config = coreConfig!!,
                        hasUserLocationConsent = false
                    )
                val cmid = payPalDataCollector.collectDeviceData(dataCollectorRequest)
                completeOrderState =
                    completeOrderUseCase(orderId, intentOption, cmid).mapToActionState()
            }
        }
    }

    private fun mapUIStateToCardRequestWithOrderId(orderId: String, config: CoreConfig) =
        uiState.value.run {
            // expiration date in UI State needs to be formatted because it uses a visual transformation
            val dateString = DateString(cardExpirationDate)
            val card = Card(
                number = cardNumber,
                expirationMonth = dateString.formattedMonth,
                expirationYear = dateString.formattedYear,
                securityCode = cardSecurityCode
            )
            CardApproveOrderRequest(config, orderId, card, APP_RETURN_URL, scaOption)
        }

    fun presentAuthChallenge(activity: ComponentActivity, authChallenge: CardAuthChallenge) {
        authChallengeState = ActionState.Loading
        when (val launchResult = cardClient.presentAuthChallenge(activity, authChallenge)) {
            is CardAuthChallengeResult.Success -> {
                authState = launchResult.authState
            }

            is CardAuthChallengeResult.Failure -> {
                authChallengeState = ActionState.Failure(launchResult.error)
            }
        }
    }

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    var approveOrderState
        get() = _uiState.value.approveOrderState
        set(value) {
            _uiState.update { it.copy(approveOrderState = value) }
        }

    private var completeOrderState
        get() = _uiState.value.completeOrderState
        set(value) {
            _uiState.update { it.copy(completeOrderState = value) }
        }

    private var authChallenge
        get() = _uiState.value.authChallenge
        set(value) {
            _uiState.update { it.copy(authChallenge = value) }
        }

    private var authChallengeState
        get() = _uiState.value.authChallengeState
        set(value) {
            _uiState.update { it.copy(authChallengeState = value) }
        }

    var scaOption: SCA
        get() = _uiState.value.scaOption
        set(value) {
            _uiState.update { it.copy(scaOption = value) }
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

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    var shouldVault: StoreInVaultOption
        get() = _uiState.value.shouldVault
        set(value) {
            _uiState.update { it.copy(shouldVault = value) }
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
}
