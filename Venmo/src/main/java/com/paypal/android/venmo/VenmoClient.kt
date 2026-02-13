package com.paypal.android.venmo

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.UpdateClientConfigAPI
import com.paypal.android.corepayments.UpdateClientConfigResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VenmoClient(
    private val ccoAPI: UpdateClientConfigAPI,
    private val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {

    constructor(context: Context, config: CoreConfig) : this(UpdateClientConfigAPI(context, config))

    fun startVenmo(activity: ComponentActivity, orderId: String) {
        applicationScope.launch {
            // we don't seem to need the result
            val ccoUpdateResult = ccoAPI.updateClientConfig(tokenId = orderId, fundingSource = "venmo")
            when (ccoUpdateResult) {
                UpdateClientConfigResult.Success -> {
                    Log.d("venmo", "CCO Update Success")
                }
                is UpdateClientConfigResult.Failure -> {
                    Log.d("venmo", "CCO Update Failure")
                }
            }

            // FROM: VenmoAppSwitch
            val localVenmoBaseUrl = "https://www.paypal.com/smart/checkout/venmo"
            val sandboxVenmoBaseUrl = "https://www.sandbox.paypal.com/smart/checkout/venmo"
            val appSwitchUri = localVenmoBaseUrl.toUri()
                .buildUpon()
                .appendQueryParameter("buyerCountry", "US")
                .appendQueryParameter("channel", "mobile-web")
                .appendQueryParameter("enableFunding", "venmo")
                .appendQueryParameter("env", "sandbox")
                .appendQueryParameter("facilitatorAccessToken", "")
                .appendQueryParameter("fundingSource", "venmo")
                .appendQueryParameter("orderId", orderId)
                .build()
            activity.startActivity(Intent(Intent.ACTION_VIEW, appSwitchUri))

            // FROM: VenmoWebProductFlow.ts (Sandbox)

            // FROM: VenmoAppSwitchProductFlow.ts (Sandbox)

        }
    }
}