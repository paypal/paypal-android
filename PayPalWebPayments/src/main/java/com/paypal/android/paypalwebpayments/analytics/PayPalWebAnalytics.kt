package com.paypal.android.paypalwebpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

@Suppress("TooManyFunctions")
internal class PayPalWebAnalytics(private val analyticsService: AnalyticsService) {

    fun notifyCheckoutStarted(orderId: String) {
        val eventName = "paypal-web-payments:checkout:started"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyCheckoutAuthChallengeStarted(orderId: String) {
        val eventName = "paypal-web-payments:checkout:auth-challenge-started"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyCheckoutAuthChallengeSucceeded(orderId: String?) {
        val eventName = "paypal-web-payments:checkout:auth-challenge-succeeded"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyCheckoutAuthChallengeFailed(orderId: String?) {
        val eventName = "paypal-web-payments:checkout:auth-challenge-failed"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyCheckoutAuthChallengeCanceled(orderId: String?) {
        val eventName = "paypal-web-payments:checkout:auth-challenge-canceled"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyVaultStarted(setupTokenId: String) {
        val eventName = "paypal-web-payments:vault:started"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeStarted(setupTokenId: String) {
        val eventName = "paypal-web-payments:vault:auth-challenge-started"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeSucceeded(setupTokenId: String?) {
        val eventName = "paypal-web-payments:vault:auth-challenge-succeeded"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeFailed(setupTokenId: String?) {
        val eventName = "paypal-web-payments:vault:auth-challenge-failed"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeCanceled(setupTokenId: String?) {
        val eventName = "paypal-web-payments:vault:auth-challenge-canceled"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }
}
