package com.paypal.android.corepayments

import android.util.LruCache

class ClientIdRepository internal constructor(
    private val configuration: CoreConfig,
    private val secureTokenServiceAPI: SecureTokenServiceAPI
) {
    constructor(configuration: CoreConfig) :
            this(configuration, SecureTokenServiceAPI(configuration))

    /**
     * Retrieves the merchant's clientID either from the local cache, or via an HTTP request if not cached.
     * @return Merchant clientID.
     */
    suspend fun fetchClientId(): String {
        clientIDCache.get(configuration.accessToken)?.let { cachedClientID ->
            return cachedClientID
        }
        val clientID = secureTokenServiceAPI.getClientId()
        clientIDCache.put(configuration.accessToken, clientID)
        return clientID
    }

    companion object {
        val clientIDCache = LruCache<String, String>(10)
    }
}
