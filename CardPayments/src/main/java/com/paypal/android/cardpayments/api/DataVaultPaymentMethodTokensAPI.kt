package com.paypal.android.cardpayments.api

import com.paypal.android.cardpayments.VaultRequest
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.PaymentsJSON
import com.paypal.android.corepayments.RestClient
import org.json.JSONObject

internal class DataVaultPaymentMethodTokensAPI(
    private val restClient: RestClient
) {
    constructor(coreConfig: CoreConfig) : this(RestClient(coreConfig))

    suspend fun createSetupToken(vaultRequest: VaultRequest): VaultResult {
        return VaultResult("fake-status", "fake-setup-token-id")
    }
}
