package com.paypal.android.card

import com.paypal.android.card.api.ConfirmPaymentSourceResponse
import com.paypal.android.card.model.PaymentSource
import com.paypal.android.card.model.PurchaseUnit
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
import com.paypal.android.core.APIClientError
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpMethod
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
import com.paypal.android.core.containsKey
import org.json.JSONException
import org.json.JSONObject

internal object ConfirmPaymentSourceRequestFactory {

    fun createRequest(
        orderID: String,
        card: Card,
        threeDSecureRequest: ThreeDSecureRequest? = null
    ): APIRequest {
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
        return APIRequest(path, HttpMethod.POST, body)
    }

    @Throws(PayPalSDKError::class)
    fun parseResponse(response: String, correlationId: String?): ConfirmPaymentSourceResponse =
        try {
            val json = PaymentsJSON(response)
            val status = json.getString("status")
            val id = json.getString("id")

            // this section is for 3DS
            val linksArray = json.getJSONArray("links")
            val links = (0 until linksArray.length()).map { linksArray.getJSONObject(it) }
            val payerActionLink = links.firstOrNull { it.getString("rel") == "payer-action" }
            val payerActionHref = payerActionLink?.getString("href")
            ConfirmPaymentSourceResponse(
                id,
                OrderStatus.valueOf(status),
                payerActionHref,
                PaymentSource(json.getJSONObject("payment_source.card")),
                if (json.json.containsKey("purchase_units")) PurchaseUnit.fromJSONArray(
                    json.json.getJSONArray(
                        "purchase_units"
                    )
                ) else null
            )
        } catch (e: JSONException) {
            throw APIClientError.dataParsingError(correlationId, e)
        }
}
