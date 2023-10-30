package com.paypal.android.usecase

import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.models.OrderRequest
import com.paypal.android.models.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(request: OrderRequest): Order = withContext(Dispatchers.IO) {
        val amountJSON = JSONObject()
            .put("currency_code", "USD")
            .put("value", "10.99")

        val purchaseUnitJSON = JSONObject()
            .put("amount", amountJSON)

//    "application_context": {
//        "return_url": "https://example.com/returnUrl",
//        "cancel_url": "https://example.com/cancelUrl"
//    }

        val appReturnUrl = "com.paypal.android.demo://example.com/returnUrl"
        val applicationContextJSON = JSONObject()
        applicationContextJSON.put("return_url", appReturnUrl)
        applicationContextJSON.put("cancel_url", appReturnUrl)

        val orderRequest = JSONObject()
            .put("intent", request.orderIntent)
            .put("purchase_units", JSONArray().put(purchaseUnitJSON))
            .put("application_context", applicationContextJSON)

        orderRequest.putOpt("payment_source", createPaymentSourceJSON(request))
        sdkSampleServerAPI.createOrder(orderRequest)
    }

    private fun createPaymentSourceJSON(request: OrderRequest): JSONObject? {
        var result: JSONObject? = null
        if (request.shouldVault) {
            val vaultJSON = JSONObject()
                .put("usage_type", "MERCHANT")
                .put("customer_type", "CONSUMER")
                .put("store_in_vault", "ON_SUCCESS")

            val paymentMethodAttributes = JSONObject()
                .put("vault", vaultJSON)

            val vaultCustomerId = request.vaultCustomerId
            if (vaultCustomerId.isNotEmpty()) {
                val customerJSON = JSONObject()
                    .put("id", vaultCustomerId)
                paymentMethodAttributes.put("customer", customerJSON)
            }

            val paymentMethodName = when (request.paymentMethod) {
                PaymentMethod.CARD -> "card"
                PaymentMethod.PAYPAL -> "paypal"
            }

            result = JSONObject()
            result.put(paymentMethodName, JSONObject()
                .put("attributes", paymentMethodAttributes)
            )
        }
        return result
    }
}
