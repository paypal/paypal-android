package com.paypal.android.api.model

import com.google.gson.annotations.SerializedName

data class ClientId(
    @SerializedName("value")
    val clientId: String
)
