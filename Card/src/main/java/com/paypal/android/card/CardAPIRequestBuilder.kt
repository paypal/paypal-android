package com.paypal.android.card

import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod

internal class CardAPIRequestBuilder(
    private val dateParser: DateParser = DateParser()
) {

    fun buildConfirmPaymentSourceRequest(orderID: String, card: Card): APIRequest {
        val path = "v2/checkout/orders/$orderID/confirm-payment-source"

        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = dateParser.parseCardExpiry(card.expirationDate)
        val expirationDate = "${cardExpiry.year}-${cardExpiry.month}"

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
