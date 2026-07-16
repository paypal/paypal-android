package com.paypal.android.corepayments

import androidx.annotation.RestrictTo
import java.util.UUID

/**
 * Helper for generating unique identifiers used across the SDK.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class UUIDHelper {

    /**
     * A random UUID with the dashes stripped, e.g. a 32-character lowercase hex string.
     */
    val formattedUUID: String
        // Strip dashes so the value can be used directly as a contextId/session identifier.
        get() = UUID.randomUUID().toString().replace("-", "")
}
