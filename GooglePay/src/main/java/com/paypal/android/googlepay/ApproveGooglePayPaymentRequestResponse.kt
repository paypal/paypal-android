package com.paypal.android.googlepay

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
data class ApproveGooglePayPaymentRequestResponse (
    val approveGooglePayPayment: ApproveGooglePayPayment
)

@InternalSerializationApi
@Serializable
@OptIn(InternalSerializationApi::class)
data class ApproveGooglePayPayment (
    val status: String
)
