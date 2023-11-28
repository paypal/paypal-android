package com.paypal.android.paypalwebpayments

import android.net.Uri
import org.json.JSONObject

// Setup Token Approval URL: com.paypal.android.demo://example.com/return_url?approval_token_id=1JH795071P291053A&approval_session_id=1JH795071P291053A

internal class PayPalDeepLinkUrlResult constructor(uri: Uri, metadata: JSONObject) {
    val payerId: String? = uri.getQueryParameter("PayerID")
    val intent: String? = uri.getQueryParameter("intent")
    val opType: String? = uri.getQueryParameter("opType")
    val token: String? = uri.getQueryParameter("token")
    val orderId: String? = metadata.getString("order_id")
}
