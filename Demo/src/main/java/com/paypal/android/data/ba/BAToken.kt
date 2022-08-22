package com.paypal.android.data.ba

import com.google.gson.annotations.SerializedName

data class BAToken(
    @SerializedName("token_id")
    val tokenId: String
)
