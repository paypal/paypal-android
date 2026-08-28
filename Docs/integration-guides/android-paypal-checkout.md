This guide shows you how to accept a **PayPal payment** in your Android app with PayPal Mobile SDK V3.0.0 — One-Time Checkout, Vault (with or without a purchase), and Pay Later / PayPal Credit. The **PayPal button** is the highlighted way to start checkout: when the buyer taps it, checkout happens in the PayPal app if they are eligible and have it installed — approving with biometrics or a passkey — then returns to your app through your Android App Link. If the PayPal app is not installed or the buyer is not eligible, checkout continues in an Auth Tab automatically.

> **Before you start:** complete [Install & Setup (Android)](../getting-started/android-install-and-setup.md). It covers the SDK dependency, `CoreConfig`, and return-link registration shared by every payment method.

## Overview

When the buyer taps your PayPal button, you call `createPayPalSession()` (with a token type, buyer identity, return URLs, and user action) and create the order — then call `start(activity, orderId, callback)`. `createPayPalSession()` is required and must come first.

**Prepare the session before checkout.** `start()` requires a prepared shopper session, created by `createPayPalSession()`. Until it has been called, `start()` will not proceed — its callback receives a `PayPalPresentAuthChallengeResult.Failure` carrying `PayPalError.sessionNotCreatedError` (logged as the `SESSION_NOT_STARTED` event). Preparing the session carries the token type, buyer identity, return URLs, and user action to PayPal and determines whether checkout uses the PayPal app or the in-app browser.

**Create the session when the buyer shows intent.** Call `createPayPalSession()` from your PayPal button's `onClick` handler, ideally at the same time as you create the order, so its network latency overlaps order creation and it is ready by the time you call `start()`.

`start()` itself only confirms whether the auth challenge (app switch or Auth Tab) launched — the buyer's actual approval, cancellation, or failure is delivered later, when you forward the return intent to `finishStart()`.

```mermaid
sequenceDiagram
    participant App as Your App
    participant SDK as PayPal SDK
    participant PayPal as PayPal (app or browser)
    participant Server as Your Server

    Note over App: Buyer taps your PayPal button
    par Prepare the session
        App->>SDK: createPayPalSession(tokenType=ORDER_ID, userIdentity, urlConfig, userAction)
    and Create the order
        App->>Server: Create order (Orders v2)
        Server-->>App: orderId
    end
    App->>SDK: start(activity, orderId, callback)
    alt Buyer eligible and PayPal app installed
        SDK->>PayPal: Open the PayPal app via your App Link
        Note over PayPal: Buyer approves with biometrics or a passkey
    else App not installed or buyer not eligible
        SDK->>PayPal: Open checkout in an Auth Tab
        Note over PayPal: Buyer logs in and approves
    end
    SDK->>App: PayPalPresentAuthChallengeResult (challenge presented)
    PayPal-->>App: Return to your app (App Link)
    App->>SDK: finishStart(intent)
    SDK->>App: PayPalFinishStartResult: Success / Canceled / Failure / NoResult
    App->>Server: Capture order (Orders v2)
```

## How the SDK works

The SDK handles the client-side of checkout. It does not create or capture orders — your server does that with the Orders v2 API. Your responsibilities are: configure the client (see Install & Setup); in your `onClick` handler call `createPayPalSession()`, create the order, and pass its ID and your `Activity` to `start()`; then forward the return intent to `finishStart()` and capture. Choosing the experience (PayPal app vs. in-app browser) and returning the buyer are handled for you. The one ordering rule is that `createPayPalSession()` must be called before `start()`.

## Before you begin

Complete [Install & Setup (Android)](../getting-started/android-install-and-setup.md). For PayPal Checkout specifically:

* Add the `com.paypal.android:paypal-payments` and `com.paypal.android:payment-buttons` modules.
* Construct the client from the `CoreConfig` you built in setup:

```kotlin
val checkoutClient = PayPalClient(context, config)   // config from Install & Setup
```

## Server: create an order

Call the Orders v2 API server-to-server and return **only the order ID** to your app. Return and cancel URLs are passed to the SDK via the URL config (from Install & Setup), not in the Orders API body.

```shell
curl -X POST https://api-m.sandbox.paypal.com/v2/checkout/orders \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <access-token>' \
  -d '{ "intent": "CAPTURE", "purchase_units": [ { "amount": { "currency_code": "USD", "value": "49.99" } } ] }'
```

## Integrate PayPal Checkout

### Step 1: Add the PayPal button

Render a `PayPalButton` on your checkout screen and set a click listener. It needs no order or session to display — declare it in your layout (or create it programmatically), then wire the tap:

