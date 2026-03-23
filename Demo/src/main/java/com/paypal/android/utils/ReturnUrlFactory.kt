package com.paypal.android.utils

import com.paypal.android.corepayments.ReturnToAppStrategy

object ReturnUrlFactory {

    fun createGenericReturnUrl(strategy: ReturnToAppStrategy, path: String? = null) =
        when (strategy) {
            is ReturnToAppStrategy.AppLink -> strategy.appLinkUrl + path?.let { "/$it" }
            is ReturnToAppStrategy.CustomUrlScheme -> "${strategy.urlScheme}://" + path.orEmpty()
    }

    fun createCheckoutSuccessUrl(strategy: ReturnToAppStrategy) = when (strategy) {
        is ReturnToAppStrategy.AppLink -> "${strategy.appLinkUrl}/success"
        is ReturnToAppStrategy.CustomUrlScheme -> "${strategy.urlScheme}://success"
    }

    fun createCheckoutCancelUrl(strategy: ReturnToAppStrategy) = when (strategy) {
        is ReturnToAppStrategy.AppLink -> "${strategy.appLinkUrl}/cancel"
        is ReturnToAppStrategy.CustomUrlScheme -> "${strategy.urlScheme}://cancel"
    }

    fun createVaultSuccessUrl(strategy: ReturnToAppStrategy) = when (strategy) {
        is ReturnToAppStrategy.AppLink -> "${strategy.appLinkUrl}/vault/success"
        is ReturnToAppStrategy.CustomUrlScheme -> "${strategy.urlScheme}://vault/success"
    }

    fun createVaultCancelUrl(strategy: ReturnToAppStrategy) = when (strategy) {
        is ReturnToAppStrategy.AppLink -> "${strategy.appLinkUrl}/vault/success"
        is ReturnToAppStrategy.CustomUrlScheme -> "${strategy.urlScheme}://vault/success"
    }
}
