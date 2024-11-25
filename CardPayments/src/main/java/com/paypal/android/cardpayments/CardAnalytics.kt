package com.paypal.android.cardpayments

import com.paypal.android.corepayments.analytics.AnalyticsService

class CardAnalytics(
    private val analyticsService: AnalyticsService
) {

    fun notifyApproveOrderStarted(orderId: String) {
        val eventName = "card-payments:approve-order:started"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderSucceeded(orderId: String) {
        val eventName = "card-payments:approve-order:succeeded"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderFailed(orderId: String) {
        val eventName = "card-payments:approve-order:failed"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderAuthChallengeReceived(orderId: String) {
        val eventName = "card-payments:approve-order:auth-challenge-received"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderAuthChallengeStarted(orderId: String?) {
        val eventName = "card-payments:approve-order:auth-challenge-started"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderAuthChallengeSucceeded(orderId: String) {
        val eventName = "card-payments:approve-order:auth-challenge-succeeded"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderAuthChallengeCanceled(orderId: String?) {
        val eventName = "card-payments:approve-order:auth-challenge-canceled"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderAuthChallengeFailed(orderId: String?) {
        val eventName = "card-payments:approve-order:auth-challenge-failed"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyApproveOrderUnknownError(orderId: String?) {
        val eventName = "card-payments:approve-order:unknown-error"
        analyticsService.sendAnalyticsEvent(eventName, orderId)
    }

    fun notifyVaultStarted(setupTokenId: String) {
        val eventName = "card-payments:vault:started"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeReceived(setupTokenId: String) {
        val eventName = "card-payments:vault:auth-challenge-received"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeStarted(setupTokenId: String?) {
        val eventName = "card-payments:vault:auth-challenge-started"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeFailed(setupTokenId: String?) {
        val eventName = "card-payments:vault:auth-challenge-failed"
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
