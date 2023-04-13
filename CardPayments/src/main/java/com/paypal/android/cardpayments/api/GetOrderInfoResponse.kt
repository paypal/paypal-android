package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.cardpayments.model.PaymentSource
import com.paypal.android.cardpayments.model.PurchaseUnit
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PaymentsJSON

internal data class GetOrderInfoResponse(
    val orderId: String,
    val orderStatus: OrderStatus,
    val orderIntent: OrderIntent,
    val paymentSource: PaymentSource? = null,
    val purchaseUnits: List<PurchaseUnit>? = null
) {
    constructor(json: PaymentsJSON) : this(
        json.getString("id"),
        OrderStatus.valueOf(json.getString("status")),
        OrderIntent.valueOf(json.getString("intent")),
        json.optMapObject("payment_source.card") {
            PaymentSource(
                it
            )
        },
        json.optMapObjectArray("purchase_units") {
            PurchaseUnit(
                it
            )
        }
    )
}
