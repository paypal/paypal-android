package com.paypal.android.card.network

import com.paypal.android.card.Card
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod

class CardAPIRequestBuilder {

    fun buildConfirmPaymentSourceRequest(orderID: String, card: Card): APIRequest {
        val path = "v2/checkout/orders/$orderID/confirm-payment-source"

        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val expirationDate = card.run {
            val parsedSecurityCode = expirationDate.split("/")
            val month = parsedSecurityCode[0]
            val year = parsedSecurityCode[1]
            "20$year-$month"
        }

        val body = """
            {
                "payment_source": {
                    "card": {
                        "number": "$cardNumber",
                        "expiry": "$expirationDate",
                        "security_code": "${card.securityCode}"
                    }
                }
            }
        """.trimIndent()
        return APIRequest(path, HttpMethod.POST, body)
    }
}