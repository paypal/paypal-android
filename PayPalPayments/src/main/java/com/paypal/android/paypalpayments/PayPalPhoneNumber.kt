package com.paypal.android.paypalpayments

/**
 * Represents a shopper's phone number, split into its country calling code and national
 * (subscriber) number.
 *
 * @param countryCode The country calling code (e.g. "1" for the United States).
 * @param nationalNumber The national (subscriber) number, without the country calling code.
 */
data class PayPalPhoneNumber(
    val countryCode: String,
    val nationalNumber: String,
)
