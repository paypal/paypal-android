package com.paypal.android.utils

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer

@Composable
fun OnNewIntentEffect(callback: (newIntent: Intent) -> Unit) {
    val context = LocalContext.current
    // pass "Unit" to register listener only once
    DisposableEffect(Unit) {
        val listener = Consumer<Intent> { newIntent ->
            callback(newIntent)
        }
        val activity = context.getActivityOrNull()
        when (activity) {
            is androidx.activity.ComponentActivity -> activity.addOnNewIntentListener(listener)
            is NewIntentListenerHost -> activity.addOnNewIntentListener(listener)
        }
        onDispose {
            when (activity) {
                is androidx.activity.ComponentActivity -> activity.removeOnNewIntentListener(listener)
                is NewIntentListenerHost -> activity.removeOnNewIntentListener(listener)
            }
        }
    }
}
