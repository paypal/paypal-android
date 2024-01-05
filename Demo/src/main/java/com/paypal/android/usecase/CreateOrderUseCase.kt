package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.models.OrderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(orderRequest: OrderRequest): Order = withContext(Dispatchers.IO) {
        sdkSampleServerAPI.createOrder(createOrderRequestJSON(orderRequest))
    }

    private fun createOrderRequestJSON(orderRequest: OrderRequest): JSONObject {
        val amountJSON = JSONObject()
            .put("currency_code", "USD")
            .put("value", "10.99")

        val purchaseUnitJSON = JSONObject()
            .put("amount", amountJSON)

        val requestJSON = JSONObject()
            .put("intent", orderRequest.orderIntent)
            .put("purchase_units", JSONArray().put(purchaseUnitJSON))

        if (orderRequest.shouldVault) {
            val vaultJSON = JSONObject()
                .put("store_in_vault", "ON_SUCCESS")

            val cardAttributesJSON = JSONObject()
                .put("vault", vaultJSON)

            val vaultCustomerId = orderRequest.vaultCustomerId
            if (vaultCustomerId.isNotEmpty()) {
                val customerJSON = JSONObject()
                    .put("id", vaultCustomerId)
                cardAttributesJSON.put("customer", customerJSON)
            }

            val cardJSON = JSONObject()
                .put("attributes", cardAttributesJSON)

            val paymentSourceJSON = JSONObject()
                .put("card", cardJSON)

            requestJSON.put("payment_source", paymentSourceJSON)
        }
        return requestJSON
    }
}
