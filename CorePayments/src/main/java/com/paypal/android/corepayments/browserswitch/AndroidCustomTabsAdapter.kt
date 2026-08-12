package com.paypal.android.corepayments.browserswitch

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.customtabs.EngagementSignalsCallback

internal class AndroidCustomTabsAdapter : CustomTabsAdapter {
    override fun getPackageName(context: Context): String? = CustomTabsClient.getPackageName(context, null)

    override fun bind(
        context: Context,
        packageName: String,
        connection: CustomTabsServiceConnection
    ): Boolean = CustomTabsClient.bindCustomTabsServicePreservePriority(context, packageName, connection)

    override fun unbind(context: Context, connection: CustomTabsServiceConnection) {
        context.unbindService(connection)
    }

    override fun newSession(client: CustomTabsClient, callback: CustomTabsCallback): CustomTabsSession? =
        client.newSession(callback)

    override fun isEngagementSignalsApiAvailable(session: CustomTabsSession): Boolean =
        session.isEngagementSignalsApiAvailable(Bundle.EMPTY)

    @SuppressLint("RequiresFeature")
    override fun setEngagementSignalsCallback(
        session: CustomTabsSession,
        callback: EngagementSignalsCallback
    ): Boolean = session.setEngagementSignalsCallback(callback, Bundle.EMPTY)

    override fun launch(
        context: Context,
        uri: Uri,
        session: CustomTabsSession?,
        packageName: String?
    ) {
        val customTabsIntent = if (session == null) {
            CustomTabsIntent.Builder().build()
        } else {
            CustomTabsIntent.Builder(session).build()
        }
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.launchUrl(context, uri)
    }
}
