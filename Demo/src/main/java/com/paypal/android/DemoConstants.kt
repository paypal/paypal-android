package com.paypal.android

import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig

object DemoConstants {
    const val APP_URL = "https://ppcp-mobile-demo-sandbox-87bbd7f0a27f.herokuapp.com"
    const val APP_CUSTOM_URL_SCHEME = "com.paypal.android.demo"

    val returnToAppUrlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "$APP_URL/paypal-return",
        cancelAppUrl = "$APP_URL/paypal-cancel",
        fallbackSchemeUrl = APP_CUSTOM_URL_SCHEME
    )
}
