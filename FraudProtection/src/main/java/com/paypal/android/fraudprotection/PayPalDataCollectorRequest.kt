package com.paypal.android.fraudprotection

data class PayPalDataCollectorRequest @JvmOverloads constructor(
    val hasUserLocationConsent: Boolean,
    val clientMetadataId: String? = null,
    val additionalData: Map<String, String>? = null,
)
