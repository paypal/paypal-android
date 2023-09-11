package com.paypal.android.ui.paypalweb

import com.paypal.android.api.model.Order
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult

data class PayPalWebUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isCreateOrderLoading: Boolean = false,
    val isStartCheckoutLoading: Boolean = false,
    val createdOrder: Order? = null,
    val completedOrder: Order? = null,
    val payPalWebCheckoutResult: PayPalWebCheckoutResult? = null,
    val payPalWebCheckoutError: PayPalSDKError? = null,
    val isCheckoutCanceled: Boolean = false,
    val isCompleteOrderLoading: Boolean = false,
    val fundingSource: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL
) {
}
