package com.paypal.android.cardpayments

import com.paypal.android.corepayments.analytics.AnalyticsService

@Suppress("TooManyFunctions")
internal class CardAnalytics(private val analyticsService: AnalyticsService) {

    private object ApproveOrderEvent {
        // @formatter:off
        const val STARTED                 = "card-payments:approve-order:started"
        const val SUCCEEDED               = "card-payments:approve-order:succeeded"
        const val FAILED                  = "card-payments:approve-order:failed"
        const val AUTH_CHALLENGE_REQUIRED = "card-payments:approve-order:auth-challenge-required"

        const val AUTH_CHALLENGE_PRESENTATION_SUCCEEDED = "card-payments:approve-order:auth-challenge-presentation:succeeded"
        const val AUTH_CHALLENGE_PRESENTATION_FAILED    = "card-payments:approve-order:auth-challenge-presentation:failed"

        const val AUTH_CHALLENGE_SUCCEEDED = "card-payments:approve-order:auth-challenge:succeeded"
        const val AUTH_CHALLENGE_FAILED    = "card-payments:approve-order:auth-challenge:failed"
        const val AUTH_CHALLENGE_CANCELED  = "card-payments:approve-order:auth-challenge-canceled"
        // @formatter:on
    }

    private fun sendEvent(
        eventName: String,
        orderId: String? = null,
        setupTokenId: String? = null
    ) {
        analyticsService.sendAnalyticsEvent(eventName, orderId ?: setupTokenId)
    }

    // region Approve Order
    fun notifyApproveOrderStarted(orderId: String?) =
        sendEvent(ApproveOrderEvent.STARTED, orderId = orderId)

    fun notifyApproveOrderSucceeded(orderId: String?) =
        sendEvent(ApproveOrderEvent.SUCCEEDED, orderId = orderId)

    fun notifyApproveOrderFailed(orderId: String?) =
        sendEvent(ApproveOrderEvent.FAILED, orderId = orderId)

    fun notifyApproveOrderAuthChallengeRequired(orderId: String) =
        sendEvent(ApproveOrderEvent.AUTH_CHALLENGE_REQUIRED, orderId = orderId)

    fun notifyApproveOrderAuthChallengePresentationSucceeded(orderId: String?) =
        sendEvent(ApproveOrderEvent.AUTH_CHALLENGE_PRESENTATION_SUCCEEDED, orderId = orderId)

    fun notifyApproveOrderAuthChallengePresentationFailed(orderId: String?) =
        sendEvent(ApproveOrderEvent.AUTH_CHALLENGE_PRESENTATION_FAILED, orderId = orderId)

    fun notifyApproveOrderAuthChallengeSucceeded(orderId: String?) =
        sendEvent(ApproveOrderEvent.AUTH_CHALLENGE_SUCCEEDED, orderId = orderId)

    fun notifyApproveOrderAuthChallengeFailed(orderId: String?) =
        sendEvent(ApproveOrderEvent.AUTH_CHALLENGE_FAILED, orderId = orderId)

    fun notifyApproveOrderAuthChallengeCanceled(orderId: String?) =
        sendEvent(ApproveOrderEvent.AUTH_CHALLENGE_CANCELED, orderId = orderId)
    // endregion

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
