package com.paypal.android.api.model

import com.google.gson.annotations.SerializedName

/**
 * AuthToken is used for capturing an OAuth Token and the params available here are a subset
 * of the ones actually returned.
 *
 * @property accessToken should be used for other API requests, passing it in as "Bearer [accessToken]".
 * @property expiresIn provides the amount of time this token will be valid for to make it easier to
 * know when to re-authenticate.
 */
data class AuthTokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("expires_in")
    val expiresIn: Long
)