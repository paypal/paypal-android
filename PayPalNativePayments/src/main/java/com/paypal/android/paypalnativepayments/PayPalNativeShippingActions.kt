package com.paypal.android.paypalnativepayments

import com.paypal.checkout.shipping.ShippingChangeActions

class PayPalNativeShippingActions(private val shippingChangeActions: ShippingChangeActions) {

    fun approve() {
        shippingChangeActions.approve()
    }

    fun reject() {
        shippingChangeActions.reject()
    }
}
