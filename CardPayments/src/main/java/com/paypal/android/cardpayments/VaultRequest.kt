package com.paypal.android.cardpayments

/**
 * @property card   Card used for payment
 * @property returnUrl Url to return to app after SCA challenge finishes
 * @property customerId Optional customer Id to associate when vaulting
 */
data class VaultRequest(
    val card: Card,
    val returnUrl: String,
    val customerId: String? = null,
)
