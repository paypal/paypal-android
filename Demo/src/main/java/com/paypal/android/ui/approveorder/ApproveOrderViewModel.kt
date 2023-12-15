package com.paypal.android.ui.approveorder

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.models.OrderRequest
import com.paypal.android.models.TestCard
import com.paypal.android.uishared.enums.StoreInVaultOption
import com.paypal.android.uishared.state.ActionButtonState
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApproveOrderViewModel @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val sdkSampleServerAPI: SDKSampleServerAPI,
    private val completeOrderUseCase: CompleteOrderUseCase,
) : ViewModel() {

    companion object {
        const val TAG = "CardFragment"
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    private val _uiState = MutableStateFlow(ApproveOrderUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var cardClient: CardClient
    private lateinit var payPalDataCollector: PayPalDataCollector


    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionButtonState.Loading
            val uiState = uiState.value
            val orderRequest = uiState.run {
                OrderRequest(intentOption, shouldVault == StoreInVaultOption.ON_SUCCESS)
            }
            val order = createOrderUseCase(orderRequest)
            createOrderState = ActionButtonState.Success(order)
        }
    }

    fun approveOrder(activity: AppCompatActivity) {
        viewModelScope.launch {
            isApproveOrderLoading = true

            val clientId = sdkSampleServerAPI.fetchClientId()
            val coreConfig = CoreConfig(clientId = clientId)
            payPalDataCollector = PayPalDataCollector(coreConfig)

            cardClient = CardClient(activity, coreConfig)
            cardClient.approveOrderListener = object : ApproveOrderListener {
                override fun onApproveOrderSuccess(result: CardResult) {
                    approveOrderResult = result
                    isApproveOrderLoading = false
                }

                override fun onApproveOrderFailure(error: PayPalSDKError) {
                    approveOrderErrorMessage = "CAPTURE fail: ${error.errorDescription}"
                    isApproveOrderLoading = false
                }

                override fun onApproveOrderCanceled() {
                    approveOrderErrorMessage = "USER CANCELED"
                    isApproveOrderLoading = false
                }

                override fun onApproveOrderThreeDSecureWillLaunch() {
                    Log.d(TAG, "3DS Auth Requested")
                }

                override fun onApproveOrderThreeDSecureDidFinish() {
                    Log.d(TAG, "3DS Success")
                    isApproveOrderLoading = false
                }
            }

            val cardRequest = createCardRequest(uiState.value, createdOrder!!)
            cardClient.approveOrder(activity, cardRequest)
        }
    }

    fun completeOrder(context: Context) {
        viewModelScope.launch {
            isCompleteOrderLoading = true

            val cmid = payPalDataCollector.collectDeviceData(context)

            completedOrder = completeOrderUseCase(createdOrder!!.id!!, intentOption, cmid)
            isCompleteOrderLoading = false
        }
    }

    private fun createCardRequest(uiState: ApproveOrderUiState, order: Order): CardRequest {
        val card = parseCard(uiState)
        return CardRequest(order.id!!, card, APP_RETURN_URL, uiState.scaOption)
    }

    private fun parseCard(uiState: ApproveOrderUiState): Card {
        // expiration date in UI State needs to be formatted because it uses a visual transformation
        val dateString = DateString(uiState.cardExpirationDate)
        return Card(
            number = uiState.cardNumber,
            expirationMonth = dateString.formattedMonth,
            expirationYear = dateString.formattedYear,
            securityCode = uiState.cardSecurityCode
        )
    }

    var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionButtonState.Success)?.value

    var approveOrderResult: CardResult?
        get() = _uiState.value.approveOrderResult
        set(value) {
            _uiState.update { it.copy(approveOrderResult = value) }
        }

    var approveOrderErrorMessage: String?
        get() = _uiState.value.approveOrderErrorMessage
        set(value) {
            _uiState.update { it.copy(approveOrderErrorMessage = value) }
        }

    var completedOrder: Order?
        get() = _uiState.value.completedOrder
        set(value) {
            _uiState.update { it.copy(completedOrder = value) }
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

    var isApproveOrderLoading: Boolean
        get() = _uiState.value.isApproveOrderLoading
        set(value) {
            _uiState.update { it.copy(isApproveOrderLoading = value) }
        }

    var isCompleteOrderLoading: Boolean
        get() = _uiState.value.isCompleteOrderLoading
        set(value) {
            _uiState.update { it.copy(isCompleteOrderLoading = value) }
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
