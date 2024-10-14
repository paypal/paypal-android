package com.paypal.android.plainclothes

import androidx.lifecycle.ViewModel
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.ui.approveorder.ApproveOrderUiState
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val getClientIdUseCase: GetClientIdUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
) : ViewModel() {

    companion object {
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    private val _uiState = MutableStateFlow(ApproveOrderUiState())
    val uiState = _uiState.asStateFlow()

    private var cardClient: CardClient? = null
    private var payPalClient: PayPalWebCheckoutClient? = null

    private var payPalDataCollector: PayPalDataCollector? = null

    fun checkoutWithPayPal() {
        TODO("Not yet implemented")
    }

    fun checkoutWithCard() {
        TODO("Not yet implemented")
    }
}