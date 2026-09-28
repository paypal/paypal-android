package com.paypal.android.corepayments.browserswitch

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class LaunchAuthTabResult {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object Success : LaunchAuthTabResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data object ActivityNotFound : LaunchAuthTabResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class Failure(val error: Exception) : LaunchAuthTabResult()
}
