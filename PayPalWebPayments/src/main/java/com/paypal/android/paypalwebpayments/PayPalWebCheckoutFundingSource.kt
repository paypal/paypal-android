package com.paypal.android.paypalwebpayments

/**
 * Enum class to specify the type of funding for an order.
 * For more information go to: https://developer.paypal.com/docs/checkout/pay-later/us/
 */
enum class PayPalWebCheckoutFundingSource(val value: String) {
    /**
     * PAYPAL_CREDIT will launch the web checkout flow and display PayPal Credit funding to eligible customers
     * Eligible costumers receive a revolving line of credit that they can use to pay over time.
     */
    PAYPAL_CREDIT("credit"),

    /**
     * PAY_LATER will launch the web checkout flow and display Pay Later offers to eligible customers,
     * which include short-term, interest-free payments and other special financing options.
     */
    PAY_LATER("paylater"),

    /**
     * PAYPAL will launch the web checkout for a one-time PayPal Checkout flow
     */
    PAYPAL("paypal")
}
