package com.paypal.android.api.model

import com.google.gson.annotations.SerializedName

data class Payee(
    @SerializedName("email_address")
    val emailAddress: String?
)
