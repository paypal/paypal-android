package com.paypal.android.corepayments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ApproveGooglePayPaymentResponse(
    @SerialName("approveGooglePayPayment")
    val status: String
)