```kotlin
payPalButton.setOnClickListener {
    beginCheckout()   // Steps 2-4
}
```

For the button's colors, labels, shapes, and sizes — and the Pay Later / PayPal Credit buttons — see [Payment Buttons (Android)](android-payment-buttons.md).

### Step 2: Create the PayPal session

Call this in the button's `onClick`, before or alongside order creation. It returns immediately and prepares the session in the background. Pass `TokenType.ORDER_ID` for One-Time Checkout and Vault with Purchase — both are approved against an order ID (see [Vault without Purchase](#vault-without-purchase) below for the vault-only case).

```kotlin
import com.paypal.android.corepayments.model.TokenType

checkoutClient.createPayPalSession(
    tokenType    = TokenType.ORDER_ID,
    userIdentity = PayPalUserIdentity(email = "buyer@example.com"),  // or null — see Identity below
    urlConfig    = urlConfig,
    userAction   = PayPalUserAction.CONTINUE   // PAY_NOW for a "Pay Now" button
)
```

### Step 3: Collect device data

Collect device data before you create the order, and attach the resulting client metadata ID to your create-order request so PayPal's risk systems can reduce declines.

```kotlin
val dataCollector = PayPalDataCollector(config)
val clientMetadataId = dataCollector.collectDeviceData(
    context, PayPalDataCollectorRequest(hasUserLocationConsent = false)
)
// Send clientMetadataId to your server; set it as the PayPal-Client-Metadata-Id header on your Orders v2 create call.
```

### Step 4: Create the order and start checkout

```kotlin
val orderId = myServer.createOrder()   // include the client metadata ID from Step 3

checkoutClient.start(
    activity = this,
    orderId  = orderId,
    callback = object : PayPalResultCallback {
        override fun onPayPalResult(result: PayPalPresentAuthChallengeResult) {
            when (result) {
                is PayPalPresentAuthChallengeResult.Success -> { /* Challenge presented (app switch or browser) — awaiting the buyer's return */ }
                is PayPalPresentAuthChallengeResult.Failure -> showError(result.error)   // includes SESSION_NOT_STARTED
            }
        }
    }
)
```

### Step 5: Handle the return

Forward the return intent to `finishStart()` when your activity re-enters the foreground. This is where the buyer's actual approval, cancellation, or failure is delivered — `start()`'s callback only confirmed the challenge launched.

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    when (val result = checkoutClient.finishStart(intent)) {
        is PayPalFinishStartResult.Success  -> result.orderId?.let { captureOrder(it) }   // result.payerId also available; both are nullable
        is PayPalFinishStartResult.Canceled -> showCheckoutScreen()   // result.orderId also available, if present
        is PayPalFinishStartResult.Failure  -> showError(result.error)   // result.orderId also available, if present
        PayPalFinishStartResult.NoResult    -> Unit   // unrelated intent; ignore
        null                                 -> Unit   // finishStart() called without a matching start()/vault() call in this process
    }
}
```

### Step 6: Capture the order

```kotlin
fun captureOrder(orderId: String) {
    // POST /v2/checkout/orders/{orderId}/capture on your server
    myServer.captureOrder(orderId)
}
```

## Identity — PayPalUserIdentity

You pass `userIdentity` to `createPayPalSession()` as a `PayPalUserIdentity` data class. The hint helps PayPal recognize the buyer, which can increase app-switch eligibility and approval rates. The SDK sends email and phone to PayPal for matching. `userIdentity` is nullable — pass `null` when you have no buyer hint; the session is still created.

| Field | Type | When to use |
| --- | --- | --- |
| `existingPayPalSessionId` | `String?` | Your server already created a buyer session — pass its ID. |
| `email` | `String?` | You know the buyer's email address. |
| `phone` | `PayPalPhoneNumber?` | You know the buyer's phone number — construct with `countryCode` and `nationalNumber`. |

```kotlin
// Email only
PayPalUserIdentity(email = "buyer@example.com")

// Email and phone together improve matching further
PayPalUserIdentity(
    email = "buyer@example.com",
    phone = PayPalPhoneNumber(countryCode = "1", nationalNumber = "4085551234")
)

// Server-side shopper session
PayPalUserIdentity(existingPayPalSessionId = serverSideSessionId)

