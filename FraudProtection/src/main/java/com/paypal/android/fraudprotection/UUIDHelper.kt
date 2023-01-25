package com.paypal.android.fraudprotection

import android.content.Context
import java.util.UUID

internal class UUIDHelper {

    companion object {
        private const val INSTALL_GUID: String = "InstallationGUID"
    }

    fun getInstallationGUID(context: Context): String {
        val existingGUID = SharedPreferenceUtils.instance.getString(context, INSTALL_GUID, null)
        return if (existingGUID != null) {
            existingGUID
        } else {
            val newGuid = UUID.randomUUID().toString()
            SharedPreferenceUtils.instance.putString(context, INSTALL_GUID, newGuid)
            newGuid
        }
    }
}
