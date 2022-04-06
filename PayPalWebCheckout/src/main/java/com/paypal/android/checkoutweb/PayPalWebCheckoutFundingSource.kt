package com.paypal.android.checkoutweb

/**
 * Enum class to specify the type of funding for an order
 */
enum class PayPalWebCheckoutFundingSource(val value: String) {
    /**
     * CREDIT will launch the web checkout flow with credit funding selected
     */
    CREDIT("credit"),

    /**
     * PAY_LATER will launch the web checkout flow with pay later selected
     */
    PAY_LATER("paylater"),

    /**
     * PAYPAL will launch the web checkout default flow
     */
    PAYPAL("paypal")
}