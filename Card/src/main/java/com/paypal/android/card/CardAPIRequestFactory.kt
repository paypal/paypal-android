package com.paypal.android.card

import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import org.json.JSONObject

internal class CardAPIRequestFactory {

    fun createConfirmPaymentSourceRequest(orderID: String, card: Card): APIRequest {
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

        val paymentSourceJSON = JSONObject().put("card", cardJSON)
        val bodyJSON = JSONObject().put("payment_source", paymentSourceJSON)
        val body = bodyJSON.toString()

        val path = "v2/checkout/orders/$orderID/confirm-payment-source"
        return APIRequest(path, HttpMethod.POST, body)
    }

    fun createAuthorizeWith3DSVerificationRequest(orderID: String, card: Card): APIRequest {
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

        val attributesJSON = JSONObject()
            .put("verification", JSONObject().put("method", "SCA_ALWAYS"))
        cardJSON.put("attributes", attributesJSON)

//        cardJSON.put("verification_method", "SCA_WHEN_REQUIRED")

        val sourceJSON = JSONObject().put("card", cardJSON)
//        val applicationContextJSON = JSONObject()
//            .put("brand_name", "YourBrandName")
//            .put("locale", "en-US")
//            .put("return_url", "https://example.com/returnUrl")
//            .put("cancel_url", "https://example.com/cancelUrl")

        val bodyJSON = JSONObject()
            .put("payment_source", sourceJSON)
//            .put("application_context", applicationContextJSON)
        val body = bodyJSON.toString()

        val path = "v2/checkout/orders/$orderID/authorize"
        return APIRequest(path, HttpMethod.POST, body)
    }
}
