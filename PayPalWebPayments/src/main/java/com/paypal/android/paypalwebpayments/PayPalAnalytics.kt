package com.paypal.android.paypalwebpayments

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.analytics.AnalyticsService
import java.util.UUID

internal class PayPalAnalytics(
    private val analyticsService: AnalyticsService
) {

    constructor(context: Context) : this(AnalyticsService(context.applicationContext))

    private fun createNewTrackingId() = UUID.randomUUID().toString()

    fun restoreFromAuthChallenge(authChallenge: PayPalAuthChallenge): PayPalAnalyticsContext? {
        val metadata =
            PayPalAuthMetadata.decodeFromString(authChallenge.options.metadata) ?: return null
        return restoreFromMetadata(metadata)
    }

    fun restoreFromMetadata(metadata: PayPalAuthMetadata): PayPalAnalyticsContext =
        when (metadata) {
            is PayPalAuthMetadata.Checkout -> metadata.run {
                createAnalyticsContext(config, trackingId = trackingId, orderId = orderId)
            }

            is PayPalAuthMetadata.Vault -> metadata.run {
                createAnalyticsContext(config, trackingId = trackingId, setupTokenId = setupTokenId)
            }
        }

    fun createAnalyticsContext(
        config: CoreConfig,
        trackingId: String,
        orderId: String? = null,
        setupTokenId: String? = null
    ): PayPalAnalyticsContext {
        return PayPalAnalyticsContext(
            config,
            analyticsService,
            trackingId,
            orderId = orderId,
            setupTokenId = setupTokenId
        )
    }

    fun createAnalyticsContext(request: PayPalWebCheckoutRequest): PayPalAnalyticsContext {
        return PayPalAnalyticsContext(
            request.config,
            analyticsService,
            trackingId = createNewTrackingId(),
            orderId = request.orderId
        )
    }

    fun createAnalyticsContext(vaultRequest: PayPalWebVaultRequest): PayPalAnalyticsContext {
        return PayPalAnalyticsContext(
            vaultRequest.config,
            analyticsService,
            trackingId = createNewTrackingId(),
            setupTokenId = vaultRequest.setupTokenId
        )
    }
}
