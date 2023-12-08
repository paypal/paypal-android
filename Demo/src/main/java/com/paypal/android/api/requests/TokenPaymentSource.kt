package com.paypal.android.api.requests

import kotlinx.serialization.Serializable

@Serializable
data class TokenPaymentSource(
    var id: String = "",
    var type: TokenPaymentSourceType = TokenPaymentSourceType.SetupToken
) : SerializablePaymentSource()
