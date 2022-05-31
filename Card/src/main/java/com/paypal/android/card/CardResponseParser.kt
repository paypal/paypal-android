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
import com.paypal.android.core.containsKey
import org.json.JSONException

internal class CardResponseParser {

    @Throws(PayPalSDKError::class)
    fun parseConfirmPaymentSourceResponse(
        response: String,
        correlationId: String?
    ): ConfirmPaymentSourceResponse =
        try {
            val json = PaymentsJSON(response)
            val status = json.getString("status")
            val id = json.getString("id")

            // this section is for 3DS
            val payerActionHref = json.getLinkHref("payer-action")
            ConfirmPaymentSourceResponse(
                id,
                OrderStatus.valueOf(status),
                payerActionHref,
                json.optGetJSONObject("payment_source.card")?.let { PaymentSource(it) },
                json.optGetJSONArray("purchase_units")?.let { PurchaseUnit.fromJSONArray(it) }
            )
        } catch (e: JSONException) {
            throw APIClientError.dataParsingError(correlationId, e)
        }

    @Throws(PayPalSDKError::class)
    fun parseGetOrderInfoResponse(response: String, correlationId: String?): GetOrderInfoResponse =
        try {
            val json = PaymentsJSON(response)
            GetOrderInfoResponse(json)
        } catch (e: JSONException) {
            throw APIClientError.dataParsingError(correlationId, e)
        }

    fun parseError(
        status: Int,
        bodyResponse: String,
        correlationID: String?
    ) = when (status) {
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

            val errorDetails = mutableListOf<OrderErrorDetail>()
            val errorDetailsJson = json.getJSONArray("details")
            for (i in 0 until errorDetailsJson.length()) {
                val errorJson = errorDetailsJson.getJSONObject(i)
                val issue = errorJson.getString("issue")
                val description = errorJson.getString("description")
                errorDetails += OrderErrorDetail(issue, description)
            }

            val description = "$message -> $errorDetails"
            APIClientError.httpURLConnectionError(
                status,
                description,
                correlationID
            )
        }
    }
}