Complete these core setup steps once. Then integrate any payment method — PayPal Checkout, Venmo, or Card — using its guide; each one starts from the setup here and adds only what is specific to that method.

## Requirements

* A PayPal developer account, a **client ID**, and your **merchant ID** (the encrypted merchant account ID, required and distinct from your client ID) from the [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/).
* A server-side integration that can create and capture orders ([Orders v2 API](https://developer.paypal.com/docs/api/orders/v2/)), and — for vault flows — create setup tokens and payment tokens.
* **App Links** configured for your return URL before testing (HTTPS with domain verification).
* Android Studio targeting Android SDK 23+.

## Step 1: Add the SDK

The SDK ships as per-feature modules — add only what you use. Every feature module includes `CorePayments` transitively. Add `fraud-protection` (device data, used to reduce declines) plus the module for each payment method you integrate:

```groovy
dependencies {
    implementation 'com.paypal.android:fraud-protection:X.Y.Z'      // device data (risk signals)

    // Add the module(s) for the method(s) you integrate:
    implementation 'com.paypal.android:paypal-payments:X.Y.Z'       // PayPal Checkout + Vault
    implementation 'com.paypal.android:payment-buttons:X.Y.Z'       // PayPal buttons
    // implementation 'com.paypal.android:venmo:X.Y.Z'             // Venmo Checkout
    // implementation 'com.paypal.android:card-payments:X.Y.Z'     // Card (ACDC)
}
```

Replace `X.Y.Z` with the current V3 release.

## Step 2: Initialize CoreConfig

Every client (`PayPalClient`, `VenmoClient`, `CardClient`) is built from a single `CoreConfig`. No network calls occur at initialization.

```kotlin
val config = CoreConfig(
    clientId       = "<YOUR_CLIENT_ID>",
    merchantId     = "<YOUR_MERCHANT_ID>",       // Required — encrypted merchant account ID, no default
    coreEnvironment = CoreEnvironment.SANDBOX,   // CoreEnvironment.LIVE for production
    bnCode         = null                        // Partner integrations only
)
```

Reuse this `config` across every method client. Switch `CoreEnvironment.SANDBOX` to `CoreEnvironment.LIVE` for production.

## Step 3: Register your return links

PayPal and Venmo checkout send the buyer to the PayPal or Venmo app (or an in-app browser) and back to your app via an Android App Link. Register it on the activity that receives the return. One intent-filter matches on **host + path prefix**, so it covers both your return and cancel URLs:

```xml
<activity android:name=".CheckoutActivity" android:exported="true" android:launchMode="singleTop">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="https" android:host="example.com" android:pathPrefix="/merchant-app" />
    </intent-filter>
</activity>
```

App Switch also supports a custom URL scheme as a fallback for when the App Link cannot be delivered. The SDK only requires that at least one of `returnAppUrl` / `fallbackSchemeUrl` be non-blank — but registering both is the safer default, since an unverified App Link would otherwise leave the buyer with no way back to your app. Register the fallback scheme separately:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="merchantapp" />
</intent-filter>
```

Define the matching URL config once; you pass it to each method's session or checkout call:

```kotlin
val urlConfig = ReturnToAppUrlConfig(
    returnAppUrl      = "https://example.com/merchant-app/return",
    cancelAppUrl      = "https://example.com/merchant-app/cancel",
    fallbackSchemeUrl = "merchantapp://return"   // Recommended — at least one of returnAppUrl / fallbackSchemeUrl must be non-blank
)
```

Verification checklist: `android:autoVerify="true"` is set; your domain serves a valid `/.well-known/assetlinks.json` with your app's SHA-256 fingerprint; the buyer has _Open supported links_ enabled. If the App Link is not verified, Android opens the return in a browser and checkout still completes via the in-browser path.

> Card (ACDC) uses a custom-scheme return only for the 3D Secure challenge and does not require App Links — see the Card guide.

## Next steps

With setup done, integrate a payment method:

* **PayPal Checkout** — One-Time Checkout, Vault (with/without purchase), Pay Later / PayPal Credit
* **Venmo Checkout** — One-Time Checkout
* **Card (ACDC)** — card payments and vaulting

## Related

* [PayPal Developer Dashboard](https://developer.paypal.com/dashboard/)
* [Orders v2 API](https://developer.paypal.com/docs/api/orders/v2/)
