package com.paypal.android.api.interceptors

import android.util.Log
import com.paypal.android.api.model.AuthToken
import com.paypal.android.api.services.AuthApi
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class TokenRefreshInterceptor @Inject constructor(
    private val authApi: AuthApi,
) : Interceptor {

    companion object {
        private const val EXPIRATION_FACTOR = 0.95
    }

    private val tag = this::class.java.toString()

    override fun intercept(chain: Interceptor.Chain): Response {
        val mainRequest = chain.request()
        var authToken = AuthApi.authToken
        val now = System.currentTimeMillis()
        if (authToken == null || authToken.expiresAt < now) {
            Log.i(tag, "Creating a new OAuth Token...")
            val token = runBlocking { authApi.postOAuthToken() } // refreshToken()
            Log.i(tag, "New token created...")
            Log.d(tag, "Token: $token")
            val expiresAt: Long = (token.expiresIn * EXPIRATION_FACTOR).toLong()
            val newAuthToken = AuthToken(token.accessToken, now + expiresAt)
            AuthApi.authToken = newAuthToken
            authToken = newAuthToken
        }
        val builder: Request.Builder =
            mainRequest.newBuilder().header("Authorization", "Bearer ${authToken.accessToken}")
        return chain.proceed(builder.build())
    }
}
