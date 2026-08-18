Read this before you integrate. It explains how a PayPal Mobile SDK V3.0.0 integration fits together — who is responsible for what, the terms you'll meet, and the shared shape of every checkout — so the per-method guides make sense.

## Responsibility model

A native SDK is one actor among several. Most integration mistakes come from assuming the SDK does something that is actually your responsibility — or your server's, or the device's.

| Responsibility | Owner |
| --- | --- |
| Create and capture the order (and setup/payment tokens for vault) via Orders v2 | **Your server** |
| Obtain a client ID and merchant ID; enable the payment methods on your account | **You (PayPal Developer Dashboard)** |
| Render the PayPal/Venmo button, or your own card fields; register return links | **Your app** |
| Prepare the checkout session, choose app-switch vs. in-app browser, present PayPal/Venmo and the card 3DS challenge, and hand back the result | **The SDK** |
| Route the buyer back to your app via App Links | **The device / OS** |

**The SDK does not:**

* Create or capture orders — your server does, with Orders v2.
* Provide a card-entry UI — for Card (ACDC) you build your own fields and input validation.
* Set return/cancel URLs in the order — in V3 you pass them to the SDK via the URL config, not the Orders API.
* Decide app-switch eligibility — PayPal decides server-side; the SDK just presents the result.

## Key concepts

| Term | What it is |
| --- | --- |
| **Order** | A purchase created on your server via Orders v2. You pass its ID to the SDK; the SDK never creates it. |
| **Client ID / Merchant ID** | Account credentials from the Developer Dashboard. Both are required to initialize the SDK in V3; the merchant ID is the encrypted merchant account ID, distinct from the client ID. |
| **Environment** | `SANDBOX` or `LIVE`, set on `CoreConfig`. |
| **Session** (`createPayPalSession()`) | A prepared PayPal checkout session carrying buyer identity, return URLs, and user action. Required before `start()` / `vault()` for PayPal — not used by Venmo or Card. |
| **Return URLs & fallback scheme** | Where PayPal/Venmo send the buyer back: an App Link plus a required custom-scheme fallback. Configured once and passed to the SDK. |
| **App switch vs. in-app browser** | PayPal/Venmo open the native app when the buyer is eligible and has it installed; otherwise checkout continues in an in-app browser. PayPal decides; both paths return the same way. |
| **Vault / setup token / payment token** | Saving a payment method. Vault with purchase rides on a normal order; vault without purchase uses a setup token. The saved token is retrieved server-side, not from the SDK result. |
| **Funding source** | For PayPal: standard PayPal, Pay Later, or PayPal Credit. |
| **Client metadata ID** (device data) | A risk signal the SDK collects and you attach to your create-order request to reduce declines. |
| **3D Secure (3DS)** | A card step-up challenge, presented by the SDK when PayPal requires it. |

## The shared checkout shape

Every method follows the same observable arc, with small differences:

1. Your app shows the buyer a way to pay (a PayPal/Venmo button, or your card form).
2. On intent, you create the order on your server (for PayPal, you also prepare the session first).
3. You call the client's `start()` / `approveOrder()` with the order ID.
4. The buyer approves — in the PayPal/Venmo app, an in-app browser, or (for card) a 3DS challenge if required.
5. The buyer returns to your app; the SDK delivers a **success**, **cancel**, or **failure** result.
6. On success, your server captures the order (or you store the setup token).

Method differences: **PayPal** uses the session-first pattern; **Venmo** skips the session and splits its result across two calls; **Card** skips the session, ships no UI, and handles 3DS manually via `presentAuthChallenge()` / `finishApproveOrder()`. Each guide documents its own.

## Key behaviors and rules

* `createPayPalSession()` must come before `start()` / `vault()` (PayPal) — otherwise the SDK returns a session-not-started error.
* **Cancellation is a normal outcome**, not an error — return the buyer to checkout.
* **Buttons and UI render without an order** — you only need an order at `start()` / `approveOrder()`.
* **Return links must be consistent** — the URLs you pass to the SDK must match what your app is registered to receive (same host + path prefix / associated domain).
* **You don't manage eligibility or routing** — PayPal chooses app-switch vs. browser; you just handle the result.

## Scope

This page covers the client-side integration model. The server-side order lifecycle (create, authorize, capture, refund) is out of scope — see the [Orders v2 API](https://developer.paypal.com/docs/api/orders/v2/).

## Next steps

* Start with [Install & Setup](../integration-guides/android-install-and-setup.md).
* New to this guide set? See the [overview](../README.md).
