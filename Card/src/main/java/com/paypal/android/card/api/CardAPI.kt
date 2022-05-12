package com.paypal.android.card.api

import com.paypal.android.card.Card
import com.paypal.android.card.ConfirmPaymentSourceRequestFactory
import com.paypal.android.card.CreateOrderRequestFactory
import com.paypal.android.card.OrderRequest
import com.paypal.android.card.threedsecure.ThreeDSecureRequest
import com.paypal.android.core.API
import com.paypal.android.core.API.Companion.SUCCESSFUL_STATUS_CODES
import com.paypal.android.core.APIClientError
import com.paypal.android.core.APIRequest
import com.paypal.android.core.HttpResponse.Companion.SERVER_ERROR
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNDETERMINED
import com.paypal.android.core.HttpResponse.Companion.STATUS_UNKNOWN_HOST
import com.paypal.android.core.OrderErrorDetail
import com.paypal.android.core.PayPalSDKError
import com.paypal.android.core.PaymentsJSON

internal class CardAPI(
    private val api: API,
) {

    @Throws(PayPalSDKError::class)
    suspend fun confirmPaymentSource(
        orderID: String,
        card: Card,
        threeDSecureRequest: ThreeDSecureRequest? = null
    ): ConfirmPaymentSourceResponse {
        return performRequest(
            ConfirmPaymentSourceRequestFactory.createRequest(orderID, card, threeDSecureRequest),
            ConfirmPaymentSourceRequestFactory::parseResponse
        )
    }

    @Throws(PayPalSDKError::class)
    suspend fun createOrder(
        orderRequest: OrderRequest,
        threeDSecureRequest: ThreeDSecureRequest? = null
    ): CreateOrderResponse {
        return performRequest(
            CreateOrderRequestFactory.createRequest(orderRequest, threeDSecureRequest),
            CreateOrderRequestFactory::parseResponse
        )
    }

    private suspend fun <R> performRequest(
        apiRequest: APIRequest,
        parseResult: (body: String, correlationId: String?) -> R
    ): R {
        val httpResponse = api.send(apiRequest)
        val correlationID = httpResponse.headers["Paypal-Debug-Id"]

        val bodyResponse = httpResponse.body
        if (bodyResponse.isNullOrBlank()) {
            throw APIClientError.noResponseData(correlationID)
        }

        val status = httpResponse.status
        if (status in SUCCESSFUL_STATUS_CODES) {
            return parseResult(bodyResponse, correlationID)
        } else {
            throw parseError(status, bodyResponse, correlationID)
        }
    }

    private fun parseError(
        status: Int,
        bodyResponse: String,
        correlationID: String?
    ) = when (status) {
        STATUS_UNKNOWN_HOST -> {
            APIClientError.unknownHost(correlationID)
        }
        STATUS_UNDETERMINED -> {
            APIClientError.unknownError(correlationID)
        }
        SERVER_ERROR -> {
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
