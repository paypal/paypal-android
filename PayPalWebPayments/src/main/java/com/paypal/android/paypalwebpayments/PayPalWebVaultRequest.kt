package com.paypal.android.paypalwebpayments

data class PayPalWebVaultRequest(
    val setupTokenId: String,
    val approveVaultHref: String
)
