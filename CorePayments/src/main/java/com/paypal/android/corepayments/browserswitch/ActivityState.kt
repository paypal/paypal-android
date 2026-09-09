package com.paypal.android.corepayments.browserswitch

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

internal data class ActivityState(
    var pendingRequest: PendingRequest?,
    var launcher: ActivityResultLauncher<Intent>? = null,
)
