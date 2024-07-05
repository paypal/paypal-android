# PayPal Android SDK Release Notes

## unreleased
* PayPalNativePayments (DEPRECATED)
  * **NOTE:** This module is being deprecated and will be removed in the future version of the SDK. Use `PayPalWebPayments` module instead.
  * Add deprecated warning message to all classes
  
## 1.4.1 (2024-06-06)
* PaymentButtons
  * Add `paylater_color` to `PayLaterButton` to control the color of the pay later button from XML.

## 1.4.0 (2024-04-29)
* CardPayments
  * Add `liabilityShift` property to `CardResult`
  * Callback `PayPalSDKError` when `CardClient#approveOrder()` 3DS verification fails
  * Add `CardClient#presentAuthChallenge()`
  * Add `returnUrl` property to `CardVaultRequest`
  * Add `authChallenge` property to `CardVaultResult`
  * Add `CardAuthChallenge` type
  * Add `CardClient.removeObservers()` method
* FraudDetection
  * Fixes Google Play Store Rejection
    * Bump Magnes version to 5.5.1
    * Create `PayPalDataCollectorRequest`
    * Add `PayPalDataCollector#collectDeviceData(context, request)`
    * Deprecate `PayPalDataCollector#collectDeviceData(context, clientMetadataId, additionalData)`
* PaymentButtons
  * Undeprecate `PayPalCreditButtonColor.BLACK` and `.DARK_BLUE`
  * Undeprecate `PayPalButtonColor.BLUE`, `.BLACK`, and `.SILVER`
  * Added analytics events
    * `payment-button:initialized` and `payment-button:tapped`
  * Update font typeface to "PayPalOpen" to meet brand guidelines
* PayPalNativePayments
  * Fixes Google Play Store Rejection
    * Bump Native Checkout version to 1.3.2
    * Add `hasUserLocationConsent` to `PayPalNativeCheckoutRequest`
* PayPalWebPayments
  * Add `PayPalWebCheckoutClient.removeObservers()` method

## 1.3.0 (2024-01-09)
* PaymentButtons
  * Add `PayPalCreditButtonColor.WHITE` and `.GOLD`
  * Deprecate `PayPalCreditButtonColor.BLACK` and `.DARK_BLUE`
  * Deprecate `PayPalButtonColor.BLUE`, `.BLACK`, and `.SILVER`
* PayPalNativeCheckout
  * Bump native-checkout version to release `1.2.1`

## 1.2.0 (2024-01-04) 

* PaymentButtons
  * Supporting custom corner radius on the PayPal Button
* PayPalWebPayments
  * Add `PayPalWebVaultListener` interface
  * Add `PayPalWebVaultResult` data class
  * Add `vaultListener` property to `PayPalWebCheckoutClient`
  * Add `vault()` method to `PayPalWebCheckoutClient`

## 1.1.0 (2023-12-05)

* PayPalNativeCheckout
  * Bump native-checkout version to release `1.2.0`
  * Add `userAuthenticationEmail` to `PayPalNativeCheckoutRequest`

## 1.0.0 (2023-10-02)

* Breaking Changes
  * CardPayments
    * Make `Amount` class internal
    * Make `ApproveOrderMetadata` class internal
    * Make `AuthenticationResult` class internal
    * Make `Payee` class internal
    * Make `PaymentSource` class internal
    * Make `PurchaseUnit` class internal
    * Make `ThreeDSecureResult` class internal
    * Make `Environment` enum associated values internal
    * Remove `OrderRequest` class
    * Rename `VaultRequest` to `CardVaultRequest`
    * Rename `VaultResult` to `CardVaultResult`
    * Rename `VaultListener` to `CardVaultListener`
  * CorePayments
    * Remove `open` modifier on `PayPalSDKError`

## 0.0.13 (2023-08-22)

* FraudDetection
  * Update `PayPalDataCollector` constructor to require a configuration instead of an environment
  * Remove `PayPalDataCollectorEnvironment` enum
  * Rename `PayPalDataCollector` `getClientMetadataId()` method to `collectDeviceData()`

## 0.0.12 (2023-08-22)

* CardPayments
  * Add `CardClient#vault()` method
  * Add `VaultRequest` and `VaultResult` types for interacting with the `CardClient#vault()` method
  * Add `VaultListener` interface to receive `CardClient#vault()` success and failure results
  * Add `vaultListener` property to `CardClient`
* PayPalNativePayments
  * Bump `PayPal Native Checkout` to `1.1.0`
* Breaking Changes
  * CardPayments
    * Remove `Vault` data class originally meant for Vault with Purchase flows (it's no longer needed)
  * CorePayments
    * Remove `STAGING` constant from `Environment` enum

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

