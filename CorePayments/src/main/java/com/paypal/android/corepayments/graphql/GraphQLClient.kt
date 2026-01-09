package com.paypal.android.corepayments.graphql

import androidx.annotation.RestrictTo
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.APIClientError.graphQLJSONParseError
import com.paypal.android.corepayments.APIClientError.invalidUrlRequest
import com.paypal.android.corepayments.APIClientError.noResponseData
import com.paypal.android.corepayments.APIClientError.unknownError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Http
import com.paypal.android.corepayments.HttpMethod
import com.paypal.android.corepayments.HttpRequest
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class GraphQLClient internal constructor(
    coreConfig: CoreConfig,
    private val http: Http = Http(),
) {
    companion object {
        const val PAYPAL_DEBUG_ID = "Paypal-Debug-Id"
    }

    constructor(coreConfig: CoreConfig) : this(coreConfig, Http())

    private val json = Json { ignoreUnknownKeys = true }

    private val graphQLEndpoint = coreConfig.environment.graphQLEndpoint
    private val graphQLURL = "$graphQLEndpoint/graphql"

    private val httpRequestHeaders = mutableMapOf(
        "Content-Type" to "application/json",
        "Accept" to "application/json",
        "x-app-name" to "nativecheckout",
        "Origin" to coreConfig.environment.graphQLEndpoint
    )

    /**
     * Implementation for sending GraphQL requests.
     * Marked with @PublishedApi so it can be accessed from the inline public function.
     */
    @OptIn(InternalSerializationApi::class)
    @PublishedApi
    internal suspend fun <R, V> sendInternal(
        graphQLRequest: GraphQLRequest<V>,
        additionalHeaders: Map<String, String>?,
        variablesSerializer: KSerializer<V>,
        responseSerializer: KSerializer<R>,
    ): GraphQLResult<R> {
        val httpRequest = createHttpRequest(graphQLRequest, variablesSerializer, additionalHeaders)
            ?: return GraphQLResult.Failure(error = invalidUrlRequest)

        val httpResponse = http.send(httpRequest)
        val correlationId = httpResponse.headers[PAYPAL_DEBUG_ID]

        return when {
            httpResponse.status != HTTP_OK -> {
                GraphQLResult.Failure(APIClientError.serverResponseError(correlationId))
            }

            httpResponse.body.isNullOrBlank() -> {
                GraphQLResult.Failure(noResponseData(correlationId))
            }

            else -> runCatching {
                val response = json.decodeFromString(
                    deserializer = GraphQLResponse.serializer(responseSerializer),
                    string = httpResponse.body
                )
                GraphQLResult.Success(response, correlationId = correlationId)
            }.getOrElse { error ->
                GraphQLResult.Failure(graphQLJSONParseError(correlationId, error))
            }
        }
    }

    private fun <V> createHttpRequest(
        graphQLRequest: GraphQLRequest<V>,
        variablesSerializer: KSerializer<V>,
        additionalHeaders: Map<String, String>?
    ): HttpRequest? = runCatching {
        val urlString = graphQLRequest.operationName?.let { "$graphQLURL?$it" } ?: graphQLURL
        val requestBody =
            json.encodeToString(GraphQLRequest.serializer(variablesSerializer), graphQLRequest)

        val combinedHeaders = httpRequestHeaders + (additionalHeaders ?: emptyMap())
        HttpRequest(
            url = URL(urlString),
            method = HttpMethod.POST,
            body = requestBody,
            headers = combinedHeaders.toMutableMap()
        )
    }.getOrNull()

    /**
     * Public API for sending GraphQL requests.
     * Uses reified type parameters for automatic serializer selection.
     */
    @OptIn(InternalSerializationApi::class)
    suspend inline fun <reified R, reified V> send(
        graphQLRequest: GraphQLRequest<V>,
        additionalHeaders: Map<String, String>? = null
    ): GraphQLResult<R> = runCatching {
        sendInternal(graphQLRequest, additionalHeaders, serializer<V>(), serializer<R>())
    }.getOrElse { throwable ->
        val error = when (throwable) {
            is SerializationException -> invalidUrlRequest
            else -> unknownError()
        }
        GraphQLResult.Failure(error)
    }
}
