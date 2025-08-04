package com.paypal.android.corepayments

data class SessionStore(private val properties: MutableMap<String, String> = mutableMapOf()) {

    fun get(key: String): String? {
        return properties[key]
    }

    fun put(key: String, value: String) {
        properties[key] = value
    }

    fun reset() {
        properties.clear()
    }
}
