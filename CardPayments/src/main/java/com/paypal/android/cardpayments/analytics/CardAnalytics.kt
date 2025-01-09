package com.paypal.android.cardpayments.analytics

import com.paypal.android.corepayments.analytics.AnalyticsService

@Suppress("TooManyFunctions")
internal class CardAnalytics(private val analyticsService: AnalyticsService) {

    fun notify(event: ApproveOrderEvent, orderId: String?) {
        analyticsService.sendAnalyticsEvent(event.value, orderId)
    }

    // region Vault
    fun notifyVaultStarted(setupTokenId: String) {
        val eventName = "card-payments:vault:started"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultSucceeded(setupTokenId: String) {
        val eventName = "card-payments:vault:succeeded"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultFailed(setupTokenId: String?) {
        val eventName = "card-payments:vault:failed"
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

    fun notifyVaultAuthChallengeSucceeded(setupTokenId: String) {
        val eventName = "card-payments:vault:auth-challenge-succeeded"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeCanceled(setupTokenId: String?) {
        val eventName = "card-payments:vault:auth-challenge-canceled"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultAuthChallengeFailed(setupTokenId: String?) {
        val eventName = "card-payments:vault:auth-challenge-failed"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }

    fun notifyVaultUnknownError(setupTokenId: String?) {
        val eventName = "card-payments:vault:unknown-error"
        analyticsService.sendAnalyticsEvent(eventName, setupTokenId)
    }
    // endregion
}
