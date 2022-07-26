package com.paypal.android.checkout.model.buyer

data class Name(
    var fullName: String?,
    var givenName: String?,
    var familyName: String?,
    val additionalProperties: MutableMap<String, Any>? = HashMap()
) {
    internal constructor(name: com.paypal.pyplcheckout.pojo.Name?) : this(
        fullName = name?.fullName,
        givenName = name?.givenName,
        familyName = name?.familyName,
        additionalProperties = name?.additionalProperties
    )
}