package com.paypal.android.fraudprotection

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

internal class SharedPreferenceUtils {

    companion object {
        val instance = SharedPreferenceUtils()
    }

    private fun getSharedPreferences(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    fun putString(context: Context, key: String, value: String) =
        getSharedPreferences(context)
                .edit()
                .putString(key, value)
                .apply()

    fun getString(context: Context, key: String, fallback: String?): String? =
         getSharedPreferences(context).getString(key, fallback) ?: fallback
}
