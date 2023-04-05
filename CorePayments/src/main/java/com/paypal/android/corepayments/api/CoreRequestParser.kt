package com.paypal.android.corepayments.api

import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.OrderErrorDetail
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PaymentsJSON
import com.paypal.android.corepayments.api.models.GetOrderInfoResponse
import org.json.JSONException

open class CoreRequestParser {

    @Throws(PayPalSDKError::class)
    fun parseGetOrderInfoResponse(httpResponse: HttpResponse): GetOrderInfoResponse =
        try {
            val bodyResponse = httpResponse.body!!
            val json = PaymentsJSON(bodyResponse)
            GetOrderInfoResponse(json)
        } catch (ignored: JSONException) {
            val correlationID = httpResponse.headers["Paypal-Debug-Id"]
            throw APIClientError.dataParsingError(correlationID)
        }

    fun parseError(httpResponse: HttpResponse): PayPalSDKError? {
        val result: PayPalSDKError?
        if (httpResponse.isSuccessful) {
            result = null
        } else {

            val correlationID = httpResponse.headers["Paypal-Debug-Id"]
            val bodyResponse = httpResponse.body
            if (bodyResponse.isNullOrBlank()) {
                result = APIClientError.noResponseData(correlationID)
            } else {
                result = when (val status = httpResponse.status) {
                    HttpResponse.STATUS_UNKNOWN_HOST -> {
                        APIClientError.unknownHost(correlationID)
                    }
                    HttpResponse.STATUS_UNDETERMINED -> {
                        APIClientError.unknownError(correlationID)
                    }
                    HttpResponse.SERVER_ERROR -> {
                        APIClientError.serverResponseError(correlationID)
                    }
                    else -> {
                        val json = PaymentsJSON(bodyResponse)
                        val message = json.getString("message")

                        val errorDetails = json.optMapObjectArray("details") {
                            val issue = it.getString("issue")
                            val description = it.getString("description")
                            OrderErrorDetail(issue, description)
                        }

                        val description = "$message -> $errorDetails"
                        APIClientError.httpURLConnectionError(status, description, correlationID)
                    }
                }
            }
        }

        return result
    }
}
