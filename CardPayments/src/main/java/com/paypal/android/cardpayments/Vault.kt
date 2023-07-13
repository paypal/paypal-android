package com.paypal.android.cardpayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Create an instance of this class to configure a [CardRequest] for vaulting.
 *
 * @property customerId Optional. When set, the value of this property will be used to associate
 * a payment method with the vault of a customer with this ID.
 */
@Parcelize
data class Vault(val customerId: String? = null) : Parcelable
