package com.paypal.android.checkout.model.buyer

data class Email(
    val stringValue: String?,
    val additionalProperties: Map<String, Any>? = HashMap()
) {
    internal constructor(email: com.paypal.pyplcheckout.pojo.Email?) : this(
        stringValue = email?.stringValue,
        additionalProperties = email?.additionalProperties
    )
}