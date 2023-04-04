package com.paypal.android.corepayments

import android.content.Context
import androidx.annotation.RawRes
import com.paypal.android.corepayments.graphql.common.GraphQLQueryResponse2
import com.paypal.android.corepayments.graphql.common.GraphQLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class PayPalGraphQLClient internal constructor(
    context: Context,
    private val coreConfig: CoreConfig,
    private val http: Http = Http()
) {
    private val applicationContext = context.applicationContext

    suspend fun send(request: GraphQLRequest): GraphQLQueryResponse2 {
        val httpRequest = mapGraphQLRequestToHttpRequest(request)
        val response = http.send(httpRequest)
        return GraphQLQueryResponse2(response)
    }

    private suspend fun mapGraphQLRequestToHttpRequest(graphQLRequest: GraphQLRequest): HttpRequest {
        val baseUrl = coreConfig.environment.grqphQlUrl
        val url = URL("$baseUrl/graphql")

        val graphQLRequestJSON = JSONObject()
            .put("query", readRawResource(graphQLRequest.queryResId))
            .put("variables", graphQLRequest.variables)
        val body = graphQLRequestJSON.toString()

        // default headers
        val headers: MutableMap<String, String> = mutableMapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json",
            "x-app-name" to "nativecheckout",
            "Origin" to coreConfig.environment.grqphQlUrl
        )

        return HttpRequest(url, HttpMethod.POST, body, headers)
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
}