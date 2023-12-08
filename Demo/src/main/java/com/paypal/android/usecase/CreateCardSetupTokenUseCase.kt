package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.requests.CardPaymentSource
import com.paypal.android.api.requests.Customer
import com.paypal.android.api.requests.PaymentSource
import com.paypal.android.api.requests.SetupTokenRequest
import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import javax.inject.Inject

class CreateCardSetupTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(customerId: String?): CardSetupToken {
        // create a payment token with an empty card attribute; the merchant app will
        // provide the card's details through the SDK
        val request = SetupTokenRequest().apply {
            customer = if (!customerId.isNullOrEmpty()) Customer(customerId) else null
            paymentSource[PaymentSource.Card] = CardPaymentSource
        }

        // Ref: https://stackoverflow.com/a/19610814
        val body = Json.encodeToString(request).replace("\\/", "/")

        val jsonOrder = JsonParser.parseString(body) as JsonObject
        val response = sdkSampleServerAPI.createSetupToken(jsonOrder)
        val responseJSON = JSONObject(response.string())

        val customerJSON = responseJSON.getJSONObject("customer")
        return CardSetupToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            status = responseJSON.getString("status"),
        )
    }
}
