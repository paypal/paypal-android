package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions

data class PayPalAuthChallenge(
    val options: BrowserSwitchOptions,
    internal val analytics: PayPalAnalyticsContext
)
