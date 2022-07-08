package com.paypal.android.core

class AuthHandler private constructor(private val authHeaderValue: String) {

    fun getAuthHeader() = authHeaderValue

    companion object {

        fun fromToken(accessToken: String): AuthHandler {
            return AuthHandler("Bearer $accessToken")
        }
    }
}
