package com.paypal.android.corepayments.api.models

import com.paypal.android.corepayments.models.PaymentSource
import com.paypal.android.corepayments.models.PurchaseUnit
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PaymentsJSON
import com.paypal.android.corepayments.models.OrderIntent

data class GetOrderInfoResponse(
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
        json.optMapObject("payment_source.card") { PaymentSource(it) },
        json.optMapObjectArray("purchase_units") { PurchaseUnit(it) }
    )
}
