package com.paypal.android.utils

import com.paypal.android.DemoConstants
import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig

object ReturnUrlProvider {
    val returnToAppUrlConfig = ReturnToAppUrlConfig(
        returnAppUrl = "${DemoConstants.APP_URL}/paypal-return",
        cancelAppUrl = "${DemoConstants.APP_URL}/paypal-cancel",
        fallbackSchemeUrl = DemoConstants.APP_CUSTOM_URL_SCHEME
    )
}
