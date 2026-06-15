package com.paypal.android.corepayments.analytics

import androidx.annotation.RestrictTo
import java.util.UUID

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ButtonSessionStore {

    @Volatile
    var buttonSessionId: String = UUID.randomUUID().toString()
        private set

    fun resetSession() {
        buttonSessionId = UUID.randomUUID().toString()
    }
}
