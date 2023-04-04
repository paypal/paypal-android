package com.paypal.android.corepayments.api

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.APIRequest
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpResponse
import com.paypal.android.corepayments.PayPalGraphQLClient
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.corepayments.R
import com.paypal.android.corepayments.SecureTokenServiceAPI
import com.paypal.android.corepayments.api.models.Eligibility
import com.paypal.android.corepayments.graphql.common.GraphQLClientImpl
import com.paypal.android.corepayments.graphql.fundingEligibility.FundingEligibilityQuery
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibilityIntent
import com.paypal.android.corepayments.graphql.fundingEligibility.models.FundingEligibilityResponse
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedCountryCurrencyType
import com.paypal.android.corepayments.graphql.fundingEligibility.models.SupportedPaymentMethodsType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection


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
        val json = JSONObject()

        val fundingEligibilityQuery = readRawResource(R.raw.graphql_query_funding_eligibility)
        json.put("query", fundingEligibilityQuery)

        val variables = JSONObject()
            .put("clientId", clientId)
            .put("intent", FundingEligibilityIntent.CAPTURE)
            .put("currency", SupportedCountryCurrencyType.USD)
            .put("enableFunding", listOf(SupportedPaymentMethodsType.VENMO))
        json.put("variables", variables)

        val apiRequest = APIRequest("/graphql", body = json.toString())
        val response = graphQLClient.send(apiRequest)
        val correlationID: String? = response.headers[GraphQLClientImpl.PAYPAL_DEBUG_ID]

        return if (response.status == HttpURLConnection.HTTP_OK) {
            val responseBody = JSONObject(response.body ?: "{}")
            val data = responseBody.getJSONObject("data")
            val graphQLResponse = FundingEligibilityResponse(data)
            Eligibility(
                isCreditCardEligible = graphQLResponse.fundingEligibility.card.eligible,
                isPayLaterEligible = graphQLResponse.fundingEligibility.payLater.eligible,
                isPaypalCreditEligible = graphQLResponse.fundingEligibility.credit.eligible,
                isPaypalEligible = graphQLResponse.fundingEligibility.paypal.eligible,
                isVenmoEligible = graphQLResponse.fundingEligibility.venmo.eligible,
            )
        } else {
            // TODO: propagate errors
            throw PayPalSDKError(
                0,
                "Error in checking eligibility",
//                "Error in checking eligibility: ${response.errors}",
                correlationID
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

    /**
     *  Checks if merchant is eligible for a set of payment methods
     *  @return [Eligibility] for payment methods
     *  @throws PayPalSDKError if something went wrong in the API call
     */
    suspend fun checkEligibility(): Eligibility {
        val fundingEligibilityQuery = FundingEligibilityQuery(
            clientId = api.fetchCachedOrRemoteClientID(),
            fundingEligibilityIntent = FundingEligibilityIntent.CAPTURE,
            currencyCode = SupportedCountryCurrencyType.USD,
            enableFunding = listOf(SupportedPaymentMethodsType.VENMO)
        )
        val response = graphQLClient.executeQuery(fundingEligibilityQuery)
        return if (response.data != null) {
            Eligibility(
                isCreditCardEligible = response.data.fundingEligibility.card.eligible,
                isPayLaterEligible = response.data.fundingEligibility.payLater.eligible,
                isPaypalCreditEligible = response.data.fundingEligibility.credit.eligible,
                isPaypalEligible = response.data.fundingEligibility.paypal.eligible,
                isVenmoEligible = response.data.fundingEligibility.venmo.eligible,
            )
        } else {
            throw PayPalSDKError(
                0,
                "Error in checking eligibility: ${response.errors}",
                response.correlationId
            )
        }
    }

    companion object {
        const val TAG = "Eligibility API"
    }
}
