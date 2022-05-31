package com.paypal.android.card.api

import com.paypal.android.card.model.PaymentSource
import com.paypal.android.card.model.PurchaseUnit
import com.paypal.android.core.OrderStatus

internal data class ConfirmPaymentSourceResponse(
    val orderID: String,
    val status: OrderStatus? = null,
    val payerActionHref: String? = null,
    val paymentSource: PaymentSource? = null,
    val purchaseUnits: List<PurchaseUnit>? = null
)
