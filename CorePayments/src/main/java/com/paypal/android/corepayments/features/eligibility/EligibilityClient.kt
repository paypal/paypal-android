package com.paypal.android.corepayments.features.eligibility

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.apis.eligibility.EligibilityAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EligibilityClient internal constructor(
    private val applicationContext: Context,
    private val eligibilityAPI: EligibilityAPI,
    private val dispatcher: CoroutineDispatcher
) {

    constructor(context: Context, coreConfig: CoreConfig) : this(
        context.applicationContext,
        EligibilityAPI(coreConfig),
        Dispatchers.Main
    )

    fun check(
        eligibilityRequest: EligibilityRequest,
        callback: CheckEligibilityResult
    ) {
        CoroutineScope(dispatcher).launch {
            try {
                val response =
                    eligibilityAPI.checkEligibility(applicationContext, eligibilityRequest)
                callback.onCheckEligibilitySuccess(EligibilityResult(response.isVenmoEligible))
            } catch (e: PayPalSDKError) {
                callback.onCheckEligibilityFailure(e)
            }
        }
    }
}