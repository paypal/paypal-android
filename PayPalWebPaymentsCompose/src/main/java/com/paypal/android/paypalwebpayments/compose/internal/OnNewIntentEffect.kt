package com.paypal.android.paypalwebpayments.compose.internal

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer

/**
 * Composable effect that registers a listener for new intents.
 *
 * This effect automatically manages the lifecycle of the onNewIntent listener,
 * registering it when the composable enters the composition and unregistering
 * it when the composable leaves the composition.
 *
 * @param callback The callback to invoke when a new intent is received
 */
@Composable
internal fun OnNewIntentEffect(callback: (newIntent: Intent) -> Unit) {
    val context = LocalContext.current
    // Pass "Unit" to register listener only once
    DisposableEffect(Unit) {
        val listener = Consumer<Intent> { newIntent ->
            callback(newIntent)
        }
        context.getActivityOrNull()?.addOnNewIntentListener(listener)
        onDispose {
            context.getActivityOrNull()?.removeOnNewIntentListener(listener)
        }
    }
}
