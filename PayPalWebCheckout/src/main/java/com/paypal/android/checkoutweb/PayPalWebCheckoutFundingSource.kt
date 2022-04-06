package com.paypal.android.checkoutweb

/**
 * Enum class to specify the type of funding for an order.
 */
enum class PayPalWebCheckoutFundingSource(val value: String) {
    /**
     * PAYPAL_CREDIT will launch the web checkout flow and display PayPal Credit funding to eligible customers
     */
    PAYPAL_CREDIT("credit"),

    /**
     * PAY_LATER will launch the web checkout flow and display Pay Later offers to eligible customers.
     */
    PAY_LATER("paylater"),

    /**
     * PAYPAL will launch the web checkout for a one-time PayPal Checkout flow
     */
    PAYPAL("paypal")
}