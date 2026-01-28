package com.paypal.android.corepayments

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
internal data class ApproveGooglePayPaymentResponse(
    @SerialName("approveGooglePayPayment")
    val approveGooglePayPayment: ApprovalData
)

@InternalSerializationApi
@Serializable
internal data class ApprovalData(
    val id: String,
    val status: String,
    val links: List<Link>,
    @SerialName("payment_source")
    val paymentSource: PaymentSource
)

@InternalSerializationApi
@Serializable
internal data class Link(
    val href: String,
    val rel: String,
    val method: String
)

@InternalSerializationApi
@Serializable
internal data class PaymentSource(
    @SerialName("google_pay")
    val googlePay: GooglePayData
)

@InternalSerializationApi
@Serializable
internal data class GooglePayData(
    val card: CardData
)

@InternalSerializationApi
@Serializable
internal data class CardData(
    @SerialName("last_digits")
    val lastDigits: String,
    val type: String,
    val brand: String
)
