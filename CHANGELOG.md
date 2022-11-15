# PayPal Android SDK Release Notes

## unreleased
* `Card`:
  * Update `CardRequest` to require `ThreeDSecureRequest `
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

