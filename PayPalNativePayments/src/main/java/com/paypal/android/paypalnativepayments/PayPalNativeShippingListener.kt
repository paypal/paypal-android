package com.paypal.android.paypalnativepayments

/**
 * A listener to receive notifications if the user changes their shipping information.
 *
 * This is **only required** if the order ID was created with `shipping_preferences = GET_FROM_FILE`.
 * [See Orders V2 documentation](https://developer.paypal.com/docs/api/orders/v2/#definition-order_application_context).
 * If the order ID was created with `shipping_preferences = NO_SHIPPING` or `SET_PROVIDED_ADDRESS`,
 * don't implement this listener.
 */
interface PayPalNativeShippingListener {

    /**
     * Notify when the users selected shipping address changes. Use [PayPalNativePaysheetActions.approve]
     * or [PayPalNativePaysheetActions.reject] to approve or reject the newly selected shipping address.
     * Optionally, if the order needs to be patched, call [PayPalNativePaysheetActions.approve] once
     * patching has completed successfully.
     *
     * @param actions actions to perform after a change in shipping address
     * @param shippingAddress the user's most recently selected shipping address
     */
    fun onPayPalNativeShippingAddressChange(
        actions: PayPalNativePaysheetActions,
        shippingAddress: PayPalNativeShippingAddress
    )

    /**
     * Notify when the users selected a different shipping method. To reflect the newly selected
     * shipping method in the paysheet, patch the order on your server with operation 'replace', with all of the
     * shipping methods (marking the new one as selected). You can also update the amount to reflect
     * the new shipping cost. Once patching completes, its mandatory to call [PayPalNativePaysheetActions.approve] or
     * [PayPalNativePaysheetActions.reject] to either accept or reject the changes and continue the flow.
     * Visit https://developer.paypal.com/docs/api/orders/v2/#orders_patch for
     * more detailed information on patching an order.
     *
     * @param actions actions to perform after a change in shipping method
     * @param shippingMethod the user's most recently selected shipping method
     */
    fun onPayPalNativeShippingMethodChange(
        actions: PayPalNativePaysheetActions,
        shippingMethod: PayPalNativeShippingMethod
    )
}
