package com.paypal.android.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.paypal.android.api.model.AuthToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson




@Singleton
class SharedPreferenceUtil @Inject constructor(@ApplicationContext context : Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun getEnvironment(): String? {
        return prefs.getString(ENVIRONMENT_KEY, "")
    }

    fun getAuthToken(): AuthToken? {
        val gson = Gson()
        val json = prefs.getString(AUTH_TOKEN, "")
        return if (json == "") {
            null
        } else {
            gson.fromJson(json, AuthToken::class.java)
        }
    }

    fun setAuthToken(authToken: AuthToken) {
        val gson = Gson()
        val json = gson.toJson(authToken)
        prefs.edit().putString(AUTH_TOKEN, json).apply()
    }

    companion object  {
        private const val ENVIRONMENT_KEY = "environment"
        private const val AUTH_TOKEN = "auth_token"
    }

}