package com.paypal.android.ui.approveorder

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardPresentAuthChallengeResult
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
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
    private val createOrderUseCase: CreateOrderUseCase,
    private val getClientIdUseCase: GetClientIdUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
) : ViewModel() {

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    private val _uiState = MutableStateFlow(ApproveOrderUiState())
    val uiState = _uiState.asStateFlow()

    private var cardClient: CardClient? = null
    private lateinit var payPalDataCollector: PayPalDataCollector

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

    fun approveOrder(activity: AppCompatActivity) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            approveOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                approveOrderWithId(activity, orderId)
            }
        }
    }

    private suspend fun approveOrderWithId(activity: AppCompatActivity, orderId: String) {
        approveOrderState = ActionState.Loading

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                approveOrderState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                val clientId = clientIdResult.value
                val coreConfig = CoreConfig(clientId = clientId)
                payPalDataCollector = PayPalDataCollector(coreConfig)

                cardClient = CardClient(activity, coreConfig)
                cardClient?.approveOrderListener = object : ApproveOrderListener {
                    override fun onApproveOrderSuccess(result: CardResult) {
                        approveOrderState = ActionState.Success(result)
                    }

                    override fun onAuthorizationRequired(authChallenge: CardAuthChallenge) {
                        cardClient?.presentAuthChallenge(activity, authChallenge)
                            ?.let { presentAuthResult ->
                                when (presentAuthResult) {
                                    is CardPresentAuthChallengeResult.Success -> {
                                        authState = presentAuthResult.authState
                                    }

                                    is CardPresentAuthChallengeResult.Failure -> {
                                        approveOrderState =
                                            ActionState.Failure(presentAuthResult.error)
                                    }
                                }
                            }
                    }

                    override fun onApproveOrderFailure(error: PayPalSDKError) {
                        approveOrderState = ActionState.Failure(error)
                    }

                    override fun onApproveOrderCanceled() {
                        approveOrderState = ActionState.Failure(Exception("USER CANCELED"))
                    }

                    override fun onApproveOrderThreeDSecureWillLaunch() {
                        Log.d(TAG, "3DS Auth Requested")
                    }

                    override fun onApproveOrderThreeDSecureDidFinish() {
                        Log.d(TAG, "3DS Success")
                    }
                }

                val cardRequest = mapUIStateToCardRequestWithOrderId(orderId)
                cardClient?.approveOrder(activity, cardRequest)
            }
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

    override fun onCleared() {
        super.onCleared()
        cardClient?.removeObservers()
    }

    fun handleBrowserSwitchResult(activity: FragmentActivity) {
        authState?.let { cardClient?.completeAuthChallenge(activity.intent, it) }
    }
}
