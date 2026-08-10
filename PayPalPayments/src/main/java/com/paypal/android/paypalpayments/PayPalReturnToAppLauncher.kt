package com.paypal.android.paypalpayments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.net.toUri

internal class PayPalReturnToAppLauncher(
    private val handler: Handler = Handler(Looper.getMainLooper())
) {

    fun launch(context: Context, cancelUrl: String, isCurrent: () -> Boolean) {
        val applicationContext = context.applicationContext ?: context
        val cancelUri = cancelUrl.toUri().buildUpon()
            .appendQueryParameter(CANCELLATION_QUERY_PARAM, "true")
            .build()
        handler.post {
            if (isCurrent()) {
                val intent = Intent(Intent.ACTION_VIEW, cancelUri)
                    .setPackage(applicationContext.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                try {
                    applicationContext.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    // Keep the terminal state available for the next regular finish call.
                } catch (_: SecurityException) {
                    // Keep the terminal state available for the next regular finish call.
                }
            }
        }
    }

    companion object {
        const val CANCELLATION_QUERY_PARAM = "paypal-sdk-custom-tab-canceled"
    }
}
