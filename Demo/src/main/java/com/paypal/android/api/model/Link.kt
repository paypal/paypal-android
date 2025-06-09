package com.paypal.android.api.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Link(
    @SerializedName("href")
    val href: String? = null,
    @SerializedName("rel")
    val rel: String? = null,
    @SerializedName("method")
    val method: String? = null
) : Parcelable