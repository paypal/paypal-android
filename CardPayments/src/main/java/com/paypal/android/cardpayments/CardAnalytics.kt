package com.paypal.android.cardpayments

import com.paypal.android.corepayments.analytics.AnalyticsService

class CardAnalytics(
    private val analyticsService: AnalyticsService
) {

    fun notifyApproveOrderStarted(orderId: String) {
        val eventName = "card-payments:approve-order:started"
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

    fun notifyApproveOrderSucceededWithout3DS(orderId: String) {
        val eventName = "card-payments:approve-order:succeeded-without-3ds"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrder3DSFailed(orderId: String?) {
        analyticsService.sendAnalyticsEvent("card-payments:3ds:failed", orderId)
    }

    fun notifyVaultStarted(setupTokenId: String) {
        val eventName = "card-payments:vault:started"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeReceived(setupTokenId: String) {
        val eventName = "card-payments:vault:auth-challenge-received"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultSuccess(setupTokenId: String) {
        analyticsService.sendAnalyticsEvent("card:browser-login:canceled", setupTokenId)
    }

    fun notifyVaultFailure(setupTokenId: String?) {
        analyticsService.sendAnalyticsEvent("card:failed", setupTokenId)
    }

    fun notifyVaultCancellation(setupTokenId: String?) {
        val eventName = "paypal-web-payments:browser-login:canceled"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }
}
