package com.paypal.android.paypalnativepayments

import com.paypal.checkout.createorder.ShippingType

/**
 * Deprecated. Use PayPalWebPayments module instead
 * The method by which the payer wants to get their items.
 */
@Deprecated("Deprecated. Use PayPalWebPayments module instead")
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
        internal fun fromShippingType(type: ShippingType?) = type?.let { valueOf(type.toString()) }
    }
}
