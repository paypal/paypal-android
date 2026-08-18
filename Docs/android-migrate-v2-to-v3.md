How to move an existing **PayPal Mobile SDK V2 (2.x)** Android integration to **V3.0.0**. The largest changes are in PayPal Checkout: the client was renamed, checkout is now session-first, `CoreConfig` requires a `merchantId`, and return URLs moved out of the Orders API into a URL config.

## What's changed (PayPal Checkout)

| Area | V2 (2.x) | V3 |
| --- | --- | --- |
| Client class | `PayPalWebCheckoutClient` | `PayPalClient` |
| `CoreConfig` | `clientId` + `environment` | adds **required** `merchantId`, optional `bnCode` |
| Session | none | `createPayPalSession()` is **required before** `start()` / `vault()` |
| Return URLs | `urlScheme` on the client + `experienceContext` URLs in the Orders API | `ReturnToAppUrlConfig` passed to `createPayPalSession()`; **not** set in the Orders API |
| `start()` | `start(activity, request)` | `start(activity, orderId, callback)` |
| Result | `PayPalWebCheckoutFinishStartResult` via `finishStart(intent)` | `start()`'s callback reports `PayPalPresentAuthChallengeResult` (`Success`/`Failure`) — whether the checkout UI was presented, nothing more. The actual checkout outcome — `PayPalFinishStartResult` (`Success`/`Canceled`/`Failure`/`NoResult`) — is delivered separately by `finishStart(intent)` when the buyer returns |
| Return handling | `finishStart(intent)` returns the result | Same method name, `finishStart(intent)`, called from `onNewIntent`; now returns `PayPalFinishStartResult?` |

Card (ACDC) and Venmo keep their own clients; the main change they inherit is the `merchantId` on `CoreConfig`. Card's `approveOrder()` / `presentAuthChallenge()` / `finishApproveOrder()` result-type pattern is unchanged from 2.x.

## Before you upgrade

* Get your **merchant ID** (the encrypted merchant account ID) from the [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/) — it is now required to initialize the SDK.
* Toolchain is unchanged from late 2.x: Java 17, a recent Android Gradle Plugin, and a compatible Kotlin version.

## Update the dependency

Bump each PayPal module to the V3 release. The PayPal Checkout artifact is renamed from `paypal-web-payments` to `paypal-payments`; the other module names are unchanged:

```groovy
dependencies {
    implementation 'com.paypal.android:paypal-payments:3.0.0'   // was paypal-web-payments in V2
    implementation 'com.paypal.android:payment-buttons:3.0.0'
    implementation 'com.paypal.android:fraud-protection:3.0.0'
}
```

## Migrate PayPal Checkout

Use this diff to guide the change:

```diff
  val config = CoreConfig(
      clientId = "<CLIENT_ID>",
+     merchantId = "<MERCHANT_ID>",   // now required, distinct from your client ID
      environment = Environment.SANDBOX
  )

- val client = PayPalWebCheckoutClient(context, config, "my-url-scheme")
+ val client = PayPalClient(context, config)
+ val urlConfig = ReturnToAppUrlConfig(
+     returnAppUrl = "https://example.com/merchant-app/return",
+     cancelAppUrl = "https://example.com/merchant-app/cancel",
+     fallbackSchemeUrl = "merchantapp://return"
+ )

  fun onPayPalButtonTapped() {
+     // NEW: prepare the session before start()
+     client.createPayPalSession(
+         tokenType = TokenType.ORDER_ID,
+         userIdentity = PayPalUserIdentity(email = "buyer@example.com"),
+         urlConfig = urlConfig,
+         userAction = PayPalUserAction.CONTINUE
+     )
      val orderId = myServer.createOrder()
-     client.start(this, PayPalWebCheckoutRequest(orderId)) { /* PayPalPresentAuthChallengeResult */ }
+     client.start(this, orderId, object : PayPalResultCallback {
+         override fun onPayPalResult(result: PayPalPresentAuthChallengeResult) {
+             when (result) {
+                 is PayPalPresentAuthChallengeResult.Success -> { /* checkout UI presented; the outcome arrives in onNewIntent */ }
+                 is PayPalPresentAuthChallengeResult.Failure -> showError(result.error)
+             }
+         }
+     })
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
-     client.finishStart(intent)?.let { /* PayPalWebCheckoutFinishStartResult */ }
+     client.finishStart(intent)?.let { result ->
+         when (result) {
+             is PayPalFinishStartResult.Success -> captureOrder(result.orderId)
+             is PayPalFinishStartResult.Canceled -> showCheckoutScreen()
+             is PayPalFinishStartResult.Failure -> showError(result.error)
+             PayPalFinishStartResult.NoResult -> { /* intent was not a checkout return */ }
+         }
+     }
  }
```

## Server-side change

In V2 you set `experienceContext.returnUrl` / `cancelUrl` when creating the order. In V3 those move to the SDK via `ReturnToAppUrlConfig` — **remove them from your Orders v2 create call** for the session-based flow. Your manifest App Link and custom-scheme fallback stay as they were (see [Install & Setup (Android)](android-install-and-setup.md)).

## Pay Later / PayPal Credit

In V2, selecting `PAY_LATER` / `PAYPAL_CREDIT` funding required the non-session `start(activity, request: PayPalWebCheckoutRequest, callback)` overload. Both that overload and `PayPalWebCheckoutRequest` were removed in V3. Funding-source selection moves entirely to your server: set `payment_source.paypal.experience_context.payment_method_selected` to `PAYPAL` (default), `PAYPAL_PAY_LATER`, or `PAYPAL_CREDIT` when you create the order — the client-side `createPayPalSession()` → `start(activity, orderId, callback)` flow is identical regardless of funding source. See [PayPal Checkout](android-paypal-checkout.md)'s "Pay Later and PayPal Credit" section for details.

## Verify the upgrade

* The project compiles with `PayPalClient` and no references to `PayPalWebCheckoutClient` remain.
* A sandbox checkout completes end to end: `createPayPalSession()` → `start(activity, orderId, callback)` → `finishStart(intent)` in `onNewIntent` → capture.
* `PayPalEvent.SESSION_NOT_STARTED` does not fire (confirms `createPayPalSession()` runs before `start()`).

If something breaks after upgrading, see [Troubleshooting (Android)](android-troubleshooting.md).

## Related

* [Install & Setup (Android)](android-install-and-setup.md)
* [PayPal Checkout — Integration Guide (Android)](android-paypal-checkout.md)
* [Troubleshooting (Android)](android-troubleshooting.md)
