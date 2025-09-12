package com.paypal.android.api.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Order(
    val id: String? = null,
    val intent: String? = null,
    val status: String? = null,
    @SerialName("last_digits")
    val cardLast4: String? = null,
    val cardBrand: String? = null,
    val vaultId: String? = null,
    val customerId: String? = null
) : Parcelable
