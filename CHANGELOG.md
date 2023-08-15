# PayPal Android SDK Release Notes

## unreleased

* CardPayments
  * Add `CardClient#vault()` method
  * Add `VaultRequest` and `VaultResult` types for interacting with the `CardClient#vault()` method
  * Add `VaultListener` interface to receive `CardClient#vault()` success and failure results
  * Add `vaultListener` property to `CardClient`
* Breaking Changes
  * CardPayments
    * Remove `Vault` data class originally meant for Vault with Purchase flows (it's no longer needed)
  * CorePayments
    * Remove `STAGING` constant from `Environment` enum
  * FraudDetection
    * Update `PayPalDataCollector` constructor to require a configuration instead of an environment
    * Remove `PayPalDataCollectorEnvironment` enum
    * Rename `PayPalDataCollector` `getClientMetadataId()` method to `collectDeviceData()`
* PayPalNativePayments
  * Bump `PayPal Native Checkout` to `1.1.0`

## 0.0.11 (2023-08-14)
* All Modules
  * Bump Kotlin version to `1.8.21`
* CardPayments
  * Make `Card` implement `Parcelable`
  * Make `CardRequest` implement `Parcelable`
* CorePayments
  * Make `Address` implement `Parcelable`
* PayPalNativePayments
  * Bump `PayPal Native Checkout` to `1.0.0`

## 0.0.10 (2023-06-23)
* Breaking Changes
  * CardPayments
    * Remove `status` property from `CardResult`
    * Remove `paymentSource` property from `CardResult`
  * CorePayments
    * CoreConfig instances must now be instantiated using a `clientId` instead of an `accessToken`

## 0.0.9 (2023-05-09)
* CardPayments:
  * Make `Card.securityCode` required
* `PayPalNativePayments`:
  * Remove `Approval` from `PayPalNativeCheckoutResult`, expose only `orderID` and `payerID`
  * Add `PayPalNativeCheckoutRequest` to `startCheckout`, removing `CreateOrder` callback
  * Remove `onPayPalCheckoutShippingChange` method from `PayPalNativeCheckoutListener`.
  * Add `PayPalNativeShippingListener` to receive events on changes in shipping information. Add `PayPalNativeShippingAddress`, `PayPalNativeShippingMethod` and `PayPalNativePaysheetActions`
  * Remove `PayPalCheckout` as an `api` dependency
* CorePayments:
  * Send `orderId` instead of `sessionId` for analytics

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

