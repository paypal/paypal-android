package com.paypal.android.cardpayments.model

import com.paypal.android.corepayments.OrderStatus
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
internal data class ConfirmPaymentSourceResponse(
    @SerialName("id")
    val id: String,
    @SerialName("status")
    val status: OrderStatus,
    @SerialName("links")
    val links: List<Link>? = null,
    @SerialName("payment_source")
    val paymentSource: PaymentSourceWrapper? = null,
    @SerialName("purchase_units")
    val purchaseUnits: List<PurchaseUnit>? = null
)

@InternalSerializationApi
@Serializable
internal data class PaymentSourceWrapper(
    @SerialName("card")
    val card: PaymentSource? = null
)

@InternalSerializationApi
@Serializable
internal data class Link(
    @SerialName("href")
    val href: String,
    @SerialName("rel")
    val rel: String,
    @SerialName("method")
    val method: String? = null
)

@InternalSerializationApi
@Serializable
internal data class ErrorResponse(
    @SerialName("message")
    val message: String,
    @SerialName("details")
    val details: List<ErrorDetail>? = null
)

@InternalSerializationApi
@Serializable
internal data class ErrorDetail(
    @SerialName("issue")
    val issue: String,
    @SerialName("description")
    val description: String
)
