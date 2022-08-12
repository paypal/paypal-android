package com.paypal.android.usecase

import androidx.core.net.toUri
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.paypal.android.api.services.PayPalApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetApprovalSessionIdActionUseCase @Inject constructor(
    private val payPalApi: PayPalApi
) {

    suspend operator fun invoke(accessToken: String): String? = withContext(Dispatchers.IO) {
        val jsonRequest = JsonParser.parseString(APPROVAL_SESSION_ID_REQUEST) as JsonObject
        val vaultSessionId = payPalApi.postApprovalSessionId("Bearer $accessToken", jsonRequest)

        val approvalSessionIdLink = vaultSessionId
            .links
            ?.find { vaultLink -> vaultLink?.rel == "approve" }
        approvalSessionIdLink?.href?.toUri()?.getQueryParameter("approval_session_id")
    }

    companion object {
        private const val APPROVAL_SESSION_ID_REQUEST = """
            {
              "customer_id": "abcd1234",
              "source": {
                "paypal": {
                  "usage_type": "MERCHANT",
                  "customer_type": "CONSUMER"
                }
              },
              "application_context": {
                "locale": "en-US",
                "return_url": "https://example.com",
                "cancel_url": "https://example.com",
                "payment_method_preference": {
                  "payee_preferred": "IMMEDIATE_PAYMENT_REQUIRED",
                  "payer_selected": "PAYPAL"
                }
              }
            }
        """
    }
}
