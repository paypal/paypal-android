package com.paypal.android.ui.paypal

enum class ShippingPreferenceType {
    GET_FROM_FILE, NO_SHIPPING, SET_PROVIDED_ADDRESS;

    val description
        get() = when (this) {
            GET_FROM_FILE -> "Get From File"
            NO_SHIPPING -> "No Shipping"
            SET_PROVIDED_ADDRESS -> "Set Provided Address"
        }
}
