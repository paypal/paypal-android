package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResult
import com.paypal.android.cardpayments.model.ConfirmPaymentSourceResponse
import com.paypal.android.cardpayments.model.ErrorResponse
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.OrderErrorDetail
import com.paypal.android.corepayments.PayPalSDKError
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class CardResponseParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @OptIn(InternalSerializationApi::class)
    fun parseConfirmPaymentSourceResponse(httpResponse: HttpResponse): ConfirmPaymentSourceResult {
        val correlationId = httpResponse.headers["Paypal-Debug-Id"]
        val error = parseError(httpResponse)

        return when {
            error != null -> ConfirmPaymentSourceResult.Failure(error)
            httpResponse.body.isNullOrBlank() -> {
                ConfirmPaymentSourceResult.Failure(APIClientError.noResponseData(correlationId))
            }

            else -> {
                try {
                    val response =
                        json.decodeFromString<ConfirmPaymentSourceResponse>(httpResponse.body!!)
                    ConfirmPaymentSourceResult.Success(
                        orderId = response.id,
                        status = response.status,
                        payerActionHref = response.links?.firstOrNull { it.rel == "payer-action" }?.href,
                        paymentSource = response.paymentSource?.card,
                        purchaseUnits = response.purchaseUnits ?: emptyList()
                    )
                } catch (_: SerializationException) {
                    ConfirmPaymentSourceResult.Failure(APIClientError.dataParsingError(correlationId))
                }
            }
        }
    }

    fun parseError(httpResponse: HttpResponse): PayPalSDKError? {
        val correlationId = httpResponse.headers["Paypal-Debug-Id"]
        val bodyResponse = httpResponse.body
        val status = httpResponse.status

        return when {
            httpResponse.isSuccessful -> null
            bodyResponse.isNullOrBlank() -> APIClientError.noResponseData(correlationId)
            status == HttpResponse.STATUS_UNKNOWN_HOST -> APIClientError.unknownHost(correlationId)
            status == HttpResponse.STATUS_UNDETERMINED -> APIClientError.unknownError(correlationId)
            status == HttpResponse.SERVER_ERROR -> APIClientError.serverResponseError(correlationId)
            else -> parseDetailedError(bodyResponse, status, correlationId)
        }
    }

    @OptIn(InternalSerializationApi::class)
    private fun parseDetailedError(
        bodyResponse: String,
        status: Int,
        correlationId: String?
    ): PayPalSDKError =
        try {
            val errorResponse = json.decodeFromString<ErrorResponse>(bodyResponse)
            val errorDetails =
                errorResponse.details?.map { OrderErrorDetail(it.issue, it.description) }
            val description = "${errorResponse.message} -> $errorDetails"
            APIClientError.httpURLConnectionError(status, description, correlationId)
        } catch (_: SerializationException) {
            APIClientError.dataParsingError(correlationId)
        }
}
