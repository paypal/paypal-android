package com.paypal.android.corepayments.usecase

import android.net.Uri
import androidx.annotation.RestrictTo

/**
 * Checks whether the device's default handler for [browserUri] is a browser known to honor Android
 * App Links, based on a static list of pre-tested browsers.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HasAppLinksCompatibleBrowserUseCase(
    private val getDefaultAppUseCase: GetDefaultAppUseCase,
) {

    operator fun invoke(browserUri: Uri?): Boolean {
        val defaultApp = getDefaultAppUseCase(browserUri) ?: return false
        return APP_LINK_COMPATIBLE_BROWSERS.any { defaultApp.contains(it) }
    }

    private companion object {
        private val APP_LINK_COMPATIBLE_BROWSERS = listOf(
            "com.android.chrome",
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
        )
    }
}
