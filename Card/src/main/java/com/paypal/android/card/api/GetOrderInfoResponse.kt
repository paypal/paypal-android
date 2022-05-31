package com.paypal.android.card.api

import com.paypal.android.card.OrderIntent
import com.paypal.android.card.model.PaymentSource
import com.paypal.android.card.model.PurchaseUnit
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PaymentsJSON

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
        // TODO: make PaymentsJSON return payments JSON objects to fully wrap JSONObject API
        json.optMapObject("payment_source.card") { PaymentSource(PaymentsJSON(it)) },
        json.optMapObjectArray("purchase_units") { PurchaseUnit(it) }
    )
}