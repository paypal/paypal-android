package com.paypal.android.api.services

enum class MerchantIntegration(val baseUrl: String, val clientId: String) {
    DEFAULT(
        baseUrl = "https://ppcp-mobile-demo-sandbox-87bbd7f0a27f.herokuapp.com/",
//        baseUrl = "http://10.0.2.2:8080/",
        clientId = "AQTfw2irFfemo-eWG4H5UY-b9auKihUpXQ2Engl4G1EsHJe2mkpfUv_SN3Mba0v3CfrL6Fk_ecwv9EOo"
    ),
}
