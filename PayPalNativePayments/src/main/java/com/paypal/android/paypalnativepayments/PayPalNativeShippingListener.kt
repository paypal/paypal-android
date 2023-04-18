package com.paypal.android.paypalnativepayments

/**
 * An optional listener to receive notifications if the user changes their shipping information.
 */
interface PayPalNativeShippingListener {

    /**
     * Notify when the users selected shipping address changes. Use [PayPalNativeShippingActions.approve]
     * or [PayPalNativeShippingActions.reject] to approve or reject the newly selected shipping address.
     * Optionally, if the order needs to be patched, call [PayPalNativeShippingActions.approve] once
     * patching has completed successfully.
     *
     * @param actions actions to perform after a change in shipping address
     * @param shippingAddress the user's most recently selected shipping address
     */
    fun onPayPalNativeShippingAddressChange(
        actions: PayPalNativeShippingActions,
        shippingAddress: PayPalNativeShippingAddress
    )

    /**
     * Notify when the users selected a different shipping method. To reflect the newly selected
     * shipping method in the paysheet, patch the order on your server with operation 'replace', with all of the
     * shipping methods (marking the new one as selected). You can also update the amount to reflect
     * the new shipping cost. Once patching completes, its mandatory to call [PayPalNativeShippingActions.approve] or
     * [PayPalNativeShippingActions.reject] to either accept or reject the changes and continue the flow.
     * Visit https://developer.paypal.com/docs/api/orders/v2/#orders_patch for
     * more detailed information on patching an order.
     *
     * @param actions actions to perform after a change in shipping method
     * @param shippingMethod the user's most recently selected shipping method
     */
    fun onPayPalNativeShippingMethodChange(
        actions: PayPalNativeShippingActions,
        shippingMethod: PayPalNativeShippingMethod
    )
}
