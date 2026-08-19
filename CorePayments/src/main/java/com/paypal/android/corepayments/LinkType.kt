package com.paypal.android.corepayments

import androidx.annotation.RestrictTo

/**
 * The return-link type reported to analytics via the `link_type` param.
 *
 * Note: exposed for internal PayPal SDK use only. Not covered by Semantic Versioning.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class LinkType(val analyticsValue: String) {
    APP_LINK("universal"),
    DEEP_LINK("deeplink"),
}
