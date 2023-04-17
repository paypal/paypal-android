package com.paypal.android.paypalnativepayments

import com.paypal.checkout.shipping.ShippingChangeActions

/**
 * The actions that can be performed when the [PayPalNativeShippingListener] methods are invoked.
 */
class PayPalNativeShippingActions(private val shippingChangeActions: ShippingChangeActions) {

    /**
     * Will refresh the paysheet with the latests updates to the current order. Call [approve] when:
     * <p><ul>
     * <li>A buyer selects a shipping address that is supported. Removes the error message on the
     * paysheet displayed after calling [reject].
     * <li>After an order has been successfully patched on your server (e.g, update amount after new shipping method
     * has been selected), to see the changes reflected on paysheet.
     * For more information on patching an order, visit: https://developer.paypal.com/docs/api/orders/v2/#orders_patch
     * </ul><p>
     */
    fun approve() {
        shippingChangeActions.approve()
    }

    /**
     * Call [reject] when a buyer selects a shipping option that is not supported or has entered a
     * shipping address that is not supported. The paysheet will require the buyer to fix the issue
     * before continuing with the order. Remove the error message by calling [approve]
     */
    fun reject() {
        shippingChangeActions.reject()
    }
}
