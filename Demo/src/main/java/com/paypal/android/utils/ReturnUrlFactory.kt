package com.paypal.android.utils

import com.paypal.android.DemoConstants.APP_CUSTOM_URL_SCHEME
import com.paypal.android.DemoConstants.APP_URL
import com.paypal.android.uishared.enums.DeepLinkStrategy

object ReturnUrlFactory {

    fun createGenericReturnUrl(deepLinkStrategy: DeepLinkStrategy) = when (deepLinkStrategy) {
        DeepLinkStrategy.APP_LINKS -> APP_URL
        DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_CUSTOM_URL_SCHEME://"
    }

    fun createCheckoutSuccessUrl(deepLinkStrategy: DeepLinkStrategy) = when (deepLinkStrategy) {
        DeepLinkStrategy.APP_LINKS -> "$APP_URL/success"
        DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_CUSTOM_URL_SCHEME://success"
    }

    fun createCheckoutCancelUrl(deepLinkStrategy: DeepLinkStrategy) = when (deepLinkStrategy) {
        DeepLinkStrategy.APP_LINKS -> "$APP_URL/cancel"
        DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_CUSTOM_URL_SCHEME://cancel"
    }

    fun createVaultSuccessUrl(deepLinkStrategy: DeepLinkStrategy) = when (deepLinkStrategy) {
        DeepLinkStrategy.APP_LINKS -> "$APP_URL/vault/success"
        DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_CUSTOM_URL_SCHEME://vault/success"
    }

    fun createVaultCancelUrl(deepLinkStrategy: DeepLinkStrategy) = when (deepLinkStrategy) {
        DeepLinkStrategy.APP_LINKS -> "$APP_URL/vault/success"
        DeepLinkStrategy.CUSTOM_URL_SCHEME -> "$APP_CUSTOM_URL_SCHEME://vault/success"
    }
}
