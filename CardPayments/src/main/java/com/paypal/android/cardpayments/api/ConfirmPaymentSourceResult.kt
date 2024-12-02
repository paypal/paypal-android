package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.model.PaymentSource
import com.paypal.android.cardpayments.model.PurchaseUnit
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError

internal sealed class ConfirmPaymentSourceResult {

    data class Success(
        val orderId: String,
        val status: OrderStatus? = null,
        val payerActionHref: String? = null,
        val paymentSource: PaymentSource? = null,
        val purchaseUnits: List<PurchaseUnit>? = null
    ) : ConfirmPaymentSourceResult()

    data class Failure(val error: PayPalSDKError) : ConfirmPaymentSourceResult()
}
