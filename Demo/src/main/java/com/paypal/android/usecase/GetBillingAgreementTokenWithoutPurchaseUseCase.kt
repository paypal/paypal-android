package com.paypal.android.usecase

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.services.PayPalApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetBillingAgreementTokenWithoutPurchaseUseCase
    @Inject constructor(private val payPalApi: PayPalApi) {

    suspend operator fun invoke(accessToken: String): String = withContext(Dispatchers.IO) {
        val jsonOrder = JsonParser.parseString(BA_TOKEN_BODY) as JsonObject
        val baToken = payPalApi.postBillingAgreementToken("Bearer $accessToken", jsonOrder)
        baToken.tokenId
    }

    companion object {
        private const val BA_TOKEN_BODY = """
            {
              "description": "Billing Agreement",
              "shipping_address":
              {
                "line1": "1350 North First Street",
                "city": "San Jose",
                "state": "CA",
                "postal_code": "95112",
                "country_code": "US",
                "recipient_name": "John Doe"
              },
              "payer":
              {
                "payment_method": "PAYPAL"
              },
              "plan":
              {
                "type": "MERCHANT_INITIATED_BILLING",
                "merchant_preferences":
                {
                  "return_url": "https://example.com/return",
                  "cancel_url": "https://example.com/cancel",
                  "notify_url": "https://example.com/notify",
                  "accepted_pymt_type": "INSTANT",
                  "skip_shipping_address": false,
                  "immutable_shipping_address": true
                }
              }
            }
        """
    }
}
