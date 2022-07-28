package com.paypal.android.checkout.model.buyer

data class Phone(
    val number: String?,
    val countryCode: String? = null,
    val e164: String? = null
) {
    internal constructor(phone: com.paypal.pyplcheckout.pojo.Phone) : this(number = phone.number)
}