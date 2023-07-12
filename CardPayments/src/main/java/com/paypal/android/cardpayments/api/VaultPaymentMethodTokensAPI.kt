package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.VaultRequest
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.HttpMethod
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

        val paymentSourceJSON = JSONObject()
        paymentSourceJSON.put("card", cardJSON)

        val requestJSON = JSONObject()
        requestJSON.put("payment_source", paymentSourceJSON)

        vaultRequest.customerId?.let { customerId ->
            val customerJSON = JSONObject()
                .put("id", customerId)
            requestJSON.put("customer", customerJSON)
        }

        val apiRequest =
            APIRequest("/v3/vault/setup-tokens/", HttpMethod.POST, requestJSON.toString())
        val httpResponse = restClient.send(apiRequest)
        return VaultResult(setupTokenId = "fake-setup-token-id")
    }
}