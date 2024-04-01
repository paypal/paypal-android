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
     * Collects device data at the time of payment activity. Once a user initiates a payment
     * from their device, PayPal uses the Client Metadata ID to verify that the payment is
     * originating from a valid, user-consented device and application. This helps reduce fraud and
     * decrease declines. This method MUST be called prior to initiating a pre-consented payment (a
     * "future payment") from a mobile device. Pass the result to your server, to include in the
     * payment request sent to PayPal. Do not otherwise cache or store this value.
     *
     * @param context Android Context
     * @param clientMetadataId The desired data to pair to the request, trimmed to 32 characters.
     * @param additionalData Additional data that should be associated with the data collection
     *
     * @return clientMetadataId Your server will send this to PayPal
     */
    @Deprecated("This method is no longer supported.")
    @JvmOverloads
    fun collectDeviceData(
        context: Context,
        clientMetadataId: String? = null,
        additionalData: HashMap<String, String>? = null
    ): String {
        val request = PayPalDataCollectorRequest(
            hasUserLocationConsent = false,
            clientMetadataId = clientMetadataId,
            additionalData = additionalData
        )
        return collectDeviceData(context, request)
    }

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

    // NEXT MAJOR VERSION: consider removing this method; it has no merchant facing purpose
    fun setLogging(shouldLog: Boolean) {
        System.setProperty("magnes.debug.mode", shouldLog.toString())
    }
}
