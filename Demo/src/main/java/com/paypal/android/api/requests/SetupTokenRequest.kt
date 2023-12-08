package com.paypal.android.api.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SetupTokenRequest(
    @SerialName("payment_source")
    var paymentSource: MutableMap<PaymentSource, SerializablePaymentSource> = mutableMapOf()
)
