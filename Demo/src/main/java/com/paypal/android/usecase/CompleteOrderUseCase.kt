package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class CompleteOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    companion object {
        private fun optNonEmptyString(json: JSONObject?, key: String): String? = json?.let {
            it.optString(key).ifEmpty {
                null
            }
        }
    }

    suspend operator fun invoke(
        orderId: String,
        intent: OrderIntent,
        clientMetadataId: String
    ): UseCaseResult<Order, Exception> = withContext(Dispatchers.IO) {
        val response = when (intent) {
            OrderIntent.CAPTURE ->
                sdkSampleServerAPI.captureOrder(orderId, clientMetadataId)

            OrderIntent.AUTHORIZE ->
                sdkSampleServerAPI.authorizeOrder(orderId, clientMetadataId)
        }
        parseOrder(response)
    }

    private fun parseOrder(json: JSONObject): UseCaseResult<Order, Exception> {
        val cardJSON = json.optJSONObject("payment_source")?.optJSONObject("card")
        val vaultJSON = cardJSON?.optJSONObject("attributes")?.optJSONObject("vault")
        val vaultCustomerJSON = vaultJSON?.optJSONObject("customer")

        return UseCaseResult.Success(
            Order(
                id = optNonEmptyString(json, "id"),
                intent = optNonEmptyString(json, "intent"),
                status = optNonEmptyString(json, "status"),
                cardLast4 = optNonEmptyString(cardJSON, "last_digits"),
                cardBrand = optNonEmptyString(cardJSON, "brand"),
                vaultId = optNonEmptyString(vaultJSON, "id"),
                customerId = optNonEmptyString(vaultCustomerJSON, "id")
            )
        )
    }
}
