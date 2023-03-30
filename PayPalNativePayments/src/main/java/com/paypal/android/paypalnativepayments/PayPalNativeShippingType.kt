package com.paypal.android.paypalnativepayments

import com.paypal.checkout.createorder.ShippingType

/**
 * The method by which the payer wants to get their items.
 */
enum class PayPalNativeShippingType {
    /**
     * The payer intends to receive the items at a specified address.
     */
    SHIPPING,

    /**
     * The payer intends to pick up the items at a specified address. For example, a store address.
     */
    PICKUP;

    companion object {
        internal fun fromShippingType(type: ShippingType?) = valueOf(type.toString())
    }
}
