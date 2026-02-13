package com.paypal.android.ui.venmo

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.models.OrderRequest
import com.paypal.android.uishared.enums.DeepLinkStrategy
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CreateOrderUseCase
import com.paypal.android.venmo.VenmoClient
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
    val createOrderUseCase: CreateOrderUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PayWithVenmoUiState())
    val uiState = _uiState.asStateFlow()

    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId)
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

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = _uiState.value.run {
                OrderRequest(
                    intent = OrderIntent.CAPTURE,
                    shouldVaultOnSuccess = false,
                    appSwitchWhenEligible = false,
                    deepLinkStrategy = DeepLinkStrategy.CUSTOM_URL_SCHEME
                )
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun startVenmo(activity: ComponentActivity) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            payWithVenmoState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            venmoClient.startVenmo(activity, orderId)
        }
    }
}