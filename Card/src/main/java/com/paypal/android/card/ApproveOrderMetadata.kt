package com.paypal.android.card

import com.paypal.android.card.model.PaymentSource
import org.json.JSONObject

data class ApproveOrderMetadata(val orderId: String, val paymentSource: PaymentSource?) {

    companion object {

        fun fromJSON(json: JSONObject?): ApproveOrderMetadata? {
            // TODO: implement
            return ApproveOrderMetadata("sample", PaymentSource("1234", "VISA"))
        }
    }

    fun toJSON(): JSONObject {
        // TODO: implement
        return JSONObject()
    }
}