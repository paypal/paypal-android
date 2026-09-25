package com.paypal.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

enum class DemoActivityType(
    val description: String,
    val switchActionLabel: String,
) {
    COMPONENT_ACTIVITY(
        description = "This ComponentActivity uses an Auth Tab for web checkout. " +
            "The plain Activity uses a Chrome Custom Tab.",
        switchActionLabel = "Use plain Activity",
    ),
    PLAIN_ACTIVITY(
        description = "This plain Activity uses a Chrome Custom Tab for web checkout. " +
            "MainActivity uses an Auth Tab.",
        switchActionLabel = "Use MainActivity",
    ),
}

fun Activity.switchDemoActivityType(currentType: DemoActivityType) {
    val destinationType = when (currentType) {
        DemoActivityType.COMPONENT_ACTIVITY -> DemoActivityType.PLAIN_ACTIVITY
        DemoActivityType.PLAIN_ACTIVITY -> DemoActivityType.COMPONENT_ACTIVITY
    }
    saveDemoActivityType(destinationType)
    startActivity(Intent(this, destinationType.activityClass))
    finish()
}

/**
 * Records normal launches and redirects checkout return intents to the last selected Activity type.
 * CustomTabActivity owns the manifest filters and forwards returns when MainActivity started checkout.
 */
fun Activity.redirectCheckoutReturnIfNeeded(currentType: DemoActivityType): Boolean {
    val isCheckoutReturn = intent.action == Intent.ACTION_VIEW
    val selectedType = if (isCheckoutReturn) getSavedDemoActivityType() else currentType
    val shouldRedirect = isCheckoutReturn && selectedType != currentType

    if (!isCheckoutReturn) {
        saveDemoActivityType(currentType)
    } else if (shouldRedirect) {
        startActivity(
            Intent(intent)
                .setClass(this, selectedType.activityClass)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
    return shouldRedirect
}

private val DemoActivityType.activityClass: Class<out Activity>
    get() = when (this) {
        DemoActivityType.COMPONENT_ACTIVITY -> MainActivity::class.java
        DemoActivityType.PLAIN_ACTIVITY -> CustomTabActivity::class.java
    }

private fun Context.saveDemoActivityType(activityType: DemoActivityType) {
    getSharedPreferences(ACTIVITY_TYPE_PREFERENCES, Context.MODE_PRIVATE).edit {
        putString(ACTIVITY_TYPE_KEY, activityType.name)
    }
}

private fun Context.getSavedDemoActivityType(): DemoActivityType {
    val savedName = getSharedPreferences(ACTIVITY_TYPE_PREFERENCES, Context.MODE_PRIVATE)
        .getString(ACTIVITY_TYPE_KEY, null)
    return savedName
        ?.let { name -> runCatching { DemoActivityType.valueOf(name) }.getOrNull() }
        ?: DemoActivityType.COMPONENT_ACTIVITY
}

private const val ACTIVITY_TYPE_PREFERENCES = "demo_activity_type"
private const val ACTIVITY_TYPE_KEY = "selected_activity_type"
