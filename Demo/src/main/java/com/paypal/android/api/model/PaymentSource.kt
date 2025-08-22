package com.paypal.android.api.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PaymentSource(
    val paypal: PayPalDetails? = null
) : Parcelable

@Parcelize
data class PayPalDetails(
    val emailAddress: String? = null,
    val appSwitchEligibility: Boolean? = null
) : Parcelable
