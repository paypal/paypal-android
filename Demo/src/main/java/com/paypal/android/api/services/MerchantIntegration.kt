package com.paypal.android.api.services

enum class MerchantIntegration(val baseUrl: String, val clientId: String) {
//    DEFAULT(
//        baseUrl = "https://ppcp-mobile-demo-sandbox-87bbd7f0a27f.herokuapp.com/",
//        clientId = "AQTfw2irFfemo-eWG4H5UY-b9auKihUpXQ2Engl4G1EsHJe2mkpfUv_SN3Mba0v3CfrL6Fk_ecwv9EOo"
//    ),
    LIVE(
        baseUrl = "https://ppcp-mobile-demo-live-f5eec0f7494a.herokuapp.com/",
        clientId = "AXmZuly86V1E8kuviT7PqQk78tOS1yf9b8GkoIk8hniqrzukz9Zsk9B8oNzpwTZsK-z7rK5OcWRgM-oe"
    ),
}
