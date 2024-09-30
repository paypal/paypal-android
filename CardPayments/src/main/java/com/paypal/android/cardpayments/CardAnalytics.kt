package com.paypal.android.cardpayments

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService
import java.util.UUID

internal class CardAnalytics(
    private val analyticsService: AnalyticsService
) {
    constructor(context: Context) : this(AnalyticsService(context.applicationContext))

    private fun createNewTrackingId() = UUID.randomUUID().toString()

    fun createAnalyticsContext(
        config: CoreConfig,
        trackingId: String,
        orderId: String? = null,
        setupTokenId: String? = null
    ): CardAnalyticsContext {
        return CardAnalyticsContext(config, analyticsService, trackingId, orderId, setupTokenId)
    }

    fun createAnalyticsContext(cardRequest: CardApproveOrderRequest): CardAnalyticsContext {
        return CardAnalyticsContext(
            cardRequest.config,
            analyticsService,
            trackingId = createNewTrackingId(),
            orderId = cardRequest.orderId
        )
    }

    fun createAnalyticsContext(vaultRequest: CardVaultRequest): CardAnalyticsContext {
        return CardAnalyticsContext(
            vaultRequest.config,
            analyticsService,
            trackingId = createNewTrackingId(),
            setupTokenId = vaultRequest.setupTokenId
        )
    }

    fun restoreFromAuthChallenge(authChallenge: CardAuthChallenge): CardAnalyticsContext? {
        val metadata =
            CardAuthMetadata.decodeFromString(authChallenge.options.metadata) ?: return null
        return restoreFromMetadata(metadata)
    }

    fun restoreFromMetadata(metadata: CardAuthMetadata): CardAnalyticsContext = when (metadata) {
        is CardAuthMetadata.ApproveOrder -> metadata.run {
            createAnalyticsContext(config, trackingId = trackingId, orderId = orderId)
        }

        is CardAuthMetadata.Vault -> metadata.run {
            createAnalyticsContext(config, trackingId = trackingId, setupTokenId = setupTokenId)
        }
    }
}