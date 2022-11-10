# Accepting Card Payments

The Card module in the PayPal SDK enables Credit and Debit card payments in your app.

Follow these steps to add Card payments:

1. [Setup a PayPal Developer Account](#setup-a-paypal-developer-account)
1. [Add Card Module](#add-card-module)
1. [Test and go live](#test-and-go-live)

## Setup a PayPal Developer Account

You will need to set up authorization to use the PayPal Payments SDK. 
Follow the steps in [Get Started](https://developer.paypal.com/api/rest/#link-getstarted) to create a client ID and generate an access token. 

You will need a server integration to create an order and capture funds using [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2). 
For initial setup, the `curl` commands below can be used in place of a server SDK.

## Add Card Module

### 1. Add the Payments SDK Card module to your app

![Maven Central](https://img.shields.io/maven-central/v/com.paypal.android/card?style=for-the-badge) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.paypal.android/card?server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge)

In your app's `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:card:<CURRENT-VERSION>"
}
```

### 2. Initiate the Payments SDK

Create a `CoreConfig` using an [access token](../../README.md#access-token):

```kotlin
val config = CoreConfig("<ACCESS_TOKEN>", environment = Environment.SANDBOX)
```

Create a `CardClient` to approve an order with a Card payment method. Also, set a listener to receive callbacks from the client.

```kotlin
val cardClient = CardClient(activity, config)
cardClient.approveOrderListener = this
```

### 3. Create an order

When a user initiates a payment flow, call `v2/checkout/orders` to create an order and obtain an order ID:

**Request**
```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw '{
    "intent": "<CAPTURE|AUTHORIZE>",
    "purchase_units": [
        {
            "amount": {
                "currency_code": "USD",
                "value": "5.00"
            }
        }
    ]
}'
```

**Response**
```json
{
   "id":"<ORDER_ID>",
   "status":"CREATED"
}
```

The `id` field of the response contains the order ID to pass to your client.

### 4. Create a request containing the card payment details

Create a `Card` object containing the user's card details.

```kotlin
val card = Card(
    number = "4111111111111111",
    expirationMonth = "01",
    expirationYear = "25",
    securityCode = "123",
    billingAddress = Address(
        streetAddress = "123 Main St.",
        extendedAddress = "Apt. 1A",
        locality = "city",
        region = "IL",
        postalCode = "12345",
        countryCode = "US"
    )
)
```

Attach the card and the order ID from [step 3](#3-create-an-order) to a `CardRequest`.

```kotlin
val cardRequest  = CardRequest("<ORDER_ID>", card)
```

Strong Consumer Authentication (SCA) is enabled by default, so you need to attach a `ThreeDSecureRequest` to a `CardRequest` that will require users to provide additional authentication information via 3D Secure. Set `sca` with `SCA_ALWAYS` to enable 3DS for every transaction

To request SCA, add the following to `CardRequest`:

```kotlin
cardRequest.threeDSecureRequest = ThreeDSecureRequest(
  sca = SCA.SCA_ALWAYS, // default value is SCA_WHEN_REQUIRED
  // custom url scheme needs to be configured in AndroidManifest.xml (see below)
  returnUrl = "myapp://return_url",
  cancelUrl = "myapp://cancel_url"
)
```

Notice the `myapp://` portion of the `returnUrl` and `cancelUrl` in the above code snippet. This `myapp` custom url scheme must also be registered in your app's `AndroidManifest.xml`.

Edit your app's `AndroidManifest.xml` to include an `intent-filter` and set the `android:scheme` on the Activity that will be responsible for handling the deep link back into the app. Also set the activity `launchMode` to `singleTop`:
> Note: `android:exported` is required if your app compile SDK version is API 31 (Android 12) or later.

```xml
<activity
    android:name=".MyDeepLinkTargetActivity"
    android:launchMode="singleTop"
    android:exported="true"
    ...
    >
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <data android:scheme="myapp"/>
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
    </intent-filter>
</activity>
```
Also, add `onNewIntent` to your activity: 

```kotlin
override fun onNewIntent(newIntent: Intent?) {
    super.onNewIntent(intent)
    intent = newIntent
}
```

### 5. Approve the order through the Payments SDK

Approve the order using your `CardClient`.

Call `cardClient.approveOrder()` to approve the order, and then handle results:

```kotlin
private fun approveMyOrder(cardRequest: CardRequest) {
  val result = cardClient.approveOrder(this, cardRequest)
}

fun onApproveOrderSuccess(result: CardResult) {
  // order was successfully approved and is ready to be captured/authorized (see step 6)
}

fun onApproveOrderFailure(error: PayPalSDKError) {
  // inspect `error` for more information
}

fun onApproveOrderCanceled() {
  // 3DS flow was canceled
}

fun onApproveOrderThreeDSecureWillLaunch() {
  // 3DS flow will launch
}

fun onApproveOrderThreeDSecureDidFinish() {
  // user successfully completed 3DS authentication
}
```

### 6. Capture/Authorize the order

If you receive a successful result in the client-side flow, you can then capture or authorize the order. 

Call `authorize` to place funds on hold:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

Call `capture` to capture funds immediately:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

## Test and Go Live

### 1. Test the Card integration

- [PayPal Developer: 3D Secure test scenarios](https://developer.paypal.com/docs/checkout/advanced/customize/3d-secure/test/)

### 2. Go live with your integration

Follow [these instructions](https://developer.paypal.com/api/rest/production/) to prepare your integration to go live.
