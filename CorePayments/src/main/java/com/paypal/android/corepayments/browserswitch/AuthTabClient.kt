package com.paypal.android.corepayments.browserswitch

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri

class AuthTabClient {
    fun launchAuthTab(
        options: ChromeCustomTabOptions,
        activityResultLauncher: ActivityResultLauncher<Intent>,
        appLinkUrl: String? = null,
        returnUrlScheme: String? = null
    ) {
        val appLinkUri = appLinkUrl?.toUri()
        val redirectHost = appLinkUri?.host
        val redirectPath = appLinkUri?.path
        val authTabIntent = AuthTabIntent.Builder().build()

        when {
            redirectHost != null && redirectPath != null -> {
                authTabIntent.launch(
                    activityResultLauncher,
                    options.launchUri,
                    redirectHost,
                    redirectPath
                )
            }

            returnUrlScheme != null -> {
                authTabIntent.launch(
                    activityResultLauncher,
                    options.launchUri,
                    returnUrlScheme
                )
            }

            else -> {
                throw IllegalArgumentException("Either appLinkUrl or returnUrlScheme must not be null")
            }
        }
    }
}