package com.paypal.android.paypalwebpayments

import android.net.Uri
import androidx.annotation.NonNull
import org.json.JSONObject

internal class PayPalDeepLinkUrlResult constructor(
    @NonNull uri: Uri,
    @NonNull metadata: JSONObject
) {
    val payerId: String? = uri.getQueryParameter("PayerID")
    val intent: String? = uri.getQueryParameter("intent")
    val opType: String? = uri.getQueryParameter("opType")
    val token: String? = uri.getQueryParameter("token")
    val orderId: String? = metadata.getString("order_id")
}
