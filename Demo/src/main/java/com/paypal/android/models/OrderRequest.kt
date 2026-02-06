package com.paypal.android.models

import com.paypal.android.api.model.OrderIntent
import com.paypal.android.uishared.enums.DeepLinkStrategy
import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val intent: OrderIntent,
    val shouldVaultOnSuccess: Boolean,
    val appSwitchWhenEligible: Boolean,
    val deepLinkStrategy: DeepLinkStrategy
)
