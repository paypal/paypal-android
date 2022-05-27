package com.paypal.android.card.api

import com.paypal.android.card.OrderIntent
import com.paypal.android.card.model.PaymentSource
import com.paypal.android.card.model.PurchaseUnit
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PaymentsJSON
import com.paypal.android.core.containsKey
import org.json.JSONObject

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
        json.optGetJSONObject("payment_source.card")?.let { PaymentSource(it) },
        if (json.json.containsKey("purchase_units")) PurchaseUnit.fromJSONArray(json.getJSONArray("purchase_units")) else null
    )
}