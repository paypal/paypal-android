package com.paypal.android.ui.paypal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.DemoConstants
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.model.PaymentMethodSelected
import com.paypal.android.api.model.UserActionSelected
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.model.TokenType
import com.paypal.android.customenvironment.CustomEnvironmentRepository
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.paypalpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalpayments.PayPalUserAction
import com.paypal.android.paypalpayments.PayPalUserIdentity
import com.paypal.android.paypalpayments.PayPalClient
import com.paypal.android.paypalpayments.PayPalFinishStartResult
import com.paypal.android.paypalpayments.PayPalCheckoutFundingSource
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
class PayPalCheckoutViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase,
    private val customEnvironmentRepository: CustomEnvironmentRepository,
) : ViewModel() {

    companion object {
        private val TAG = PayPalCheckoutViewModel::class.qualifiedName
    }

    private fun buildCoreConfig(): CoreConfig =
        customEnvironmentRepository.getCoreConfig(
            CoreConfig(SDKSampleServerAPI.clientId, SDKSampleServerAPI.merchantId)
        )

    private val payPalDataCollector = PayPalDataCollector(buildCoreConfig())
    private val paypalClient: PayPalClient = PayPalClient(applicationContext, buildCoreConfig())

    private val _uiState = MutableStateFlow(PayPalUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    var shouldVault: StoreInVaultOption
        get() = _uiState.value.shouldVaultOption
        set(value) {
            _uiState.update { it.copy(shouldVaultOption = value) }
        }

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    private var payPalCheckoutState
        get() = _uiState.value.payPalCheckoutState
        set(value) {
            _uiState.update { it.copy(payPalCheckoutState = value) }
        }

    private var completeOrderState
        get() = _uiState.value.completeOrderState
        set(value) {
            _uiState.update { it.copy(completeOrderState = value) }
        }

    var paymentMethodOption: PayPalCheckoutFundingSource
        get() = _uiState.value.paymentMethodOption
        set(value) {
            _uiState.update { it.copy(paymentMethodOption = value) }
        }

    var amount: String
        get() = _uiState.value.amount
        set(value) {
            _uiState.update { it.copy(amount = value) }
        }

    var userIdentity: PayPalUserIdentity?
        get() = _uiState.value.userIdentity
        set(value) {
            _uiState.update { it.copy(userIdentity = value) }
        }

    var userAction: PayPalUserAction
        get() = _uiState.value.userAction
        set(value) {
            _uiState.update { it.copy(userAction = value) }
        }

    private fun createPayPalSession() {
        paypalClient.createPayPalSession(
            tokenType = TokenType.ORDER_ID,
            userIdentity = _uiState.value.userIdentity,
            urlConfig = DemoConstants.returnToAppUrlConfig,
            userAction = _uiState.value.userAction
        )
    }

    fun createOrder() {
        createPayPalSession()
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = _uiState.value.run {
                val shouldVault = shouldVaultOption == StoreInVaultOption.ON_SUCCESS
                OrderRequest(
                    intentOption,
                    shouldVault,
                    amount,
                    paymentMethodOption.toPaymentMethodSelected(),
                    userAction.toUserActionSelected()
                )
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    private fun PayPalCheckoutFundingSource.toPaymentMethodSelected(): PaymentMethodSelected =
        when (this) {
            PayPalCheckoutFundingSource.PAYPAL_CREDIT -> PaymentMethodSelected.PAYPAL_CREDIT
            PayPalCheckoutFundingSource.PAY_LATER -> PaymentMethodSelected.PAYPAL_PAY_LATER
            PayPalCheckoutFundingSource.PAYPAL -> PaymentMethodSelected.PAYPAL
        }

    private fun PayPalUserAction.toUserActionSelected(): UserActionSelected = when (this) {
        PayPalUserAction.CONTINUE -> UserActionSelected.CONTINUE
        PayPalUserAction.PAY_NOW -> UserActionSelected.PAY_NOW
        PayPalUserAction.SETUP_NOW -> UserActionSelected.SETUP_NOW
    }

    fun startCheckout(activity: Activity) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            payPalCheckoutState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            startCheckoutWithOrderId(activity, orderId)
        }
    }

    private fun startCheckoutWithOrderId(activity: Activity, orderId: String) {
        payPalCheckoutState = ActionState.Loading

        paypalClient.start(activity, orderId) { startResult ->
            when (startResult) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    // do nothing; wait for web checkout to return to the app
                }

                is PayPalPresentAuthChallengeResult.Failure ->
                    payPalCheckoutState = ActionState.Failure(startResult.error)
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

    fun completeAuthChallenge(intent: Intent) =
        paypalClient.finishStart(intent)?.let { payPalAuthResult ->
            when (payPalAuthResult) {
                is PayPalFinishStartResult.Success -> {
                    payPalCheckoutState = ActionState.Success(payPalAuthResult)
                }

                is PayPalFinishStartResult.Canceled -> {
                    val error = Exception("USER CANCELED")
                    payPalCheckoutState = ActionState.Failure(error)
                }

                is PayPalFinishStartResult.Failure -> {
                    Log.i(TAG, "Checkout Error: ${payPalAuthResult.error.errorDescription}")
                    payPalCheckoutState = ActionState.Failure(payPalAuthResult.error)
                }

                PayPalFinishStartResult.NoResult -> {
                    // no result; re-enable PayPal button so user can retry
                    payPalCheckoutState = ActionState.Idle
                }
            }
        }
}
