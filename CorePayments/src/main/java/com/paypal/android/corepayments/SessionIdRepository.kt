package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * Holds a [sessionId] that is generated once per app start and reused for the lifetime of the
 * process. The value is used as the `contextId` for the
 * `createShopperSessionWithAppSwitchEligibility` call so that events belonging to the same app
 * session can be tied together.
 *
 * The id is an in-memory, immutable value backed by a lazily-created process-wide
 * singleton ([instance]).
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SessionIdRepository(
    uuidHelper: UUIDHelper = UUIDHelper()
) {

    /**
     * Unique identifier generated once per app start. Held in memory only (not persisted), so a
     * new value is created the next time the process starts.
     */
    val sessionId: String = uuidHelper.formattedUUID

    companion object {

        /**
         * Process-wide singleton. Created lazily on first access, which fixes the [sessionId]
         * value for the remainder of the app's lifetime.
         */
        val instance: SessionIdRepository by lazy { SessionIdRepository() }
    }
}
