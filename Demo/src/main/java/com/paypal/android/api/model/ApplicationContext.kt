package com.paypal.android.api.model

import com.google.gson.annotations.SerializedName

data class ApplicationContext(

    @SerializedName("return_url")
    val returnURL: String,

    @SerializedName("cancel_url")
    val cancelURL: String
)
