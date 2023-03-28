# PayPal Android SDK Release Notes

## unreleased
* CardPayments:
  * Make `Card.securityCode` required

## 0.0.8 (2023-03-13)
* `PayPalNativePayments`:
  *  Bump `PayPal Native Checkout` to `0.8.8` and add `return_url`
* Send analytic events for `PayPalNativePayments`, `PayPalWebPayments`, and `CardPayments` flows

## 0.0.7 (2023-01-25)
* Rename `PayPalDataCollector` to `FraudProtection`
* Rename `PayPalNativeCheckout` to `PayPalNativePayments`
* Rename `PayPalWebCheckout` to `PayPalWebPayments`
* Rename `Card` to `CardPayments`
* Rename `PayPalUI` to `PaymentButtons`
* Rename `Core` to `CorePayments`
* `PayPalNativeCheckout`:
  * Fix `MagnesSDK` not found error
  * Bump NXO to 0.8.7

## 0.0.6 (2022-12-02)
* `Card`:
  * Remove `ThreeDSecureRequest` from `CardRequest`
  * Update `CardRequest` to pass `return_url` and an optional `sca`
* `PayPalUI`:
  * Fix: remove loading spinner on buttons.

## 0.0.5 (2022-10-18)
* Add `PayPalNativeCheckout`:
  * Use the native Paypal checkout flow in your app 

## 0.0.4 (2022-07-25)
* Fix hardcoded url scheme for the BrowserSwitch in Card

## 0.0.3 (2022-07-21)
* Remove client secret and add support for Full Access Tokens

## 0.0.2 (2022-07-21)
* Add concurrency to github actions
* Add snapshot repository

## 0.0.1 (2022-07-18)
* First testable version of the SDK.
* Add `Card`: 
  * Enables users to use unbranded cards to approve orders
* Add `PayPalWebCheckout`:
  * Use the web Paypal checkout flow in your app
* Add `PayPalUI`:
  * Provides a set of customizable PayPal Buttons