// No buyer hint
checkoutClient.createPayPalSession(tokenType = TokenType.ORDER_ID, userIdentity = null, urlConfig = urlConfig, userAction = PayPalUserAction.CONTINUE)
```

## Vault with Purchase

Vault with Purchase saves the buyer's PayPal account **while** completing a real purchase, in a single approval. There is no separate client call — use the exact same button → `createPayPalSession(tokenType = TokenType.ORDER_ID, ...)` → `start(activity, orderId, callback)` → `finishStart(intent)` sequence above. The only difference is that your server creates the order with `payment_source.paypal.attributes.vault` set:

```shell
curl -X POST https://api-m.sandbox.paypal.com/v2/checkout/orders \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <access-token>' \
  -d '{
    "intent": "CAPTURE",
    "purchase_units": [ { "amount": { "currency_code": "USD", "value": "49.99" } } ],
    "payment_source": { "paypal": { "attributes": { "vault": { "store_in_vault": "ON_SUCCESS", "usage_type": "MERCHANT", "customer_type": "CONSUMER" } } } }
  }'
```

On approval PayPal both completes the purchase and stores the payment method, but `PayPalFinishStartResult.Success` still only carries the order ID and payer ID — **the SDK does not return a payment token for this flow.** Retrieve the vaulted token server-side (GET the order, or via the Payment Method Tokens API) after capture.

## Vault without Purchase

Save a buyer's PayPal account for future charges with no purchase now. Call `createPayPalSession()` first with `tokenType = TokenType.VAULT_ID` (use `SETUP_NOW` for a "Set up now" button), then create the setup token on your server and call `vault()`.

```kotlin
// In your "Set up now" button's onClick handler:
checkoutClient.createPayPalSession(
    tokenType    = TokenType.VAULT_ID,
    userIdentity = userIdentity,
    urlConfig    = urlConfig,
    userAction   = PayPalUserAction.SETUP_NOW
)

val setupTokenId = myServer.createSetupToken()

