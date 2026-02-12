package com.paypal.android.uishared.enums

import com.paypal.android.DemoConstants
import com.paypal.android.corepayments.ReturnToAppStrategy

enum class ReturnToAppStrategyOption {
    APP_LINKS,
    CUSTOM_URL_SCHEME;

    fun toReturnToAppStrategy(): ReturnToAppStrategy = when (this) {
        APP_LINKS -> ReturnToAppStrategy.AppLink(DemoConstants.APP_URL)
        CUSTOM_URL_SCHEME -> ReturnToAppStrategy.CustomUrlScheme(DemoConstants.APP_CUSTOM_URL_SCHEME)
    }
}
