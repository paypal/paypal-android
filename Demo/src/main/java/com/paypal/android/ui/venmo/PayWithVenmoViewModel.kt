package com.paypal.android.ui.venmo

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.customenvironment.CustomEnvironmentRepository
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateVenmoOrderUseCase
import com.paypal.android.venmo.VenmoClient
import com.paypal.android.venmo.VenmoFinishStartResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayWithVenmoViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createOrderUseCase: CreateVenmoOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase,
    private val customEnvironmentRepository: CustomEnvironmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayWithVenmoUiState())
    val uiState = _uiState.asStateFlow()

    private fun buildCoreConfig(): CoreConfig =
        customEnvironmentRepository.getCoreConfig(CoreConfig(SDKSampleServerAPI.clientId, merchantId = "49PMUL5PVD5SY"))

    private val coreConfig by lazy { buildCoreConfig() }
    private val payPalDataCollector by lazy { PayPalDataCollector(coreConfig) }
    private val venmoClient by lazy { VenmoClient(applicationContext, coreConfig) }

    private var checkEligibilityState
        get() = _uiState.value.checkEligibilityState
        set(value) {
            _uiState.update { it.copy(checkEligibilityState = value) }
        }

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private var payWithVenmoState
        get() = _uiState.value.payWithVenmoState
        set(value) {
            _uiState.update { it.copy(payWithVenmoState = value) }
        }

    private var completeOrderState
        get() = _uiState.value.completeOrderState
        set(value) {
            _uiState.update { it.copy(completeOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    fun checkEligibility() {
        viewModelScope.launch {
            checkEligibilityState = ActionState.Loading
            val result = venmoClient.isEligible(buyerCountry = "US")
            checkEligibilityState = ActionState.Success(result)
        }
    }

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = _uiState.value.run {
                OrderRequest(
                    intent = OrderIntent.CAPTURE,
                    shouldVaultOnSuccess = false,
                    appSwitchWhenEligible = true,
                    returnToAppStrategy = ReturnToAppStrategyOption.APP_LINKS
                )
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun startVenmo(activity: ComponentActivity) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            payWithVenmoState = ActionState.Failure(Exception("Create an order to continue."))
            return
        }
        viewModelScope.launch {
            payWithVenmoState = ActionState.Loading
            try {
                venmoClient.start(activity, orderId)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                payWithVenmoState = ActionState.Failure(e)
            }
        }
    }

    fun finishVenmo(intent: Intent) {
        val result = venmoClient.finishStart(intent)
        // Only update state if this is an actual Venmo result
        // Ignore NoResult as it means the intent is unrelated to Venmo flow
        when (result) {
            is VenmoFinishStartResult.Success -> {
                payWithVenmoState = ActionState.Success(result)
            }
            is VenmoFinishStartResult.Failure -> {
                payWithVenmoState = ActionState.Failure(result.error)
            }
            is VenmoFinishStartResult.Canceled,
            is VenmoFinishStartResult.NoResult -> {
                // Do nothing - canceled or unrelated intent
            }
            null -> {
                // Do nothing - no active Venmo flow
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
                // TODO: allow order intent to be configurable
                completeOrderState =
                    completeOrderUseCase(orderId, OrderIntent.CAPTURE, cmid).mapToActionState()
            }
        }
    }
}
