package com.paypal.android.corepayments

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.browser.customtabs.CustomTabsIntent

// Ref: https://developer.android.com/training/basics/intents/result#custom
class LaunchChromeCustomTab : ActivityResultContract<Uri, ChromeCustomTabsResult>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        val intent = customTabsIntent.intent
        intent.data = input
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?)
        = ChromeCustomTabsResult(resultCode = resultCode, intent = intent)
}