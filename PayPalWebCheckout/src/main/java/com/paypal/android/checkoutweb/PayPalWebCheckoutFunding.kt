package com.paypal.android.checkoutweb

/**
 * Enum class to specify the type of funding for an order
 */
enum class PayPalWebCheckoutFunding(val value: String) {
    CREDIT("credit"), PAY_LATER("paylater"), DEFAULT("")
}