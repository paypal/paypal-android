package com.paypal.android.utils

import android.content.Intent
import androidx.core.util.Consumer

interface NewIntentListenerHost {
    fun addOnNewIntentListener(listener: Consumer<Intent>)
    fun removeOnNewIntentListener(listener: Consumer<Intent>)
}
