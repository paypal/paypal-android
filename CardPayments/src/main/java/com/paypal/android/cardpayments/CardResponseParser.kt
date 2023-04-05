package com.paypal.android.cardpayments

import com.paypal.android.cardpayments.api.ConfirmPaymentSourceResponse
import com.paypal.android.corepayments.models.PaymentSource
import com.paypal.android.corepayments.models.PurchaseUnit
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.PaymentsJSON
import com.paypal.android.corepayments.api.CoreRequestParser
import org.json.JSONException

internal class CardResponseParser : CoreRequestParser() {

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
}
