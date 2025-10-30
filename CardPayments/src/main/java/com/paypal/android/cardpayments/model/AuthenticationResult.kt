package com.paypal.android.cardpayments.model

import com.paypal.android.cardpayments.threedsecure.ThreeDSecureResult
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
internal data class AuthenticationResult(
    @SerialName("liability_shift")
    val liabilityShift: String?,
    @SerialName("three_d_secure")
    val threeDSecure: ThreeDSecureResult? = null
)
