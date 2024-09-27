package com.paypal.android.corepayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class CoreConfig @JvmOverloads constructor(
    val clientId: String,
    val environment: Environment = Environment.SANDBOX,
) : Parcelable
