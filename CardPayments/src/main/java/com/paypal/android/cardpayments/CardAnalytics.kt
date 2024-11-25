package com.paypal.android.cardpayments

import com.paypal.android.corepayments.analytics.AnalyticsService

class CardAnalytics(
    private val analyticsService: AnalyticsService
) {

    fun notify3DSStarted(orderId: String) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:started", orderId)
    }

    fun notifyConfirmPaymentSourceSucceeded(orderId: String) {
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:succeeded",
            orderId
        )
    }

    fun notifyConfirmPaymentSourceChallengeRequired(orderId: String) {
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:challenge-required",
            orderId
        )
    }

    fun notifyConfirmPaymentSourceFailed(orderId: String) {
        analyticsService.sendAnalyticsEvent(
            "card-payments:3ds:confirm-payment-source:failed",
            orderId
        )
    }

    fun notifyApproveOrder3DSCanceled(orderId: String?) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:challenge:user-canceled", orderId)
    }

    fun notifyApproveOrder3DSSuccess(orderId: String) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:succeeded", orderId)
    }

    fun notifyApproveOrder3DSFailed(orderId: String?) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", orderId)
    }

    fun notifyVaultSuccess(setupTokenId: String) {
        analyticsService.sendAnalyticsEvent("card:browser-login:canceled", setupTokenId)
    }

    fun notifyVaultFailure(setupTokenId: String?) {
        analyticsService.sendAnalyticsEvent("card:failed", setupTokenId)
    }

    fun notifyVaultCancellation(setupTokenId: String?) {
        analyticsService.sendAnalyticsEvent("paypal-web-payments:browser-login:canceled", setupTokenId)
    }
}
