package com.paypal.android.card

import com.paypal.android.card.model.PaymentSource
import com.paypal.android.core.PaymentsJSON
import org.json.JSONObject

data class ApproveOrderMetadata(val orderID: String, val paymentSource: PaymentSource?) {

    companion object {

        const val KEY_ORDER_ID = "order_id"
        const val KEY_PAYMENT_SOURCE = "payment_source"

        fun fromJSON(data: JSONObject?): ApproveOrderMetadata? =
            data?.let { fromJSON(PaymentsJSON(it)) }

        private fun fromJSON(json: PaymentsJSON): ApproveOrderMetadata {
            val orderId = json.getString(KEY_ORDER_ID)
            val paymentSource = json.optMapObject(KEY_PAYMENT_SOURCE) { PaymentSource(it) }
            return ApproveOrderMetadata(orderId, paymentSource)
        }
    }

    fun toJSON(): JSONObject =
        JSONObject()
            .put(KEY_ORDER_ID, orderID)
            .putOpt(KEY_PAYMENT_SOURCE, paymentSource?.toJSON())
}
