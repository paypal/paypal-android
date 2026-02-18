package com.paypal.android.models

import com.paypal.android.api.model.OrderIntent
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class OrderRequest(
    val intent: OrderIntent,
    val shouldVaultOnSuccess: Boolean,
    val appSwitchWhenEligible: Boolean,
    @Transient val returnToAppStrategy: ReturnToAppStrategyOption = ReturnToAppStrategyOption.APP_LINKS
)
