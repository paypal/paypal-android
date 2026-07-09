package com.paypal.android.ui.venmo

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.DemoConstants
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.ReturnToAppStrategy
import com.paypal.android.corepayments.returnUrl
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayWithVenmoUiState())
    val uiState = _uiState.asStateFlow()

    private val coreConfig = CoreConfig(BuildConfig.CLIENT_ID, Environment.CUSTOM)
    private val payPalDataCollector = PayPalDataCollector(coreConfig)
    private val venmoClient = VenmoClient(applicationContext, coreConfig)

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
            val returnToAppStrategy = ReturnToAppStrategy.AppLink(DemoConstants.APP_URL)
            val returnUrl = returnToAppStrategy.returnUrl.toUri()
                .buildUpon()
                .fragment("return")
                .toString()
            try {
                venmoClient.start(activity, orderId, returnUrl)
            } catch (e: Exception) {
                payWithVenmoState = ActionState.Failure(e)
            }
        }
    }

    fun finishVenmo(intent: Intent) {
        venmoClient.finishStart(intent)?.let { result ->
            payWithVenmoState = when (result) {
                is VenmoFinishStartResult.Success -> ActionState.Success(result)
                is VenmoFinishStartResult.Failure -> ActionState.Failure(result.error)
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