package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.browserswitch.BrowserSwitchOptions

data class PayPalAuthChallenge(
    override val options: BrowserSwitchOptions,
    internal val analytics: PayPalAnalyticsContext
) : BrowserSwitchAuthChallenge
