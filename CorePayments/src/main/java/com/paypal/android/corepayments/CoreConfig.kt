package com.paypal.android.corepayments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoreConfig @JvmOverloads constructor(
    val clientId: String,
    val environment: Environment = Environment.SANDBOX,
) : Parcelable
