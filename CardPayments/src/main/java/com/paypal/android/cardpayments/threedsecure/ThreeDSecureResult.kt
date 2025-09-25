package com.paypal.android.cardpayments.threedsecure

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
internal data class ThreeDSecureResult(
    @SerialName("enrollment_status")
    val enrollmentStatus: String? = null,
    @SerialName("authentication_status")
    val authenticationStatus: String? = null
)
