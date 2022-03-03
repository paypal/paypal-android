package com.paypal.android.paypaldatacollector

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import lib.android.paypal.com.magnessdk.Environment
import lib.android.paypal.com.magnessdk.InvalidInputException
import lib.android.paypal.com.magnessdk.MagnesSDK
import lib.android.paypal.com.magnessdk.MagnesSettings
import lib.android.paypal.com.magnessdk.MagnesSource

class PayPalDataCollector {

    private val magnesSDK = MagnesSDK.getInstance()
    private val uuidHelper = UUIDHelper()

    fun getClientMetadataId(context: Context) : String {
        val request = PayPalDataCollectorRequest(uuidHelper.getInstallationGUID(context))
        return getClientMetadataId(context, request)
    }

    @SuppressLint("Range")
    fun getClientMetadataId(context: Context, request: PayPalDataCollectorRequest) : String {
        val appContext = context.applicationContext
        return try {
            val magnesSettingsBuilder = MagnesSettings.Builder(appContext)
                .setMagnesSource(MagnesSource.PAYPAL)
                .disableBeacon(request.disableBeacon)
                .setMagnesEnvironment(Environment.LIVE) // check this
                .setAppGuid(request.applicationGuid)
            magnesSDK.setUp(magnesSettingsBuilder.build())
            val result = magnesSDK.collectAndSubmit(
                appContext,
                request.clientMetadataId,
                request.additionalData
            )
            result.paypalClientMetaDataId
        } catch (e: InvalidInputException) {
            // Either clientMetadataId or appGuid exceeds their character limit
            Log.e(
                "Exception",
                "Error fetching client metadata ID. Contact Braintree Support for assistance.",
                e
            )
            ""
        }
    }
}