package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.model.PaymentSource
import com.paypal.android.cardpayments.model.PurchaseUnit
import com.paypal.android.corepayments.OrderStatus
import com.paypal.android.corepayments.PayPalSDKError
import kotlinx.serialization.InternalSerializationApi

internal sealed class ConfirmPaymentSourceResult {

    data class Success @OptIn(InternalSerializationApi::class) constructor(
        val orderId: String,
        val status: OrderStatus,
        val payerActionHref: String? = null,
        val paymentSource: PaymentSource? = null,
        val purchaseUnits: List<PurchaseUnit> = emptyList()
    ) : ConfirmPaymentSourceResult()

    data class Failure(val error: PayPalSDKError) : ConfirmPaymentSourceResult()
}
