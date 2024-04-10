package com.paypal.android.fraudprotection

/**
 * Request object containing parameters to configure fraud protection data collection.
 *
 * @property [hasUserLocationConsent] informs the SDK if your application has obtained
 * consent from the user to collect location data in compliance with
 * <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">
 * Google Play Developer Program policies</a>
 * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
 * @property [clientMetadataId] forward this data to your server when completing a transaction
 * @property [additionalData] additional metadata to link with data collection
 */
data class PayPalDataCollectorRequest @JvmOverloads constructor(
    val hasUserLocationConsent: Boolean,
    val clientMetadataId: String? = null,
    val additionalData: Map<String, String>? = null,
)
