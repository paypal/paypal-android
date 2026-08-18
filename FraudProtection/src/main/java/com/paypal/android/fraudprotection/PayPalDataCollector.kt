package com.paypal.android.fraudprotection

import android.content.Context
import android.util.Log
import com.paypal.android.corepayments.CoreConfig
import lib.android.paypal.com.magnessdk.InvalidInputException
import lib.android.paypal.com.magnessdk.MagnesSDK
import lib.android.paypal.com.magnessdk.MagnesSettings
import lib.android.paypal.com.magnessdk.MagnesSource

/**
 * Enables you to collect data about a customer's device and correlate it with a session identifier on your server.
 */
class PayPalDataCollector internal constructor(
    coreConfig: CoreConfig,
    private val magnesSDK: MagnesSDK,
    private val uuidHelper: UUIDHelper
) {

    private val environment = coreConfig.magnesEnvironment

    constructor(config: CoreConfig) : this(config, MagnesSDK.getInstance(), UUIDHelper())

    /**
     * Use to collects device data at the time of payment. Once a user initiates a payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context Android Context
     * @param request Request object containing parameters to configure data collection
     */
    fun collectDeviceData(context: Context, request: PayPalDataCollectorRequest): String {
        val appContext = context.applicationContext
        return try {
            val magnesSettingsBuilder = MagnesSettings.Builder(appContext)
                .setMagnesSource(MagnesSource.PAYPAL)
                .disableBeacon(false)
                .setMagnesEnvironment(environment)
                .setAppGuid(uuidHelper.getInstallationGUID(context))
                .setHasUserLocationConsent(request.hasUserLocationConsent)
            magnesSDK.setUp(magnesSettingsBuilder.build())
            val result = magnesSDK.collectAndSubmit(
                appContext,
                request.clientMetadataId,
                HashMap(request.additionalData ?: emptyMap())
            )
            result.paypalClientMetaDataId
        } catch (e: InvalidInputException) {
            // Either clientMetadataId or appGuid exceeds their character limit
            Log.e(
                "Exception",
                "Error fetching client metadata ID",
                e
            )
            ""
        }
    }
}
