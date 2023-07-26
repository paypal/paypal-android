package com.paypal.android.api.services

enum class MerchantIntegration(val clientId: String, val baseUrl: String) {
    DEFAULT(
        clientId = "AcXwOk3dof7NCNcriyS8RVh5q39ozvdWUF9oHPrWqfyrDS4AwVdKe32Axuk2ADo6rI_31Vv6MGgOyzRt",
        baseUrl = "https://sdk-sample-merchant-server.herokuapp.com/"
    ),
    DIRECT(
        clientId = "AVhcAP8TDu5PFeAw97M8187g-iYQW8W0AhvvXaMaWPojJRGGkunX8r-fyPkKGCv09P83KC2dijKLKwyz",
        baseUrl = "https://sdk-sample-merchant-server.herokuapp.com/direct/"
    ),
    CONNECTED_PATH(
        clientId = "AcvkeOozOElJtQoZxxdAsDUrsClbAiNv7KIW6675dAiC7EX5R0wvSPiUNCc2JPEKHyFPfegwh_OV2afV",
        baseUrl = "https://sdk-sample-merchant-server.herokuapp.com/connected_path/"
    ),
    MANAGED_PATH(
        clientId = "Afba8WbtSOqWlMoxroE5Ym8CVnogJJcHpj2uFpPYzN7oJz8NOi9XRrmHmpbWVQm6vRX0SwCvabaNKo06",
        baseUrl = "https://sdk-sample-merchant-server.herokuapp.com/managed_path/"
    ),
}
