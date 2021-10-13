package com.paypal.android.checkout

/**
 * Currency Code provides the set of currency codes supported by PayPal.
 *
 * @see [Currency Codes](https://developer.paypal.com/docs/api/reference/currency-codes/)
 */
enum class CurrencyCode {
    /**
     * Currency Code for: Australian dollar
     */
    AUD,

    /**
     * Currency Code for: Brazilian real
     *
     * Note: This currency is supported as a payment currency and a currency balance for in-country
     * PayPal accounts only. If the receiver of funds is not from Brazil, then PayPal converts funds
     * into the primary holding currency of the account with the applicable currency conversion
     * rate. The currency conversion rate includes PayPal's applicable spread or fee.
     */
    BRL,

    /**
     * Currency Code for: Canadian dollar
     */
    CAD,

    /**
     * Currency Code for: Chinese Renmenbi
     *
     * Note: This currency is supported as a payment currency and a currency balance for in-country
     * PayPal accounts only.
     */
    CNY,

    /**
     * Currency Code for: Czech koruna
     */
    CZK,

    /**
     * Currency Code for: Danish krone
     */
    DKK,

    /**
     * Currency Code for: Euro
     */
    EUR,

    /**
     * Currency Code for: Hong Kong dollar
     */
    HKD,

    /**
     * Currency Code for: Hungarian forint
     *
     * Note: This currency does not support decimals. If you pass a decimal amount, an error occurs.
     */
    HUF,

    /**
     * Currency Code for: Indian rupee
     *
     * Note: This currency is supported as a payment currency and a currency balance for in-country
     * PayPal India accounts only.
     */
    INR,

    /**
     * Currency Code for: Israeli new shekel
     */
    ILS,

    /**
     * Currency Code for: Japanese yen
     *
     * Note: This currency does not support decimals. If you pass a decimal amount, an error occurs.
     */
    JPY,

    /**
     * Currency Code for: Malaysian ringgit
     *
     * Note: This currency is supported as a payment currency and a currency balance for in-country
     * PayPal accounts only.
     */
    MYR,

    /**
     * Currency Code for: Mexican peso
     */
    MXN,

    /**
     * Currency Code for: New Taiwan dollar
     *
     * Note: This currency does not support decimals. If you pass a decimal amount, an error occurs.
     */
    TWD,

    /**
     * Currency Code for: New Zealand dollar
     */
    NZD,

    /**
     * Currency Code for: Norwegian krone
     */
    NOK,

    /**
     * Currency Code for: Philippine peso
     */
    PHP,

    /**
     * Currency Code for: Polish złoty
     */
    PLN,

    /**
     * Currency Code for: Pound Sterling
     */
    GBP,

    /**
     * Currency Code for: Russian ruble
     */
    RUB,

    /**
     * Currency Code for: Singapore dollar
     */
    SGD,

    /**
     * Currency Code for: Swedish krona
     */
    SEK,

    /**
     * Currency Code for: Swiss franc
     */
    CHF,

    /**
     * Currency Code for: Thai baht
     */
    THB,

    /**
     * Currency Code for: United States dollar
     */
    USD
}

internal val CurrencyCode.asNativeCheckout: com.paypal.checkout.createorder.CurrencyCode
    get() = enumValueOf(this.name)

internal val com.paypal.checkout.createorder.CurrencyCode.asPaypalCheckout: CurrencyCode
    get() = enumValueOf(this.name)