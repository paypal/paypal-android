package com.paypal.android.api.services

import android.util.Base64
import com.paypal.android.BuildConfig
import com.paypal.android.api.model.AuthToken
import com.paypal.android.api.model.AuthTokenResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {

    /**
     * Creates a new OAuth Token that can be used for subsequent API requests.
     *
     * @see payPalAuthorization
     */
    @Headers("Accept: application/json", "Accept-Language: en_US")
    @FormUrlEncoded
    @POST("/v1/oauth2/token")
    suspend fun postOAuthToken(
        @Header("Authorization") authorization: String = payPalAuthorization,
        @Field("grant_type") grantType: String = "client_credentials"
    ): AuthTokenResponse

    companion object {

        /**
         * Valid credentials for creating a server <-> server auth token require a valid client id
         * as the user. This should be encoded in Base64.
         *
         * The proper format prior to encoding is as follows: clientId:secret
         *
         * OkHttp provides a convenient "Basic" function which handles the heavy lifting for us.
         */
        val payPalAuthorization: String

        var authToken: AuthToken? = null

        init {
            val basicAuth = "${BuildConfig.CLIENT_ID}:"
            val bytes = basicAuth.toByteArray(Charsets.UTF_8)
            payPalAuthorization = Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    }
}
