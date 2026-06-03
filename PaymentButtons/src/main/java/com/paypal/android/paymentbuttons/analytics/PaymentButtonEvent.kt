package com.paypal.android.paymentbuttons.analytics

internal enum class PaymentButtonEvent(val value: String) {
    INITIALIZED("payment-button:initialized"),
    TAPPED("payment-button:tapped"),
}
