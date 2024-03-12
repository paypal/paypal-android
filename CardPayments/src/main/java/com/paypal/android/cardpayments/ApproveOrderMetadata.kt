package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.model.PaymentSource
import com.paypal.android.corepayments.PaymentsJSON
import org.json.JSONObject

internal data class ApproveOrderMetadata(
    val orderId: String,
    val paymentSource: PaymentSource? = null
) {

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
            .put(KEY_ORDER_ID, orderId)
            .putOpt(KEY_PAYMENT_SOURCE, paymentSource?.toJSON())
}
