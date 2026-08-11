Common problems integrating the PayPal Mobile SDK V3.0.0 on Android, organized by symptom. For an error that happens inside one specific flow, also check that guide's **Result handling** section.

## Build and dependency

`Failed to resolve: com.paypal.android:...`

* Likely cause: the module or version is not available, or Maven Central is not in your repositories.
* Fix: confirm `mavenCentral()` is in your repositories and use a current V3 version; add only the modules you use (see [Install & Setup (Android)](android-install-and-setup.md)).

**Duplicate class or version conflict at build time**

* Likely cause: a transitive dependency (e.g. `browser-switch`, Kotlin stdlib) conflicts with another library.
* Fix: align to the SDK's expected toolchain — Java 17, a recent Android Gradle Plugin, and a compatible Kotlin version — and resolve conflicting transitive versions.

**SDK classes missing in a minified (R8/ProGuard) release build**

* Likely cause: code shrinking stripped classes the SDK needs.
* Fix: add keep rules for the PayPal SDK packages you use, then re-test the release build.

## Setup and initialization

`SESSION_NOT_STARTED` returned on `start()` or `vault()`

* Likely cause: `createPayPalSession()` was not called first.
* Fix: call `createPayPalSession(userIdentity, urlConfig, userAction)` in your button's `onClick`, before or alongside order creation. See [PayPal Checkout](android-paypal-checkout.md).

**Auth or configuration errors right after** `start()`

* Likely cause: wrong `environment`, or an invalid/missing `merchantID` (required in V3 and distinct from your client ID).
* Fix: recheck `CoreConfig` — client ID, `merchantID`, and `Environment.SANDBOX` vs `.LIVE`.

## Return and redirect

**The buyer completes checkout but never returns to your app**

* Likely cause: your App Link is not verified, or the return/cancel URLs do not match the host + path prefix your activity is registered for.
* Fix: verify `/.well-known/assetlinks.json` (with your app's SHA-256), `android:autoVerify="true"`, and _Open supported links_ enabled; keep return/cancel URLs under the registered host + prefix. The required `fallbackSchemeUrl` covers App Link delivery failures.

**The result callback never fires after the buyer returns**

* Likely cause: the return intent was not forwarded, or the activity relaunches instead of resuming.
* Fix: call `checkoutClient.handleReturnUrl(intent)` in `onNewIntent`, and set the return activity to `launchMode="singleTop"`.

**Checkout stayed in the browser instead of opening the PayPal app**

* Likely cause: expected fallback — the buyer is not App Switch eligible, the PayPal app is not installed, the buyer is not in the US, or the identity email does not match the signed-in PayPal account.
* Fix: to exercise the app-switch path in testing, meet all trigger conditions (see [PayPal Checkout](android-paypal-checkout.md) → Testing and go-live).

## Result and challenge

**Unexpected** `Cancel`

* Likely cause: the buyer backed out, or the return was not delivered cleanly and resolved as canceled.
* Fix: treat `Cancel` as a normal outcome (return the buyer to checkout); if it happens unexpectedly often, re-check your App Link and return handling.

**A card 3DS challenge never appears**

* Likely cause: you did not call `presentAuthChallenge()` on an `AuthorizationRequired` result, or did not call `finishApproveOrder(intent)` on return.
* Fix: on `AuthorizationRequired`, call `cardClient.presentAuthChallenge(activity, authChallenge)`, then `cardClient.finishApproveOrder(intent)` in `onNewIntent`. See [Card / ACDC](android-card-acdc.md).

## Process death

**An in-flight 3DS challenge is lost when the activity is recreated**

* Likely cause: the client's in-flight state did not survive process death.
* Fix: persist `cardClient.instanceState` before the process dies and call `cardClient.restore(instanceState)` on recreation.

## Still stuck

* Re-check [Install & Setup (Android)](android-install-and-setup.md).
* Look up exact class and method signatures in the generated API reference (Dokka).
* Run the sample app to compare against a known-good integration.
* Contact your PayPal account team or support.

## Related

* [Install & Setup (Android)](android-install-and-setup.md)
* [PayPal Checkout — Integration Guide (Android)](android-paypal-checkout.md)
* [Card / ACDC — Integration Guide (Android)](android-card-acdc.md)
