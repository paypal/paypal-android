package com.paypal.android.card

import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod

internal class CardAPIRequestFactory {

    fun createConfirmPaymentSourceRequest(orderID: String, card: Card): APIRequest {
        val path = "v2/checkout/orders/$orderID/confirm-payment-source"

        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

        val body = """
            {
                "payment_source": {
                    "card": {
                        "number": "$cardNumber",
                        "expiry": "$cardExpiry",
                        "security_code": "${card.securityCode}"
                    }
                }
            }
        """.trimIndent()
        return APIRequest(path, HttpMethod.POST, body)
    }
}
