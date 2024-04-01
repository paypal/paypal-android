package com.paypal.android.fraudprotection

/**
 * Request object containing parameters to configure fraud protection data collection.
 *
 * @property [hasUserLocationConsent] true if the user has explicitly granted permission for a
 * merchant app to collect device data to protect against fraud; false if explicit permission was
 * not granted (default).
 * @property [clientMetadataId] forward this data to your server when completing a transaction
 * @property [additionalData] additional metadata to link with data collection
 */
data class PayPalDataCollectorRequest @JvmOverloads constructor(
    val hasUserLocationConsent: Boolean,
    val clientMetadataId: String? = null,
    val additionalData: Map<String, String>? = null,
)
