package com.paypal.android.ui.card

import android.content.Context
import com.paypal.android.paypaldatacollector.PayPalDataCollector
import com.paypal.android.paypaldatacollector.PayPalDataCollectorEnvironment
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class DataCollectorHandler @Inject constructor(@ApplicationContext val context: Context) {

    private val paypalDataCollector = PayPalDataCollector(PayPalDataCollectorEnvironment.SANDBOX)

    fun getClientMetadataId(clientMetadataId: String? = null, additionalData: HashMap<String, String>? = null): String =
        paypalDataCollector.getClientMetadataId(context, clientMetadataId, additionalData)

    fun setLogging(shouldLog: Boolean) { paypalDataCollector.setLogging(shouldLog) }
}
