package com.paypal.android.paypalnativepayments

/**
 * Used to configure options for approving a PayPal native order
 */
data class PayPalNativeCheckoutRequest(
    /**
     * The order ID associated with the request.
     */
    val orderId: String,

    /**
     * Optional: User email to initiate a quicker authentication flow
     * in cases where the user has a PayPal Account with the same email.
     */
    val userAuthenticationEmail: String? = null,

    /**
     * @property [hasUserLocationConsent] informs the SDK if your application has obtained
     * consent from the user to collect location data in compliance with
     * <a href="https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive">
     * Google Play Developer Program policies</a>
     * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
     */
    val hasUserLocationConsent: Boolean = false
) {
    @Deprecated(
        message = "Use the constructor to pass in the `hasUserLocationConsent` parameter.",
        replaceWith = ReplaceWith("PayPalNativeCheckoutRequest(orderId, hasUserLocationConsent)")
    )
    constructor(orderId: String) : this(orderId, null, false)

    @Deprecated("Use the constructor to pass in the `hasUserLocationConsent` parameter.",
        replaceWith = ReplaceWith("PayPalNativeCheckoutRequest(orderId, userAuthenticationEmail, hasUserLocationConsent)")
    )
    constructor(orderId: String, userAuthenticationEmail: String?) : this(
        orderId,
        userAuthenticationEmail,
        false
    )
}
