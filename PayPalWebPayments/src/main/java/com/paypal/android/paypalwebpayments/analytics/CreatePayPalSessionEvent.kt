@file:Suppress("SpacingAroundParens", "NoMultipleSpaces", "MaxLineLength")

package com.paypal.android.paypalwebpayments.analytics

// v3 flow — fired by PayPalWebCheckoutClient.createPayPalSession(), which pre-warms the
// shopper session ahead of start()/vault(). Shared by both checkout and vault callers since
// createPayPalSession() itself has no orderId/setupTokenId to attach.
internal enum class CreatePayPalSessionEvent(val value: String) {
    // @formatter:off
    STARTED(  "paypal-web-payments:create-paypal-session:started"),
    SUCCEEDED("paypal-web-payments:create-paypal-session:succeeded"),
    FAILED(   "paypal-web-payments:create-paypal-session:failed"),
    // @formatter:on
}
