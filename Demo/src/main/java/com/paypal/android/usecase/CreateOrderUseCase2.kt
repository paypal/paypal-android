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

class CreateOrderUseCase2 @Inject constructor(
    private val sdkSampleServerAPI: SDKSampleServerAPI
) {

    suspend operator fun invoke(request: OrderRequest): Pair<String, String> = withContext(Dispatchers.IO) {
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
        val response = sdkSampleServerAPI.createOrderJSON(orderRequest)
        val responseJSON = JSONObject(response.string())
        val links = responseJSON.getJSONArray("links")

        var payerActionHref: String? = null
        for (i in 0 until links.length()) {
            val link = links.getJSONObject(i)
            if (link.getString("rel") == "payer-action") {
                payerActionHref = link.getString("href")
                break
            }
        }
        Pair(payerActionHref!!, responseJSON.getString("id"))

//        {
//            "id": "5UM28916LG5328135",
//            "status": "PAYER_ACTION_REQUIRED",
//            "payment_source": {
//            "paypal": {}
//        },
//            "links": [
//            {
//                "href": "https://api.sandbox.paypal.com/v2/checkout/orders/5UM28916LG5328135",
//                "rel": "self",
//                "method": "GET"
//            },
//            {
//                "href": "https://www.sandbox.paypal.com/checkoutnow?token=5UM28916LG5328135",
//                "rel": "payer-action",
//                "method": "GET"
//            }
//            ]
//        }
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
