package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResponse
import com.paypal.android.cardpayments.model.PaymentSource
import com.paypal.android.cardpayments.model.PurchaseUnit
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.OrderErrorDetail
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PaymentsJSON
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
            val correlationId = httpResponse.headers["Paypal-Debug-Id"]
            throw APIClientError.dataParsingError(correlationId)
        }

    fun parseError(httpResponse: HttpResponse): PayPalSDKError? {
        val result: PayPalSDKError?
        if (httpResponse.isSuccessful) {
            result = null
        } else {

            val correlationId = httpResponse.headers["Paypal-Debug-Id"]
            val bodyResponse = httpResponse.body
            if (bodyResponse.isNullOrBlank()) {
                result = APIClientError.noResponseData(correlationId)
            } else {
                result = when (val status = httpResponse.status) {
                    HttpResponse.STATUS_UNKNOWN_HOST -> {
                        APIClientError.unknownHost(correlationId)
                    }
                    HttpResponse.STATUS_UNDETERMINED -> {
                        APIClientError.unknownError(correlationId)
                    }
                    HttpResponse.SERVER_ERROR -> {
                        APIClientError.serverResponseError(correlationId)
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
                        APIClientError.httpURLConnectionError(status, description, correlationId)
                    }
                }
            }
        }

        return result
    }
}
