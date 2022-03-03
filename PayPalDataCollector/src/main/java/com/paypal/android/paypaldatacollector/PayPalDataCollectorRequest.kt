package com.paypal.android.paypaldatacollector

class PayPalDataCollectorRequest @JvmOverloads constructor(
    var applicationGuid: String = "",
    var clientMetadataId: String = "",
    var disableBeacon: Boolean = false,
    var additionalData: HashMap<String, String> = HashMap()
)