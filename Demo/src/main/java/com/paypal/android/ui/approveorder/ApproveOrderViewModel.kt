package com.paypal.android.ui.approveorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardApproveOrderResult
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardFinishApproveOrderResult
import com.paypal.android.cardpayments.CardPresentAuthChallengeResult
import com.paypal.android.cardpayments.CardRequest
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApproveOrderViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    private val createOrderUseCase: CreateOrderUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
) : ViewModel() {

    companion object {
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId)
    private val payPalDataCollector = PayPalDataCollector(coreConfig)
    private val cardClient = CardClient(applicationContext, coreConfig)

    private val _uiState = MutableStateFlow(ApproveOrderUiState())
    val uiState = _uiState.asStateFlow()

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = uiState.value.run {
                OrderRequest(intentOption, shouldVault == StoreInVaultOption.ON_SUCCESS)
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun approveOrder(activity: Activity) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            approveOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                approveOrderWithId(activity, orderId)
            }
        }
    }

    private fun approveOrderWithId(activity: Activity, orderId: String) {
        approveOrderState = ActionState.Loading

        val cardRequest = mapUIStateToCardRequestWithOrderId(orderId)
        cardClient.approveOrder(cardRequest) { result ->
            when (result) {
                is CardApproveOrderResult.Success -> {
                    val orderInfo = result.run {
                        OrderInfo(orderId, status, didAttemptThreeDSecureAuthentication)
                    }
                    approveOrderState = ActionState.Success(orderInfo)
                }

                is CardApproveOrderResult.AuthorizationRequired -> {
                    presentAuthChallenge(activity, result.authChallenge)
                }

                is CardApproveOrderResult.Failure -> {
                    approveOrderState = ActionState.Failure(result.error)
                }
            }
        }
    }

    private fun presentAuthChallenge(
        activity: Activity,
        authChallenge: CardAuthChallenge
    ) {
        when (val presentAuthResult = cardClient.presentAuthChallenge(activity, authChallenge)) {
            is CardPresentAuthChallengeResult.Success -> {
                // do nothing; wait for user to authenticate PayPal checkout in Chrome Custom Tab
            }

            is CardPresentAuthChallengeResult.Failure ->
                approveOrderState = ActionState.Failure(presentAuthResult.error)
        }
    }

    fun completeOrder(context: Context) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            completeOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                completeOrderState = ActionState.Loading
                val dataCollectorRequest =
                    PayPalDataCollectorRequest(hasUserLocationConsent = false)
                val cmid = payPalDataCollector.collectDeviceData(context, dataCollectorRequest)
                completeOrderState =
                    completeOrderUseCase(orderId, intentOption, cmid).mapToActionState()
            }
        }
    }

    private fun mapUIStateToCardRequestWithOrderId(orderId: String) = uiState.value.run {
        // expiration date in UI State needs to be formatted because it uses a visual transformation
        val dateString = DateString(cardExpirationDate)
        val card = Card(
            number = cardNumber,
            expirationMonth = dateString.formattedMonth,
            expirationYear = dateString.formattedYear,
            securityCode = cardSecurityCode
        )
        CardRequest(orderId, card, APP_RETURN_URL, scaOption)
    }

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    private var approveOrderState
        get() = _uiState.value.approveOrderState
        set(value) {
            _uiState.update { it.copy(approveOrderState = value) }
        }

    private var completeOrderState
        get() = _uiState.value.completeOrderState
        set(value) {
            _uiState.update { it.copy(completeOrderState = value) }
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

    fun completeAuthChallenge(intent: Intent) {
        cardClient.finishApproveOrder(intent)?.let { approveOrderResult ->
            when (approveOrderResult) {
                is CardFinishApproveOrderResult.Success -> {
                    val orderInfo = approveOrderResult.run {
                        OrderInfo(orderId, status, didAttemptThreeDSecureAuthentication)
                    }
                    approveOrderState = ActionState.Success(orderInfo)
                }

                is CardFinishApproveOrderResult.Failure ->
                    approveOrderState = ActionState.Failure(approveOrderResult.error)

                CardFinishApproveOrderResult.Canceled ->
                    approveOrderState = ActionState.Failure(Exception("USER CANCELED"))

                CardFinishApproveOrderResult.NoResult -> {
                    // no result; re-enable approve order button so user can retry
                    approveOrderState = ActionState.Idle
                }
            }
        }
    }
}
