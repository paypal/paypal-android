package com.paypal.android.api.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PayPalSetupToken(
    val id: String,
    val customerId: String,
    val status: String,
    val links: List<Link>? = null,
) : Parcelable {
    val approveUrl: String?
        get() = links?.firstOrNull { it.rel == "approve" }?.href
}
