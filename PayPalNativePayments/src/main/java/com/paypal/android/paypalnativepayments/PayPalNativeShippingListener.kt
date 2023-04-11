package com.paypal.android.paypalnativepayments

/**
 * An optional listener to receive notifcations if the user changes their shipping information.
 */
interface PayPalNativeShippingListener {

    /**
     * Notify when the users selected shipping address changes
     *
     * @param actions actions to perform after patching the order
     * @param shippingAddress the user's most recently selected shipping address
     */
    fun onPayPalNativeShippingAddressChange(actions: PayPalNativeShippingActions, shippingAddress: PayPalNativeShippingAddress)

    /**
     * Notify when the users selected shipping method changes
     *
     * @param actions actions to perform after patching the order
     * @param shippingMethod the user's most recently selected shipping method
     */
    fun onPayPalNativeShippingMethodChange(actions: PayPalNativeShippingActions, shippingMethod: PayPalNativeShippingMethod)
}
