package com.paypal.android.paypalwebpayments

import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService

class PayPalAnalyticsContext(
    private val config: CoreConfig,
    private val analyticsService: AnalyticsService,
    private val orderId: String? = null
) {
    fun notifyWebCheckoutFailure() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:failed", config, orderId = orderId)
    }

    fun notifyWebCheckoutStarted() {
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:started",
            config,
            orderId = orderId
        )
    }

    fun notifyWebVaultStarted() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:started", config)
    }

    fun notifyVaultFailure() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:failed", config)
    }

    fun notifyWebCheckoutSucceeded() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:succeeded", config, orderId)
    }

    fun notifyWebCheckoutUserCanceled() {
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:browser-login:canceled",
            config,
            orderId
        )
    }

    fun notifyWebVaultSucceeded() {
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:vault-wo-purchase:succeeded",
            config
        )
    }

    fun notifyWebVaultFailure() {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:vault-wo-purchase:failed", config)
    }

    fun notifyWebVaultUserCanceled() {
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:vault-wo-purchase:canceled",
            config
        )
    }
}
