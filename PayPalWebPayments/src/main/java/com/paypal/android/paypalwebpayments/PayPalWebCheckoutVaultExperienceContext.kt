package com.paypal.android.paypalwebpayments

data class PayPalWebCheckoutVaultExperienceContext internal constructor(
    val returnUrl: String,
    val cancelUrl: String
) {
    internal constructor(
        urlScheme: String,
        domain: String,
        returnUrlPath: String,
        cancelUrlPath: String
    ) : this(
        returnUrl = "$urlScheme://$domain/$returnUrlPath",
        cancelUrl = "$urlScheme://$domain/$cancelUrlPath"
    )
}
