package com.paypal.android.corepayments.features.eligibility

import androidx.annotation.RestrictTo

/**
 * Contains the result of a successful [EligibilityClient.check] response.
 *
 * @property [isVenmoEligible] indicates Venmo eligibility
 * @property [isCardEligible] indicates Card eligibility
 * @property [isPayPalEligible] indicates PayPal eligibility
 * @property [isPayLaterEligible] indicates PayPal Pay Later eligibility
 * @property [isCreditEligible] indicates PayPal Credit eligibility
 */
data class EligibilityResult @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    val isVenmoEligible: Boolean,
    val isCardEligible: Boolean,
    val isPayPalEligible: Boolean,
    val isPayLaterEligible: Boolean,
    val isCreditEligible: Boolean
)
