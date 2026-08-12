package com.paypal.android.corepayments.chromecustomtabs

import android.content.Context
import android.net.Uri
import android.os.RemoteException
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.customtabs.EngagementSignalsCallback

/** Defines the Custom Tabs operations required to launch tabs and track browser sessions. */
internal interface CustomTabsAdapter {
    fun getPackageName(context: Context): String?
    fun bind(context: Context, packageName: String, connection: CustomTabsServiceConnection): Boolean
    fun unbind(context: Context, connection: CustomTabsServiceConnection)
    fun newSession(client: CustomTabsClient, callback: CustomTabsCallback): CustomTabsSession?
    @Throws(RemoteException::class, UnsupportedOperationException::class)
    fun isEngagementSignalsApiAvailable(session: CustomTabsSession): Boolean
    @Throws(RemoteException::class, UnsupportedOperationException::class)
    fun setEngagementSignalsCallback(session: CustomTabsSession, callback: EngagementSignalsCallback): Boolean
    fun launch(context: Context, uri: Uri, session: CustomTabsSession?, packageName: String?)
}
