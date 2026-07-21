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
    val appSwitchEnabled: Boolean? = null,
    @SerialName("shopper_session_id")
    val shopperSessionId: String? = null,
    @SerialName("shopper_session_expiration_at")
    val shopperSessionExpirationAt: String? = null,
    @SerialName("matched_authentication_methods")
    val matchedAuthenticationMethods: List<String>? = null,
    @SerialName("app_switch_url")
    val appSwitchUrl: String? = null,
    @SerialName("checkout_fallback_url")
    val fallbackUrl: String? = null,
    @SerialName("error_description")
    val errorDescription: String? = null,
    @SerialName("is_cached_session")
    val isCachedSession: Boolean? = null,
    @SerialName("is_vault")
    val isVault: Boolean? = null,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("endpoint")
    val endpoint: String? = null,
    @SerialName("presentation_type")
    val presentationType: String? = null,
    @SerialName("flow")
    val flow: String? = null,
    @SerialName("app_switch_eligible")
    val appSwitchEligible: Boolean? = null,
    @SerialName("ineligible_reason")
    val ineligibleReason: String? = null,
    @SerialName("merchant_id")
    val merchantId: String? = null,
    @SerialName("bn_code")
    val bnCode: String? = null,
    @SerialName("client_id")
    val eventClientId: String? = null,
    @SerialName("user_action")
    val userAction: String? = null,
    @SerialName("paypal_installed")
    val paypalInstalled: String? = null,
    @SerialName("return_app_url")
    val returnAppUrl: String? = null,
    @SerialName("cancel_app_url")
    val cancelAppUrl: String? = null,
    @SerialName("fallback_scheme_url")
    val fallbackSchemeUrl: String? = null,
    @SerialName("link_type")
    val linkType: String? = null
)
