package com.paypal.android.corepayments.features.eligibility

import android.content.Context
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.apis.eligibility.EligibilityAPI
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 *  Use EligibilityClient to check eligibility for different payment methods.
 */
class EligibilityClient internal constructor(
    private val applicationContext: Context,
    private val eligibilityAPI: EligibilityAPI,
    private val dispatcher: CoroutineDispatcher
) {

    /**
     * Build an EligibilityClient instance using configuration.
     *
     * @param context A reference to an Android context.
     * @param coreConfig The target configuration (see [CoreConfig]).
     */
    constructor(context: Context, coreConfig: CoreConfig) : this(
        context.applicationContext,
        EligibilityAPI(coreConfig),
        Dispatchers.Main
    )

    /**
     * Check eligibility for a set of payment methods.
     *
     * @param eligibilityRequest A request object (see [EligibilityRequest]).
     * @param callback A callback to receive a success or error result (see [EligibilityCheckListener]).
     */
    fun check(
        eligibilityRequest: EligibilityRequest,
        callback: EligibilityCheckListener
    ) {
        CoroutineScope(dispatcher).launch {
            try {
                val eligibility =
                    eligibilityAPI.checkEligibility(applicationContext, eligibilityRequest)
                val result = EligibilityResult(
                    isVenmoEligible = eligibility.isVenmoEligible,
                    isCardEligible = eligibility.isCreditCardEligible,
                    isPayPalEligible = eligibility.isPayPalEligible,
                    isPayLaterEligible = eligibility.isPayLaterEligible,
                    isCreditEligible = eligibility.isPayPalCreditEligible
                )
                callback.onCheckEligibilitySuccess(result)
            } catch (e: PayPalSDKError) {
                callback.onCheckEligibilityFailure(e)
            }
        }
    }
}
