# Accepting PayPal Payments

The PayPal module in the PayPal SDK enables PayPal payments in your app.

Follow these steps to add PayPal payments:

1. [Setup a PayPal Developer Account](#setup-a-paypal-developer-account)
1. [Add PayPal Module](#add-paypal-module)
1. [Test and go live](#test-and-go-live)

## Setup a PayPal Developer Account

You will need to set up authorization to use the PayPal Payments SDK. 
Follow the steps in [Get Started](https://developer.paypal.com/api/rest/#link-getstarted) to create a client ID and generate an access token. 

You will need a server integration to create an order to capture funds using the [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2).
For initial setup, the `curl` commands below can be used as a reference for making server-side RESTful API calls.

## Add PayPal Module

### 1. Add the Payments SDK  to your app

In your `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:paypal-web-checkout:1.0.0"
}
```

### 2. Configure your app to handle browser switching

The PayPal SDK redirects users to a web interface to complete the PayPal payment flow. After a user has completed the flow, a custom URL scheme is used to return control back to your app.

Edit your app's `AndroidManifest.xml` to include an `intent-filter` and set the `android:scheme` on the Activity that will be responsible for handling the deep link back into the app:

```xml
<activity android:name="com.company.app.MyPaymentsActivity"
    android:exported="true"
    android:launchMode="singleTop">
    ...
    <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <data android:scheme="custom-url-scheme"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>
    </intent-filter>
</activity>
```

### 3. Initiate the Payments SDK

Create a `CoreConfig` using your client ID from the PayPal Developer Portal:

```kotlin
val config = CoreConfig("<CLIENT_ID>", environment = Environment.SANDBOX)
```

Set a return URL using the custom scheme you configured in the `ActivityManifest.xml` [step 2](#2-configure-your-app-to-handle-browser-switching):

```kotlin
val returnUrl = "custom-url-scheme"
```

Create a `PayPalWebCheckoutClient` to approve an order with a PayPal payment method:

```kotlin
val payPalWebCheckoutClient = PayPalWebCheckoutClient(requireActivity(), config, returnUrl)
```

Set a listener on the client to receive payment flow callbacks:

```kotlin
payPalWebCheckoutClient.listener = object : PayPalWebCheckoutListener {

    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        // order was successfully approved and is ready to be captured/authorized (see step 7)
    }

    override fun onPayPalWebFailure(error: PayPalSDKError) {
        // handle the error
    }

    override fun onPayPalWebCanceled() {
        // the user canceled the flow
    }
}
```

### 4. Create an order

When a user initiates a payment flow, call `v2/checkout/orders` to create an order and obtain an order ID:

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

The `id` field of the response contains the order ID to pass to your client.

### 5. Create a request object for launching the PayPal flow

Configure your `PayPalWebCheckoutRequest` and include the order ID generated in [step 4](#4-create-an-order):

```kotlin
val payPalWebCheckoutRequest = PayPalWebCheckoutRequest("<ORDER_ID>")
```

You can also specify one of the follwing funding sources for your order: `PayPal` (default), `PayLater` or `PayPalCredit`.
> For more information on PayPal Pay Later go to: https://developer.paypal.com/docs/checkout/pay-later/us/

### 6. Approve the order using the PayPal SDK

To start the PayPal Web Checkokut flow, call `payPalWebCheckoutClient.start(payPalWebCheckoutRequest)`.

When a user completes the PayPal payment flow successfully, the result will be returned to the listener set in [step 4](#4-initiate-the-payments-sdk).

### 7. Capture/Authorize the order

After receiving a successful result from the `onPayPalWebSuccess` callback, you can now capture or authorize the order. 

Call `capture` to capture funds immediately:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

Call `authorize` to place funds on hold:

```bash
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```

## Test and go live

### 1. Test the PayPal integration

Follow the [Create sandbox account](https://developer.paypal.com/api/rest/#link-createsandboxaccounts) instructions to create a PayPal test account.
When prompted to login with PayPal during the payment flow on your mobile app, you can log in with the test account credentials created above to complete the Sandbox payment flow. 

### 2. Go live with your integration

Follow [these instructions](https://developer.paypal.com/api/rest/production/) to prepare your integration to go live.
