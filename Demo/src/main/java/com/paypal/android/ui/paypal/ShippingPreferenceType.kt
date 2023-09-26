package com.paypal.android.ui.paypal

import com.paypal.checkout.createorder.ShippingPreference

enum class ShippingPreferenceType {
    GET_FROM_FILE, NO_SHIPPING, SET_PROVIDED_ADDRESS;

    val nxoShippingPreference = ShippingPreference.valueOf(this.toString())
}
