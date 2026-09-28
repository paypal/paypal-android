package com.paypal.android.customenvironment

import com.paypal.android.DemoActivityType

internal data class ActivityTypeSettings(
    val type: DemoActivityType,
    val onSwitch: () -> Unit,
)
