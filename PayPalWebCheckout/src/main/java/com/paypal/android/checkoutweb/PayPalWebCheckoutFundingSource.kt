package com.paypal.android.checkoutweb

/**
 * Enum class to specify the type of funding for an order
 */
enum class PayPalWebCheckoutFundingSource(val value: String) {
    CREDIT("credit"), PAY_LATER("paylater"), PAYPAL("paypal")
}