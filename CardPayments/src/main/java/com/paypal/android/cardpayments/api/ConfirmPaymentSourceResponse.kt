package com.paypal.android.cardpayments.api

import com.paypal.android.corepayments.models.PaymentSource
import com.paypal.android.corepayments.models.PurchaseUnit
import com.paypal.android.corepayments.OrderStatus

internal data class ConfirmPaymentSourceResponse(
    val orderID: String,
    val status: OrderStatus? = null,
    val payerActionHref: String? = null,
    val paymentSource: PaymentSource? = null,
    val purchaseUnits: List<PurchaseUnit>? = null
)
