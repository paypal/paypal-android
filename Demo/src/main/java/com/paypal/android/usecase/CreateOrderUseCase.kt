package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.models.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(request: OrderRequest): SDKSampleServerResult<Order, Exception> =
        withContext(Dispatchers.IO) {
            val amountJSON = JSONObject()
                .put("currency_code", "USD")
                .put("value", "10.99")

            val purchaseUnitJSON = JSONObject()
                .put("amount", amountJSON)

            val orderRequest = JSONObject()
                .put("intent", request.intent)
                .put("purchase_units", JSONArray().put(purchaseUnitJSON))

            if (request.shouldVault) {
                val vaultJSON = JSONObject()
                    .put("store_in_vault", "ON_SUCCESS")

                val cardAttributesJSON = JSONObject()
                    .put("vault", vaultJSON)

                val cardJSON = JSONObject()
                    .put("attributes", cardAttributesJSON)

                val paymentSourceJSON = JSONObject()
                    .put("card", cardJSON)

                orderRequest.put("payment_source", paymentSourceJSON)
            }
            sdkSampleServerAPI.createOrder(orderRequest)
        }
}
