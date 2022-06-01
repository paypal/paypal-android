package com.paypal.android.card

import com.paypal.android.card.api.GetOrderRequest
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import org.json.JSONObject

internal class CardRequestFactory {

    fun createConfirmPaymentSourceRequest(cardRequest: CardRequest): APIRequest =
        cardRequest.run {
            val cardNumber = card.number.replace("\\s".toRegex(), "")
            val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

            val cardJSON = JSONObject()
                .put("number", cardNumber)
                .put("expiry", cardExpiry)

            card.cardholderName?.let { cardJSON.put("name", it) }
            card.securityCode?.let { cardJSON.put("security_code", it) }

            card.billingAddress?.apply {
                val billingAddressJSON = JSONObject()
                    .put("address_line_1", streetAddress)
                    .put("address_line_2", extendedAddress)
                    .put("admin_area_1", region)
                    .put("admin_area_2", locality)
                    .put("postal_code", postalCode)
                    .put("country_code", countryCode)
                cardJSON.put("billing_address", billingAddressJSON)
            }

            threeDSecureRequest?.also {
                val verificationJSON = JSONObject()
                    .put("method", it.sca.name)
                val attributesJSON = JSONObject()
                    .put("verification", verificationJSON)
                cardJSON.put("attributes", attributesJSON)
                // add return and cancel url when its supported
            }

            val paymentSourceJSON = JSONObject().put("card", cardJSON)
            val bodyJSON = JSONObject().put("payment_source", paymentSourceJSON)
            val body = bodyJSON.toString()

            val path = "v2/checkout/orders/$orderID/confirm-payment-source"
            APIRequest(path, HttpMethod.POST, body)
        }

    fun createGetOrderInfoRequest(getOrderRequest: GetOrderRequest): APIRequest {
        val path = "v2/checkout/orders/${getOrderRequest.orderId}"
        return APIRequest(path, HttpMethod.GET, "")
    }
}
