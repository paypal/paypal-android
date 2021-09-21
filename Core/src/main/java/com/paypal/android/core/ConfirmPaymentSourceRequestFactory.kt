package com.paypal.android.core

import java.net.URL

class ConfirmPaymentSourceRequestFactory(private val configuration: PaymentsConfiguration) {

    fun create(orderId: String, paymentSource: String): HttpRequest {
        val confirmPaymentSourceUrl = "${configuration.environment.url}/v2/checkout/orders/${orderId}/confirm-payment-source"
        val body = """
            {
                "payment_source": $paymentSource
            }
        """.trimIndent()

        //add client id and all that stuff to the request.
        return HttpRequest(URL(confirmPaymentSourceUrl), "POST", body)
    }
}