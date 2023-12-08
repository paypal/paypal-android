package com.paypal.android.api.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TokenPaymentSourceType {
    @SerialName("SETUP_TOKEN")
    SetupToken
}
