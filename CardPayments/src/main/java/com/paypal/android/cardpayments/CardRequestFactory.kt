package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.api.GetOrderRequest
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.HttpMethod
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
            cardJSON.put("security_code", card.securityCode)

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

            val bodyJSON = JSONObject()
            val verificationJSON = JSONObject()
                .put("method", sca.name)
            val attributesJSON = JSONObject()
                .put("verification", verificationJSON)
            cardJSON.put("attributes", attributesJSON)

            val returnURLJSON = JSONObject()
                .put("return_url", returnUrl)
                .put("cancel_url", returnUrl) // we can set the same url
            bodyJSON.put("application_context", returnURLJSON)

            val paymentSourceJSON = JSONObject().put("card", cardJSON)
            bodyJSON.put("payment_source", paymentSourceJSON)

            val body = bodyJSON.toString().replace("\\/", "/")

            val path = "v2/checkout/orders/$orderID/confirm-payment-source"
            APIRequest(path, HttpMethod.POST, body)
        }

    fun createGetOrderInfoRequest(getOrderRequest: GetOrderRequest): APIRequest {
        val path = "v2/checkout/orders/${getOrderRequest.orderId}"
        return APIRequest(path, HttpMethod.GET, "")
    }
}
