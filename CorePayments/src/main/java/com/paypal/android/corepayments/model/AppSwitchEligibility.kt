package com.paypal.android.corepayments.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class AppSwitchEligibility(
    val appSwitchEligible: Boolean,
    val launchUrl: String?,
    val ineligibleReason: String?
)
