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
    get() = when (this) {
        CurrencyCode.AUD -> com.paypal.checkout.createorder.CurrencyCode.AUD
        CurrencyCode.BRL -> com.paypal.checkout.createorder.CurrencyCode.BRL
        CurrencyCode.CAD -> com.paypal.checkout.createorder.CurrencyCode.CAD
        CurrencyCode.CNY -> com.paypal.checkout.createorder.CurrencyCode.CNY
        CurrencyCode.CZK -> com.paypal.checkout.createorder.CurrencyCode.CZK
        CurrencyCode.DKK -> com.paypal.checkout.createorder.CurrencyCode.DKK
        CurrencyCode.EUR -> com.paypal.checkout.createorder.CurrencyCode.EUR
        CurrencyCode.HKD -> com.paypal.checkout.createorder.CurrencyCode.HKD
        CurrencyCode.HUF -> com.paypal.checkout.createorder.CurrencyCode.HUF
        CurrencyCode.INR -> com.paypal.checkout.createorder.CurrencyCode.INR
        CurrencyCode.ILS -> com.paypal.checkout.createorder.CurrencyCode.ILS
        CurrencyCode.JPY -> com.paypal.checkout.createorder.CurrencyCode.JPY
        CurrencyCode.MYR -> com.paypal.checkout.createorder.CurrencyCode.MYR
        CurrencyCode.MXN -> com.paypal.checkout.createorder.CurrencyCode.MXN
        CurrencyCode.TWD -> com.paypal.checkout.createorder.CurrencyCode.TWD
        CurrencyCode.NZD -> com.paypal.checkout.createorder.CurrencyCode.NZD
        CurrencyCode.NOK -> com.paypal.checkout.createorder.CurrencyCode.NOK
        CurrencyCode.PHP -> com.paypal.checkout.createorder.CurrencyCode.PHP
        CurrencyCode.PLN -> com.paypal.checkout.createorder.CurrencyCode.PLN
        CurrencyCode.GBP -> com.paypal.checkout.createorder.CurrencyCode.GBP
        CurrencyCode.RUB -> com.paypal.checkout.createorder.CurrencyCode.RUB
        CurrencyCode.SGD -> com.paypal.checkout.createorder.CurrencyCode.SGD
        CurrencyCode.SEK -> com.paypal.checkout.createorder.CurrencyCode.SEK
        CurrencyCode.CHF -> com.paypal.checkout.createorder.CurrencyCode.CHF
        CurrencyCode.THB -> com.paypal.checkout.createorder.CurrencyCode.THB
        CurrencyCode.USD -> com.paypal.checkout.createorder.CurrencyCode.USD
    }

internal val com.paypal.checkout.createorder.CurrencyCode.asPaypalCheckout: CurrencyCode
    get() = when (this) {
        com.paypal.checkout.createorder.CurrencyCode.AUD -> CurrencyCode.AUD
        com.paypal.checkout.createorder.CurrencyCode.BRL -> CurrencyCode.BRL
        com.paypal.checkout.createorder.CurrencyCode.CAD -> CurrencyCode.CAD
        com.paypal.checkout.createorder.CurrencyCode.CNY -> CurrencyCode.CNY
        com.paypal.checkout.createorder.CurrencyCode.CZK -> CurrencyCode.CZK
        com.paypal.checkout.createorder.CurrencyCode.DKK -> CurrencyCode.DKK
        com.paypal.checkout.createorder.CurrencyCode.EUR -> CurrencyCode.EUR
        com.paypal.checkout.createorder.CurrencyCode.HKD -> CurrencyCode.HKD
        com.paypal.checkout.createorder.CurrencyCode.HUF -> CurrencyCode.HUF
        com.paypal.checkout.createorder.CurrencyCode.INR -> CurrencyCode.INR
        com.paypal.checkout.createorder.CurrencyCode.ILS -> CurrencyCode.ILS
        com.paypal.checkout.createorder.CurrencyCode.JPY -> CurrencyCode.JPY
        com.paypal.checkout.createorder.CurrencyCode.MYR -> CurrencyCode.MYR
        com.paypal.checkout.createorder.CurrencyCode.MXN -> CurrencyCode.MXN
        com.paypal.checkout.createorder.CurrencyCode.TWD -> CurrencyCode.TWD
        com.paypal.checkout.createorder.CurrencyCode.NZD -> CurrencyCode.NZD
        com.paypal.checkout.createorder.CurrencyCode.NOK -> CurrencyCode.NOK
        com.paypal.checkout.createorder.CurrencyCode.PHP -> CurrencyCode.PHP
        com.paypal.checkout.createorder.CurrencyCode.PLN -> CurrencyCode.PLN
        com.paypal.checkout.createorder.CurrencyCode.GBP -> CurrencyCode.GBP
        com.paypal.checkout.createorder.CurrencyCode.RUB -> CurrencyCode.RUB
        com.paypal.checkout.createorder.CurrencyCode.SGD -> CurrencyCode.SGD
        com.paypal.checkout.createorder.CurrencyCode.SEK -> CurrencyCode.SEK
        com.paypal.checkout.createorder.CurrencyCode.CHF -> CurrencyCode.CHF
        com.paypal.checkout.createorder.CurrencyCode.THB -> CurrencyCode.THB
        com.paypal.checkout.createorder.CurrencyCode.USD -> CurrencyCode.USD
    }