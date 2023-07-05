package com.paypal.android.api.services

enum class Merchant(val baseUrl: String) {
    DEFAULT("https://sdk-sample-merchant-server.herokuapp.com/"),
    DIRECT("https://sdk-sample-merchant-server.herokuapp.com/direct/"),
    CONNECTED_PATH("https://sdk-sample-merchant-server.herokuapp.com/connected_path/"),
    MANAGED_PATH("https://sdk-sample-merchant-server.herokuapp.com/managed_path/"),
}
