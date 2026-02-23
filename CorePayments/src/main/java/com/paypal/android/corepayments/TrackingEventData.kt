package com.paypal.android.corepayments

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data classes for Kotlin serialization of tracking events REST API request
 */
@InternalSerializationApi
@Serializable
internal data class TrackingEventRequest(
    val events: TrackingEvents
)

@InternalSerializationApi
@Serializable
internal data class TrackingEvents(
    @SerialName("event_params")
    val eventParams: TrackingEventParams
)

@InternalSerializationApi
@Serializable
internal data class TrackingEventParams(
    @SerialName("app_id")
    val appId: String,
    @SerialName("app_name")
    val appName: String,
    @SerialName("partner_client_id")
    val clientId: String,
    @SerialName("c_sdk_ver")
    val clientSDKVersion: String,
    @SerialName("client_os")
    val clientOS: String,
    @SerialName("comp")
    val component: String,
    @SerialName("device_manufacturer")
    val deviceManufacturer: String,
    @SerialName("mobile_device_model")
    val deviceModel: String,
    @SerialName("merchant_sdk_env")
    val environment: String,
    @SerialName("event_name")
    val eventName: String,
    @SerialName("event_source")
    val eventSource: String,
    @SerialName("is_simulator")
    val isSimulator: Boolean,
    @SerialName("mapv")
    val merchantAppVersion: String?,
    @SerialName("platform")
    val platform: String,
    @SerialName("t")
    val timestamp: String,
    @SerialName("tenant_name")
    val tenantName: String,
    @SerialName("order_id")
    val orderId: String? = null,
    @SerialName("button_type")
    val buttonType: String? = null,
    @SerialName("app_switch_enabled")
    val appSwitchEnabled: Boolean = false
)
