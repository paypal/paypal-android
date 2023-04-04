package com.paypal.android.corepayments.api

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalGraphQLClient
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.SecureTokenServiceAPI
import com.paypal.android.corepayments.api.models.Eligibility
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibility
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedPaymentMethodsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


/**
 *  API that checks merchants eligibility for different payment methods.
 */
internal class EligibilityAPI internal constructor(
//    private val api: API,
    private val applicationContext: Context,
    private val secureTokenServiceAPI: SecureTokenServiceAPI,
    private val graphQLClient: PayPalGraphQLClient,
//    private val graphQLClient: GraphQLClient
) {

    /**
     *  EligibilityAPI constructor
     *  @param coreConfig configuration parameters for eligibility API
     */
    constructor(coreConfig: CoreConfig, context: Context) : this(
        context.applicationContext,
        SecureTokenServiceAPI(coreConfig),
        PayPalGraphQLClient(coreConfig),
    )

    suspend fun checkEligibility2(): Eligibility {
        val clientId = secureTokenServiceAPI.fetchCachedOrRemoteClientID()

        // Ref: https://www.apollographql.com/docs/react/data/operation-best-practices/#use-graphql-variables-to-provide-arguments
        val graphQLRequestJSON = JSONObject()

        val fundingEligibilityQuery = readRawResource(R.raw.graphql_query_funding_eligibility)
        graphQLRequestJSON.put("query", fundingEligibilityQuery)

        val variables = JSONObject()
            .put("clientId", clientId)
            .put("intent", FundingEligibilityIntent.CAPTURE)
            .put("currency", SupportedCountryCurrencyType.USD)
            .put("enableFunding", listOf(SupportedPaymentMethodsType.VENMO))
        graphQLRequestJSON.put("variables", variables)

        val apiRequest = APIRequest("/graphql", body = graphQLRequestJSON.toString())
        val graphQLResponse = graphQLClient.send(apiRequest)
        return if (graphQLResponse.isSuccessful) {
            val fundingEligibility =
                FundingEligibility(graphQLResponse.data.getJSONObject("fundingEligibility"))
            Eligibility(
                isCreditCardEligible = fundingEligibility.card.eligible,
                isPayLaterEligible = fundingEligibility.payLater.eligible,
                isPaypalCreditEligible = fundingEligibility.credit.eligible,
                isPaypalEligible = fundingEligibility.paypal.eligible,
                isVenmoEligible = fundingEligibility.venmo.eligible,
            )
        } else {
            throw PayPalSDKError(
                0,
                "Error in checking eligibility: ${graphQLResponse.errors}",
                graphQLResponse.correlationID
            )
        }
    }

    private suspend fun readRawResource(@RawRes resId: Int): String = withContext(Dispatchers.IO) {
        try {
            val resInputStream = applicationContext.resources.openRawResource(resId)
            val resAsBytes = ByteArray(resInputStream.available())
            resInputStream.read(resAsBytes)
            String(resAsBytes)
        } catch (e: Exception) {
            throw Exception("TODO: throw SDK typed error", e)
        }
    }

    companion object {
        const val TAG = "Eligibility API"
    }
}
