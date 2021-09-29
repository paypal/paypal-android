package com.paypal.android.card

import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod

internal class CardAPIRequestFactory(
    private val dateParser: DateParser = DateParser()
) {

    fun createConfirmPaymentSourceRequest(orderID: String, card: Card): APIRequest {
        val path = "v2/checkout/orders/$orderID/confirm-payment-source"

        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val expirationDate = dateParser.parseExpirationDate(card.expirationDate)

        val monthString = "%02d".format(expirationDate.month)
        val cardExpiry = "${expirationDate.year}-$monthString"

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
