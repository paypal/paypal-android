package com.paypal.android.cardpayments

import com.paypal.android.corepayments.analytics.AnalyticsService

class CardAnalytics(
    private val analyticsService: AnalyticsService
) {

    fun notifyApproveOrderStarted(orderId: String) {
        val eventName = "card-payments:approve-order:started"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyConfirmPaymentSourceSucceeded(orderId: String) {
        val eventName = "card-payments:approve-order:confirm-payment-source-succeeded"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyConfirmPaymentSourceAuthChallengeReceived(orderId: String) {
        val eventName = "card-payments:approve-order:confirm-payment-auth-challenge-received"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyConfirmPaymentSourceFailed(orderId: String) {
        val eventName = "card-payments:approve-order:confirm-payment-source-failed"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
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
        analyticsService.sendAnalyticsEvent(
            "paypal-web-payments:browser-login:canceled",
            setupTokenId
        )
    }
}
