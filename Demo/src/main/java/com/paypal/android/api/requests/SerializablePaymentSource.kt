package com.paypal.android.api.requests

import kotlinx.serialization.Serializable

// Ref: https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#sealed-classes
@Serializable
sealed class SerializablePaymentSource