checkoutClient.vault(
    activity     = this,
    setupTokenId = setupTokenId,
    callback = object : PayPalResultCallback {
        override fun onPayPalResult(result: PayPalPresentAuthChallengeResult) {
            when (result) {
                is PayPalPresentAuthChallengeResult.Success -> { /* Challenge presented — awaiting the buyer's return */ }
                is PayPalPresentAuthChallengeResult.Failure -> showError(result.error)   // includes SESSION_NOT_STARTED
            }
        }
    }
)
```

Forward the return intent to `finishVault()` in `onNewIntent`, the same way checkout forwards to `finishStart()`. The vaulted approval is identified by `approvalSessionId`, not the setup token ID:

```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    when (val result = checkoutClient.finishVault(intent)) {
        is PayPalFinishVaultResult.Success -> savePaymentToken(result.approvalSessionId)
        PayPalFinishVaultResult.Canceled   -> showVaultScreen()
        is PayPalFinishVaultResult.Failure -> showError(result.error)
        PayPalFinishVaultResult.NoResult   -> Unit   // unrelated intent; ignore
        null                                -> Unit   // finishVault() called without a matching start()/vault() call in this process
    }
}
```

## Pay Later and PayPal Credit

Pay Later and PayPal Credit are alternate PayPal funding sources — not separate SDK flows. Selection happens entirely on your server: set `payment_source.paypal.experience_context.payment_method_selected` to `PAYPAL` (default), `PAYPAL_PAY_LATER`, or `PAYPAL_CREDIT` when you create the order. The client-side flow is identical regardless of funding source — the same `createPayPalSession()` → `start(activity, orderId, callback)` → `finishStart(intent)` sequence shown above handles all three. Both Pay Later and PayPal Credit are supported for One-Time Checkout and Vault with Purchase; neither applies to Vault without Purchase.

`PayPalCheckoutFundingSource` (`PAYPAL` / `PAY_LATER` / `PAYPAL_CREDIT`) is a convenience enum you can use in your own app code to track which funding source the buyer selected and map it to the corresponding `payment_method_selected` value on your create-order request — it is not passed to any `PayPalClient` method.

Dedicated buttons exist — `PayLaterButton` and `PayPalCreditButton` (`payment-buttons` module) — see [Payment Buttons (Android)](android-payment-buttons.md).

## Result handling

`start()` and `vault()` deliver a `PayPalPresentAuthChallengeResult` that only confirms whether the auth challenge (app switch or Auth Tab) launched. The buyer's actual outcome arrives later, when you forward the return intent to `finishStart()` (checkout, including Vault with Purchase) or `finishVault()` (Vault without Purchase). Handle all cases — cancellation is a normal buyer choice, not an error.

| Call | Type | Cases | What you do |
| --- | --- | --- | --- |
| `start()` / `vault()` callback | `PayPalPresentAuthChallengeResult` | `Success` / `Failure(error)` | `Success` only confirms the challenge launched. `Failure` means checkout/vault never launched — show the error. `SESSION_NOT_STARTED` means `createPayPalSession()` was not called before `start()` / `vault()` — fix the ordering. |
| `finishStart(intent)` | `PayPalFinishStartResult?` | `Success(orderId?, payerId?)` / `Canceled(orderId?)` / `Failure(error, orderId?)` / `NoResult` / `null` | Success: capture the order if `orderId` is present. Canceled: return the buyer to your checkout screen; `orderId` is included when available; no charge was made. Failure: show an error; `orderId` is included when available. `NoResult`: the intent was not a PayPal return — ignore it. `null`: `finishStart()` was called with no matching `start()`/`vault()` call in this process — ignore it. |
| `finishVault(intent)` | `PayPalFinishVaultResult?` | `Success(approvalSessionId)` / `Canceled` / `Failure(error)` / `NoResult` / `null` | Success: store the returned `approvalSessionId`. Canceled: return the buyer to your save screen. Failure: show an error. `NoResult`: the intent was not a PayPal return — ignore it. `null`: same as above — no matching `start()`/`vault()` call in this process. |

## Best practices

**Show a loading indicator after the button tap.** Disable the button immediately after the buyer taps it, and show a loading indicator while `createPayPalSession()`, order creation, and `start()` are in flight. This prevents duplicate submissions.

**Provide buyer email and phone.** Pass both in `userIdentity` when you have them — together they improve app-switch eligibility, risk assessment, and approval rates beyond email alone.

**Handle the return to your app.** Remove the loading indicator as soon as your app returns to the foreground (`onNewIntent` / `onResume`). If the buyer backgrounded the PayPal app without approving or canceling, let them resume rather than restarting checkout.

**Handle redirection to the browser.** Occasionally the OS opens the return in a Chrome tab instead of your app — usually because the App Link is not verified. Registering a `fallbackSchemeUrl` alongside your App Link covers the common case; as a safeguard, consider website logic that detects a redirected buyer, confirms order status, and guides them back.

## Testing and go-live

Test on a **physical device** — the app-switch path does not work on an emulator.

### Trigger the app-switch path

The SDK switches to the PayPal app only when all of the following hold; otherwise it falls back to an Auth Tab (or a Custom Tab on browsers that do not support Auth Tab):

* A physical device with the PayPal app installed (the sandbox app for sandbox testing).
* Merchant and buyer are in the US, and your integration is App Switch eligible.
* The buyer-identity email you pass to `createPayPalSession()` matches the account signed into the PayPal app.
* In the PayPal app, **Extend your login session** (and fingerprint) are enabled under **Avatar › Login and security**.

To test the in-app browser path, use a device without the PayPal app installed, or an ineligible buyer. Full sandbox-app setup lives in the published [PayPal Android testing guide](https://developer.paypal.com/braintree/docs/guides/paypal/testing-go-live/android/v5#testing-app-switch).

### Before you launch, test

| Scenario | Expected result |
| --- | --- |
| **App switch — end to end** | The buyer switches to the PayPal app, approves, returns to your app, and the order captures. |
| **In-app browser — end to end** | With the PayPal app not installed (or an ineligible buyer), checkout completes in an Auth Tab and the order captures. |
| **In-app browser — buyer cancellation** | Close the Auth Tab before approval and verify the SDK reports `Canceled`. |
| Buyer cancels in PayPal | The buyer is returned to your checkout screen and no charge is made. |
| Vault with Purchase | Order captures; the vaulted token is retrievable server-side (not from the SDK result). |
| Vault without Purchase | You receive and store the approval session ID from `finishVault()`, and can charge the vaulted account later. |
| Pay Later / PayPal Credit eligible buyer | Financing offers render; approval and capture proceed the same as standard PayPal. |

### Go live

- [ ] Call `createPayPalSession()` on the button tap, before or alongside order creation.
- [ ] Switch `CoreEnvironment.SANDBOX` to `CoreEnvironment.LIVE` and use your live client ID and merchant ID.
- [ ] Verify the App Link return works in a release build (assetlinks.json hosted and verified).
- [ ] Verify the custom-scheme fallback (`fallbackSchemeUrl`) is registered and returns the buyer to your app.
- [ ] Confirm all result variants are handled: `PayPalPresentAuthChallengeResult` (Success, Failure) from `start()`/`vault()`, and `PayPalFinishStartResult` / `PayPalFinishVaultResult` (Success, Canceled, Failure, NoResult, and `null`) from `finishStart()`/`finishVault()`.
- [ ] Collect device data and pass the client metadata ID on your Orders v2 request.
- [ ] Contact your PayPal account team to enable App Switch for production traffic.

## Related

* [Install & Setup (Android)](../getting-started/android-install-and-setup.md)
* [Payment Buttons (Android)](android-payment-buttons.md)
* [Troubleshooting (Android)](android-troubleshooting.md)
* [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/)
* [Orders v2 API](https://developer.paypal.com/docs/api/orders/v2/)
