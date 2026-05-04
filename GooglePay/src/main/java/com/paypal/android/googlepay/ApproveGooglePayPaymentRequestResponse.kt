package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class ApproveGooglePayPaymentRequestResponse(
    val approveGooglePayPayment: ApproveGooglePayPayment
)

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class ApproveGooglePayPayment(
    val status: String,

    @SerialName("payment_source")
    val paymentSource: PaymentSource
)


@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class PaymentSource(
    @SerialName("google_pay")
    val googlePay: GooglePayPaymentSource
)

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class GooglePayPaymentSource(
    val card: GooglePayCard
)

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
internal data class GooglePayCard(
    @SerialName("last_digits")
    val lastDigits: String,
    val type: String,
    val brand: String
)
