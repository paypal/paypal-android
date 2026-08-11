How to move an existing **PayPal Mobile SDK V2 (2.x)** Android integration to **V3.0.0**. The largest changes are in PayPal Checkout: the client was renamed, checkout is now session-first, `CoreConfig` requires a `merchantID`, and return URLs moved out of the Orders API into a URL config.

> **Preview — confirm before you rely on it.** V3.0.0 is not yet released (published `main` is 2.3.0). This guide is derived from the current V3 integration guides and the published 2.x API; confirm the exact deltas against the official V3 release notes when V3 ships.

## What's changed (PayPal Checkout)

| Area | V2 (2.x) | V3 |
| --- | --- | --- |
| Client class | `PayPalWebCheckoutClient` | `PayPalWebClient` |
| `CoreConfig` | client ID + environment | adds **required** `merchantID`, optional `bnCode` |
| Session | none | `createPayPalSession()` is **required before** `start()` |
| Return URLs | `urlScheme` on the client + `experienceContext` URLs in the Orders API | `ReturnToAppUrlConfig` passed to `createPayPalSession()`; **not** set in the Orders API |
| `start()` | `start(activity, request)` | `start(orderId, callback)` |
| Result | `PayPalWebCheckoutFinishStartResult` via `finishStart(intent)` | `PayPalWebCheckoutResult` (`Success`/`Cancel`/`Failure`) via `PayPalWebStartCallback` |
| Return handling | `finishStart(intent)` returns the result | `handleReturnUrl(intent)` resolves the `start()` callback |

Card (ACDC) and Venmo keep their own clients; the main change they inherit is the `merchantID` on `CoreConfig`. Card's `approveOrder()` / `presentAuthChallenge()` / `finishApproveOrder()` result-type pattern is unchanged from 2.x.

## Before you upgrade

* Get your **merchant ID** (the encrypted merchant account ID) from the [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/) — it is now required to initialize the SDK.
* Toolchain is unchanged from late 2.x: Java 17, a recent Android Gradle Plugin, and a compatible Kotlin version.

## Update the dependency

Bump each PayPal module to the V3 release (module names are unchanged):

```groovy
dependencies {
    implementation 'com.paypal.android:paypal-web-payments:3.0.0'
    implementation 'com.paypal.android:payment-buttons:3.0.0'
    implementation 'com.paypal.android:fraud-protection:3.0.0'
}
```

## Migrate PayPal Checkout

Use this diff to guide the change:

```diff
  val config = CoreConfig(
      clientID = "<CLIENT_ID>",
+     merchantID = "<MERCHANT_ID>",   // now required, distinct from your client ID
      environment = Environment.SANDBOX
  )

- val client = PayPalWebCheckoutClient(context, config, "my-url-scheme")
+ val client = PayPalWebClient(context, config)
+ val urlConfig = ReturnToAppUrlConfig(
+     returnAppUrl = "https://example.com/merchant-app/return",
+     cancelAppUrl = "https://example.com/merchant-app/cancel",
+     fallbackSchemeUrl = "merchantapp://return"
+ )

  fun onPayPalButtonTapped() {
+     // NEW: prepare the session before start()
+     client.createPayPalSession(
+         userIdentity = PayPalUserIdentity.Email(email = "buyer@example.com", phone = null),
+         urlConfig = urlConfig,
+         userAction = PayPalUserAction.CONTINUE
+     )
      val orderId = myServer.createOrder()
-     client.start(this, PayPalWebCheckoutRequest(orderId)) { /* PayPalPresentAuthChallengeResult */ }
+     client.start(orderId, object : PayPalWebStartCallback {
+         override fun onPayPalWebStartResult(result: PayPalWebCheckoutResult) {
+             when (result) {
+                 is PayPalWebCheckoutResult.Success -> captureOrder(result.orderId)
+                 is PayPalWebCheckoutResult.Cancel  -> showCheckoutScreen()
+                 is PayPalWebCheckoutResult.Failure -> showError(result.error)
+             }
+         }
+     })
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
-     client.finishStart(intent)?.let { /* PayPalWebCheckoutFinishStartResult */ }
+     client.handleReturnUrl(intent)   // resolves the start() callback
  }
```

## Server-side change

In V2 you set `experienceContext.returnUrl` / `cancelUrl` when creating the order. In V3 those move to the SDK via `ReturnToAppUrlConfig` — **remove them from your Orders v2 create call** for the session-based flow. Your manifest App Link and custom-scheme fallback stay as they were (see [Install & Setup (Android)](android-install-and-setup.md)).

## Pay Later / PayPal Credit

Funding-source selection still uses the non-session `start(activity, request: PayPalWebCheckoutRequest, callback)` overload in V3 (it was not removed). If you selected `PAY_LATER` / `PAYPAL_CREDIT` in V2, that code keeps working; the session-based `start(orderId, callback)` hardcodes `PAYPAL`.

## Verify the upgrade

* The project compiles with `PayPalWebClient` and no references to `PayPalWebCheckoutClient` remain.
* A sandbox checkout completes end to end: `createPayPalSession()` → `start(orderId, callback)` → return via `handleReturnUrl(intent)` → capture.
* `SESSION_NOT_STARTED` does not occur (confirms `createPayPalSession()` runs before `start()`).

If something breaks after upgrading, see [Troubleshooting (Android)](android-troubleshooting.md).

## Related

* [Install & Setup (Android)](android-install-and-setup.md)
* [PayPal Checkout — Integration Guide (Android)](android-paypal-checkout.md)
* [Troubleshooting (Android)](android-troubleshooting.md)
