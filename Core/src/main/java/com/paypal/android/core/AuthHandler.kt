package com.paypal.android.core

class AuthHandler private constructor(private val authHeaderValue: String) {

    fun getAuthHeader() = authHeaderValue

    companion object {

        fun fromClientId(clientId: String): AuthHandler {
            val credentials = "$clientId:".base64encoded()
            return AuthHandler("Basic $credentials")
        }

        fun fromToken(accessToken: String): AuthHandler {
            return AuthHandler("Bearer $accessToken")
        }
    }
}