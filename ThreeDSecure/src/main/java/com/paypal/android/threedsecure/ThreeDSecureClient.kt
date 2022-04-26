package com.paypal.android.threedsecure

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchOptions
import com.paypal.android.card.Card
import com.paypal.android.core.API
import com.paypal.android.core.APIRequest
import com.paypal.android.core.CoreConfig
import com.paypal.android.core.HttpMethod
import org.json.JSONObject

class ThreeDSecureClient internal constructor(private val api: API) {

    private val browserSwitchClient = BrowserSwitchClient()

    constructor(configuration: CoreConfig) : this(API(configuration))

    suspend fun verify(activity: FragmentActivity, orderID: String, card: Card) {
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

        val verificationJSON = JSONObject()
            .put("method", "SCA_ALWAYS")
        val attributesJSON = JSONObject()
            .put("verification", verificationJSON)
        cardJSON.put("attributes", attributesJSON)

        val paymentSourceJSON = JSONObject()
            .put("card", cardJSON)

        val bodyJSON = JSONObject()
            .put("payment_source", paymentSourceJSON)
        val body = bodyJSON.toString()

        val path = "v2/checkout/orders/$orderID/confirm-payment-source"
        val apiRequest = APIRequest(path, HttpMethod.POST, body)

        val httpResponse = api.send(apiRequest)

        val responseJSON = JSONObject(httpResponse.body)

        val linksArray = responseJSON.getJSONArray("links")
        val links = (0 until linksArray.length()).map { linksArray.getJSONObject(it) }

        val payerActionLink = links.first { it.getString("rel") == "payer-action" }
        val href = payerActionLink.getString("href")

        val options = BrowserSwitchOptions()
            .url(Uri.parse(href))
            .returnUrlScheme("com.paypal.android.demo")
        browserSwitchClient.start(activity, options)

        // FUTURE: inspect URL for 3DS verification success / failure
    }
}
