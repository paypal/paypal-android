package com.paypal.android.paypalnativepayments

import com.paypal.checkout.order.Address

data class PayPalNativeShippingAddress internal constructor(
    val addressLine1: String?,
    val addressLine2: String?,
    val adminArea1: String?,
    val adminArea2: String?,
    val postalCode: String?,
    val countryCode: String?) {

    internal constructor(address: Address): this(
        address.addressLine1,
        address.addressLine2,
        address.adminArea1,
        address.adminArea2,
        address.postalCode,
        address.countryCode
    )
}