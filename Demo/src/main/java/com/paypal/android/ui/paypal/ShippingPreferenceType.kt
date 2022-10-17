package com.paypal.android.ui.paypal

import com.paypal.checkout.createorder.ShippingPreference

enum class ShippingPreferenceType {
    GET_FROM_FILE, NO_SHIPPING, SET_PROVIDED_ADDRESS;

    val description
        get() = when (this) {
            GET_FROM_FILE -> "Get From File"
            NO_SHIPPING -> "No Shipping"
            SET_PROVIDED_ADDRESS -> "Set Provided Address"
        }
    val nxoShippingPreference = ShippingPreference.valueOf(this.toString())
}
