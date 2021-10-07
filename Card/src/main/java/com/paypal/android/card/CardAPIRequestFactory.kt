package com.paypal.android.card

import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import org.json.JSONObject

internal class CardAPIRequestFactory {

    fun createConfirmPaymentSourceRequest(orderID: String, card: Card): APIRequest {
        val path = "v2/checkout/orders/$orderID/confirm-payment-source"

        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

        val cardJSON = JSONObject()
            .put("number", cardNumber)
            .put("expiry", cardExpiry)

        card.securityCode?.let { cardJSON.put("security_code", it) }

        val paymentSourceJSON = JSONObject().put("card", cardJSON)
        val bodyJSON = JSONObject().put("payment_source", paymentSourceJSON)
        val body = bodyJSON.toString()

        return APIRequest(path, HttpMethod.POST, body)
    }
}
