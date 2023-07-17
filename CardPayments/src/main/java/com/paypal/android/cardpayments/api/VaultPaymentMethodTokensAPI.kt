package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.VaultRequest
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.PaymentsJSON
import com.paypal.android.corepayments.RestClient
import org.json.JSONObject

internal class VaultPaymentMethodTokensAPI(
    private val restClient: RestClient
) {
    constructor(coreConfig: CoreConfig) : this(RestClient(coreConfig))

    suspend fun createSetupToken(vaultRequest: VaultRequest): VaultResult {
        val card = vaultRequest.card
        val cardNumber = card.number.replace("\\s".toRegex(), "")
        val cardExpiry = "${card.expirationYear}-${card.expirationMonth}"

        val cardJSON = JSONObject()
            .put("number", cardNumber)
            .put("expiry", cardExpiry)
            .put("security_code", card.securityCode)
        card.cardholderName?.let { cardJSON.put("name", it) }

        cardJSON.put("verification_method", "SCA_WHEN_REQUIRED")
        val experienceContextJSON = JSONObject()

        val returnUrl = vaultRequest.returnUrl
        experienceContextJSON.put("return_url", returnUrl)
        experienceContextJSON.put("cancel_url", returnUrl)
        cardJSON.put("experience_context", experienceContextJSON)

        val paymentSourceJSON = JSONObject()
        paymentSourceJSON.put("card", cardJSON)

        val requestJSON = JSONObject()
        requestJSON.put("payment_source", paymentSourceJSON)

        vaultRequest.customerId?.let { customerId ->
            val customerJSON = JSONObject()
                .put("id", customerId)
            requestJSON.put("customer", customerJSON)
        }

        // Ref: https://stackoverflow.com/a/19610814
        val body = requestJSON.toString().replace("\\/", "/")

        val apiRequest =
            APIRequest("v3/vault/setup-tokens/", HttpMethod.POST, body)
        val httpResponse = restClient.send(apiRequest)

        val bodyResponse = httpResponse.body!!
        val responseJSON = PaymentsJSON(bodyResponse)

        val setupTokenId = responseJSON.getString("id")
        val status = responseJSON.getString("status")
        val customerId = responseJSON.getString("customer.id")
        return VaultResult(status, setupTokenId, customerId)
    }
}
