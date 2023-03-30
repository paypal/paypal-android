package com.paypal.android.paypalnativepayments

import com.paypal.checkout.order.Options


data class PayPalNativeShippingMethod internal constructor(
    val id: String,
    val selected: Boolean,
    val label: String,
    val type: PayPalNativeShippingType?,
    val value: String?,
    val currencyCode: String?
) {

    internal constructor(options: Options) : this(
        options.id,
        options.selected,
        options.label,
        PayPalNativeShippingType.fromShippingType(options.type),
        options.amount?.value,
        options.amount?.currencyCode?.name
    )
}