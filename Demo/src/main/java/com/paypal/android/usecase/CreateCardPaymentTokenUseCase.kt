package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.CardPaymentToken
import com.paypal.android.api.model.CardSetupToken
import com.paypal.android.api.requests.PaymentSource
import com.paypal.android.api.requests.PaymentTokenRequest
import com.paypal.android.api.requests.TokenPaymentSource
import com.paypal.android.api.requests.TokenPaymentSourceType
import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import javax.inject.Inject

class CreateCardPaymentTokenUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(setupToken: CardSetupToken): CardPaymentToken {
        val request = PaymentTokenRequest().apply {
            paymentSource[PaymentSource.Token] = TokenPaymentSource().apply {
                id = setupToken.id
                type = TokenPaymentSourceType.SetupToken
            }
        }

        val body = Json.encodeToString(request)
        val requestJson = JsonParser.parseString(body) as JsonObject
        val response = sdkSampleServerAPI.createPaymentToken(requestJson)
        val responseJSON = JSONObject(response.string())

        val customerJSON = responseJSON.getJSONObject("customer")
        val cardJSON = responseJSON
            .getJSONObject("payment_source")
            .getJSONObject("card")

        return CardPaymentToken(
            id = responseJSON.getString("id"),
            customerId = customerJSON.getString("id"),
            cardLast4 = cardJSON.getString("last_digits"),
            cardBrand = cardJSON.getString("brand")
        )
    }
}
