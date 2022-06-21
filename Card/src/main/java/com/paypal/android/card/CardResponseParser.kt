package com.paypal.android.card

import com.paypal.android.card.api.ConfirmPaymentSourceResponse
import com.paypal.android.card.api.GetOrderInfoResponse
import com.paypal.android.card.model.PaymentSource
import com.paypal.android.card.model.PurchaseUnit
import com.paypal.android.core.APIClientError
import com.paypal.android.core.HttpResponse
import com.paypal.android.core.OrderErrorDetail
import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON
import org.json.JSONException

internal class CardResponseParser {

    @Throws(PayPalSDKError::class)
    fun parseConfirmPaymentSourceResponse(httpResponse: HttpResponse): ConfirmPaymentSourceResponse =
        try {
            val bodyResponse = httpResponse.body!!

            val json = PaymentsJSON(bodyResponse)
            val status = json.getString("status")
            val id = json.getString("id")

            // this section is for 3DS
            val payerActionHref = json.getLinkHref("payer-action")
            ConfirmPaymentSourceResponse(
                id,
                OrderStatus.valueOf(status),
                payerActionHref,
                json.optMapObject("payment_source.card") { PaymentSource(it) },
                json.optMapObjectArray("purchase_units") { PurchaseUnit(it) }
            )
        } catch (ignored: JSONException) {
            val correlationID = httpResponse.headers["Paypal-Debug-Id"]
            throw APIClientError.dataParsingError(correlationID)
        }

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
