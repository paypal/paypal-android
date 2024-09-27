package com.paypal.android.cardpayments

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService

internal class CardAnalytics(
    private val analyticsService: AnalyticsService
) {
    constructor(context: Context) : this(AnalyticsService(context.applicationContext))

    fun createAnalyticsContext(
        config: CoreConfig,
        orderId: String? = null,
        setupTokenId: String? = null
    ): CardAnalyticsContext {
        return CardAnalyticsContext(config, analyticsService, orderId, setupTokenId)
    }

    fun createAnalyticsContext(cardRequest: CardApproveOrderRequest): CardAnalyticsContext {
        return CardAnalyticsContext(
            cardRequest.config,
            analyticsService,
            orderId = cardRequest.orderId
        )
    }

    fun createAnalyticsContext(vaultRequest: CardVaultRequest): CardAnalyticsContext {
        return CardAnalyticsContext(
            vaultRequest.config,
            analyticsService,
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
            createAnalyticsContext( config, orderId = orderId )
        }

        is CardAuthMetadata.Vault -> metadata.run {
            createAnalyticsContext(config, setupTokenId = setupTokenId)
        }
    }
}