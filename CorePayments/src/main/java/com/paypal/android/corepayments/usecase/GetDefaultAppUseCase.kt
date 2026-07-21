package com.paypal.android.corepayments.usecase

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.RestrictTo

/**
 * Returns the package name of the default application that handles [uri], or `null` if none can be
 * resolved.
 *
 * Mirrors braintree_android's `GetDefaultAppUseCase`.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GetDefaultAppUseCase(private val packageManager: PackageManager) {

    operator fun invoke(uri: Uri?): String? {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
}
