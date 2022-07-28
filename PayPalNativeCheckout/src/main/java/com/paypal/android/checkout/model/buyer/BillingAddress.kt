package com.paypal.android.checkout.model.buyer

data class BillingAddress(
    val isPrimary: Boolean?,
    val fullAddress: String?,
    val addressId: String?,
    val line1: String? = null,
    val line2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val isSelected: Boolean = false
) {
    internal constructor(address: com.paypal.pyplcheckout.pojo.Address) : this(
        isPrimary = address.isPrimary,
        fullAddress = address.fullAddress,
        addressId = address.addressId
    )
}