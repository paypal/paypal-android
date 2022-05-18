package com.paypal.android.core.graphql.fundingEligibility

enum class SupportedPaymentMethodsType {
    VENMO, CREDIT, PAYLATER
}

enum class Intent {
    SALE, CAPTURE, AUTHORIZE, ORDER, TOKENIZE, SUBSCRIPTION
}

enum class SupportedCountryCurrencyType {
    USD
}
