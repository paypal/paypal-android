package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

class GetBillingAgreementTokenUseCase
    @Inject constructor(private val sdkSampleServerAPI: SDKSampleServerAPI) {

    suspend operator fun invoke(): Order = withContext(Dispatchers.IO) {
        sdkSampleServerAPI.createOrder(JSONObject(BA_TOKEN_BODY))
    }

    companion object {
        private const val BA_TOKEN_BODY = """
           {
              "intent": "CAPTURE",
              "purchase_units": [
                {
                
                  "amount": {
                    "currency_code": "USD",
                    "value": "95.00"
                  }
                }
              ],
              "payment_source": {
                "paypal": {
                  "attributes": {
                    "vault": {
                      "confirm_payment_token": "ON_ORDER_COMPLETION",
                      "usage_type": "MERCHANT"
                    }
                  }
                }
              },
              "application_context": {
                "return_url": "https://example.com/returnUrl",
                "cancel_url": "https://example.com/cancelUrl"
              }
            }
        """
    }
}
