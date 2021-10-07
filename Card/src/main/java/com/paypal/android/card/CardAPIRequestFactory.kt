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

        card.billingAddress?.let { billingAddress ->
            val billingAddressJSON = JSONObject()
                .put("address_line_1", billingAddress.addressLine1)
                .put("address_line_2", billingAddress.addressLine2)
                .put("admin_area_1", billingAddress.state)
                .put("admin_area_2", billingAddress.city)
                .put("postal_code", billingAddress.postalCode)
                .put("country_code", billingAddress.countryCode)
            cardJSON.put("billing_address", billingAddressJSON)
        }

        val paymentSourceJSON = JSONObject().put("card", cardJSON)
        val bodyJSON = JSONObject().put("payment_source", paymentSourceJSON)
        val body = bodyJSON.toString()

        return APIRequest(path, HttpMethod.POST, body)
    }
}
