# Android SDK
###### tags: `android`,`sdk`

---
title: Integrate card payments in Android apps
contentType: SDK
productStatus: Current
sdkVersion: v5
date: 2022-07-01T17:28:35Z
---

> **Important:** The PayPal Mobile SDK is in beta release. Use our Github repo to [report feedback](https://github.com/paypal/Android-SDK/issues).

The PayPal Mobile SDK helps you accept [card](#link-integrate) and [PayPal](#link-integrate) payments in your Android app. Visit our open-source [GitHub repository](https://github.com/paypal/Android-SDK/) for implementation details.


<Container>
<!-- This section just adds columns for the text and the image. -->
<Row>
<Col>
<br />

**Buyer experience**
        
Use our customizable PayPal buttons alongside your own custom checkout UI to give your buyers an experience that aligns with your business branding.
        
        
</Col>
<Col>
<img src="https://www.paypalobjects.com/devdoc/sdk-android-mobile-phone.jpg" width="250" alt="PayPal Mobile SDK on Android"/></Col>
</Row>
</Container>


## Know before you code
First, create a [business sandbox account](https://developer.paypal.com/tools/sandbox/accounts/) to get started developing.


Next, log in to the [PayPal Developer Dashboard](https://developer.paypal.com/dashboard) using your newly created account to obtain:
* Client ID
* Client Secret

You will need these values to generate an access token in step 5, "Initiate the PayPal Mobile SDK for Android".


## 1. Enable your account to accept card payments

<!-- Note: this whole section is a boilerplate asset: `partial:partials/docs/sandbox/verify-acdc.en-XC`. We can edit it, but it will deviate from other pages that use this content.-->

Before you can accept card payments on your website, verify that your sandbox business account is enabled for advanced credit and debit cards (ACDC). To verify:

1. Log into the [PayPal Developer Dashboard](https://developer.paypal.com/dashboard), go to **My Apps & Credentials > Sandbox > REST API apps**, and select the name of your app.
2. Go to **Sandbox App Settings > App feature options > Accept payments** and select the **Advanced options** link to see if ACDC is enabled for your account.

3. If ACDC isn't enabled, select the **Advanced Credit and Debit** checkbox and select the "Save" link to enable ACDC.

    * If you created a sandbox business account through [sandbox.paypal.com](https://sandbox.paypal.com), and the ACDC status for the account shows as disabled, [complete the sandbox onboarding steps](https://www.sandbox.paypal.com/bizsignup/entry/product/ppcp) to enable ACDC.


## 2. Integrate

Click on the **Card payments**, **PayPal Web Payments** or **PayPal UI** tab to get started.

<ContentTabs>
<ContentTab label="Card payments">

**Tab: Card payments**

## Collect card details

Build and customize your own card fields to align with your branding. This section shows you how to collect debit and credit card payments using your own UI.

### 1. Add the `CardPayments` module to your app

Add the following dependency in the <code>build.gradle</code> file for your app:

```groovy
dependencies {
  implementation "com.paypal.android:card:<CURRENT-VERSION>"
}
```


### 2. Fetch an Access Token on your server
    
On your server, fetch an `ACCESS_TOKEN` using the [following instructions](https://developer.paypal.com/api/rest/authentication/).

> **Note:** This access token is only for the sandbox environment. When you're ready to go live, you need to get a live access token. To do so, replace the request sandbox URL with: https://api-m.paypal.com/v1/oauth2/token.



### 3. Create a CardClient

In your Android app, use the `ACCESS_TOKEN` you fetched on your server in step 2 to construct a `CoreConfig`. Construct a `CardClient` using your config object.

```kotlin
val config = CoreConfig("<ACCESS_TOKEN>", environment = Environment.SANDBOX)\n
val cardClient = CardClient(config)
```

### 4. Fetch an Order ID on your server
    
On your server, use the <a href="https://developer.paypal.com/docs/api/orders/v2">Orders v2 API</a> to create an `ORDER_ID`. Use your `ACCESS_TOKEN` from step 3 in the `Authorization` header.
    
The `intent` type that you specify must either be `AUTHORIZE` or `CAPTURE`. This must match the `intent` type you use to process your order at the end of the integration (step 8).

**cURL Request**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/' \
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
```
{
   "id":"<ORDER_ID>",
   "status":"CREATED"
}
```
When a buyer initiates the payment flow, you will need to send the `ORDER_ID` from your server to your client app.    

### 5. Create a card request
#### 5.1. Collect Card payment details
Collect a buyer's card details using your custom UI. Collecting a billing address is encouraged as it minimizes the need for issuing banks to present authentication challenges to customers.

Construct a `Card` object with the buyer's card details.

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
#### 5.2. Set up 3-D Secure

<a href="https://developer.paypal.com/api/nvp-soap/payflow/3d-secure-overview/">3-D Secure (3DS)</a> is enabled for all card payments to comply with [The Second Payment Services Directive (PSD2)](https://www.paypal.com/uk/webapps/mpp/PSD2?_ga=1.18434873.1625369690.1652045188). PSD2 is a European Union regulation that introduces <a href="https://www.ukfinance.org.uk/our-expertise/payments-and-innovation/strong-customer-authentication">Strong Customer Authentication (SCA)</a> and other security requirements.

Specify your SCA launch option type via the `sca` parameter in the `CardRequest` initializer:   
* `SCA.SCA_WHEN_REQUIRED` - this is enabled by default. This will only launch an SCA challenge when applicable.
* `SCA.SCA_ALWAYS` - you can optionally set this to require an SCA challenge for every card transaction.
    
#### 5.3. Set your app for browser switching
The `sca` challenge launches in a browser withing your application. You will need to provide a `returnUrl` so the browser returns to your application after the `sca` challenge finishes. 
    The `returnUrl` should have the following format: 
```
myapp://return_url
```
The `myapp://` portion of the `returnUrl` in the above snippet, is custom url scheme must also be registered in your app's `AndroidManifest.xml`.

Edit your app's `AndroidManifest.xml` to include an `intent-filter` and set the `android:scheme` on the Activity that will be responsible for handling the deep link back into the app. Also set the activity `launchMode` to `singleTop`:
> Note: `android:exported` is required if your app compile SDK version is API 31 (Android 12) or later.

```xml
<activity
    android:name=".MyCardPaymentActivity"
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
#### 5.4. Build a `CardRequest`

Once you have completed the steps above, build a `CardRequest` object as follows:
    
```kotlin
val cardRequest  = CardRequest(
    orderID = "<ORDER_ID>",
    card = card,
    returnUrl = "myapp://return_url", // custom url scheme needs to be configured in AndroidManifest.xml
    sca = SCA.SCA_ALWAYS // default value is SCA_WHEN_REQUIRED
)
```


### 6. Approve the order

Once your `CardRequest` has been constructed with the card details, call `cardClient.approveOrder()` to process the payment.

```kotlin
class MyCardPaymentActivity: FragmentActivity {
    
    fun cardCheckoutTapped(cardRequest: CardRequest) {
        cardClient.approveOrder(this, cardRequest)
    }
}
```
    
### 7. Implement `ApproveOrderListener`    

```kotlin=
class MyCardPaymentActivity: FragmentActivity, ApproveOrderListener {
    
    fun cardCheckoutTapped(cardRequest: CardRequest) {
        val result = cardClient.approveOrder(this, cardRequest)
    }
    
    fun setupCardClient() {
        cardClient.listener = this
    }
    
    fun onApproveOrderSuccess(result: CardResult) {
      // order was successfully approved and is ready to be captured/authorized (see step 6)
    }
    fun onApproveOrderFailure(error: PayPalSDKError) {
      // inspect 'error' for more information
    }
    fun onApproveOrderCanceled() {
      // 3DS flow was canceled
    }
    fun onApproveOrderThreeDSecureWillLaunch() {
      // 3DS flow will launch
    }
    fun onApproveOrderThreeDSecureDidFinish() {
      // 3D Secure auth did finish successfully
    }
}
```


### 8. Authorize and capture the order
    
When the PayPal Android SDK successfully calls `onApproveOrderSuccess` method, submit your `ORDER_ID` for authorization or capture.

Call the <a href="https://developer.paypal.com/docs/api/orders/v2/#orders_authorize"> <code>authorize</code> endpoint of the Orders V2 API</a> to place funds on hold:
    
**cURL**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```
    
Call the <a href="https://developer.paypal.com/docs/api/orders/v2/#orders_capture"><code>capture</code> endpoint of the Orders V2 API</a> to capture funds immediately:
    
**cURL**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```


### 9. Testing
    
Use our list of [test numbers for various card brands](https://developer.paypal.com/docs/checkout/advanced/customize/3d-secure/test/) to test your integration. We also offer additional 3D Secure [test cards](https://developer.paypal.com/docs/checkout/advanced/customize/3d-secure/test/) cases.


</ContentTab>
<ContentTab label="PayPal Web Payments">

**Tab: PayPal Web Payments**

## PayPal Checkout Lite flow
With `PayPalWebPayments` you can have PayPal Checkout in-app web flow. Its launches the checkout experience in a browser within your application, significantlly reducing the size of the SDK. Follow this steps to integrate `PayPalWebPayments`:

### 1. Add `PayPalWebPayments` to your app

In your <code>build.gradle</code> file, add the following dependency:

```groovy
dependencies {
  implementation "com.paypal.android:paypal-wen-payments:<CURRENT-VERSION>"
}
```
### 2. Configure your app to handle browser switching

`PayPalWebPayments` redirects users to a web interface to complete the PayPal Checkout flow. After a user has completed the flow, a `custom-url-scheme` is used to return control back to your app.

Edit your app's `AndroidManifest.xml` to include an `intent-filter` and set the `android:scheme` on the Activity that will be responsible for handling the deep link back into the app. Also set the activity `launchMode` to `singleTop`:
> Note: `android:exported` is required if your app compile SDK version is API 31 (Android 12) or later.
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
Also, add `onNewIntent` to your activity:

```kotlin
override fun onNewIntent(newIntent: Intent?) {
    super.onNewIntent(intent)
    intent = newIntent
}
```
    
### 3. Fetch an Access Token on your server
    
On your server, fetch an `ACCESS_TOKEN` using the [following instructions](https://developer.paypal.com/api/rest/authentication/).

> **Note:** This access token is only for the sandbox environment. When you're ready to go live, you need to get a live access token. To do so, replace the request sandbox URL with: https://api-m.paypal.com/v1/oauth2/token.


### 4. Create a PayPalWebCheckoutClient
In your Android app, use the `ACCESS_TOKEN` you fetched on your server in step 3 to construct a `CoreConfig`.

```kotlin
val config = CoreConfig("<ACCESS_TOKEN>", environment = Environment.SANDBOX)
```
Set a return URL using the custom scheme you configured in the `ActivityManifest.xml` step 2:

```kotlin
val returnUrl = "custom-url-scheme"
```
Create a `PayPalWebCheckoutClient` to approve an order with a PayPal payment method:

```kotlin
val payPalWebCheckoutClient = PayPalWebCheckoutClient(requireActivity(), config, returnUrl)
```
Set a `PayPalWebCheckoutListener` on the client to receive payment flow callbacks:

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

### 5. Fetch an Order ID on your server
    
On your server, use the <a href="https://developer.paypal.com/docs/api/orders/v2">Orders v2 API</a> to create an `ORDER_ID`. Use your `ACCESS_TOKEN` from step 3 in the `Authorization` header.
    
The `intent` type that you specify must either be `AUTHORIZE` or `CAPTURE`. This must match the `intent` type you use to process your order at the end of the integration (step 8).

**cURL Request**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/' \
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
```
{
   "id":"<ORDER_ID>",
   "status":"CREATED"
}
```
When a buyer initiates the payment flow, you will need to send the `ORDER_ID` from your server to your client app.

### 6. Create a web checkout request
Configure your `PayPalWebCheckoutRequest` with the order ID generated in step 5. You can also specify one of the follwing funding sources for your order: `PayPal (default)`, `PayLater` or `PayPalCredit`.
> Click [here](https://developer.paypal.com/docs/checkout/pay-later/us/) for more information on PayPal Pay Later

```kotlin
val payPalWebCheckoutRequest = PayPalWebCheckoutRequest("<ORDER_ID>", fundingSource = PayPalWebCheckoutFundingSource.PAYPAL)
```

### 7. Approve the order

Once your `PayPalWebCheckoutRequest` has been call `payPalWebCheckoutClient.start()` to process the payment.

```kotlin
class MyCardPaymentActivity: FragmentActivity {
    
    fun payPalWebCheckoutTapped(payPalWebCheckoutRequest: PayPalWebCheckoutRequest) {
        payPalWebCheckoutClient.start(payPalWebCheckoutRequest)
    }
}
```

### 8. Authorize and capture the order
    
When the PayPal Android SDK successfully calls `onPayPalWebSuccess` method on `PayPalWebCheckoutListener`, submit your `ORDER_ID` for authorization or capture.

Call the <a href="https://developer.paypal.com/docs/api/orders/v2/#orders_authorize"> <code>authorize</code> endpoint of the Orders V2 API</a> to place funds on hold:
    
**cURL**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```
    
Call the <a href="https://developer.paypal.com/docs/api/orders/v2/#orders_capture"><code>capture</code> endpoint of the Orders V2 API</a> to capture funds immediately:
    
**cURL**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--data-raw ''
```
</ContentTab>
<ContentTab label="PayPal UI">
    
**Tab: PayPal UI**

## Use PayPal UI to add PayPal buttons 

The `PayPalUI` module provides a set of ready-made PayPal-branded buttons to create a more seamless integration with PayPal `web` and `native` payments.

Follow these steps to add PayPal buttons to your integration.

### 1. Add PayPalUI to your app

In your <code>build.gradle</code> file, add the following dependency:

```groovy
dependencies {
  implementation "com.paypal.android:paypal-ui:<CURRENT-VERSION>"
}
```


### 2. Create a PayPal button

The PayPalUI module gives you 3 buttons you can use in your application, with a set of customizations like color, edges, size, and labels:
  <ul>
    <li><code>PayPalButton</code>: generic PayPal button</li>
    <li><code>PayPalPayLater</code>: a PayPal button with a fixed PayLater label</li>
    <li><code>PayPalCredit</code>: a PayPal button with the PayPalCredit logo</li>
  </ul>

Add a <code>PayPalButton</code> to your layout XML:


```xml
<com.paypal.android.ui.paymentbutton.PayPalButton
android:id="@+id/paypal_button"
android:layout_width="match_parent"
android:layout_height="wrap_content" />
```

### 3. Reference the PayPal button

Reference the button in your code:

```java
val payPalButton = findViewById<PayPalButton>(R.id.paypal_button)
payPalButton.setOnClickListener {
        // start the PayPal web or native
}
```
</ContentTab>
</ContentTabs>


    
## Go live

* Before you go live, complete [production onboarding](https://www.paypal.com/bizsignup/entry/product/ppcp) to be eligible to process cards with your live PayPal account.
* [Go live](https://developer.paypal.com/api/rest/production/) by moving your application to PayPal's production environment.


## Explore additional features

When your integration is complete, learn how to add more capabilities:

<Row>
<Col xs={12} sm={5} className="payLater-wrapper">
<div>
<img src="https://www.paypalobjects.com/devdoc/3D_secure_icon.svg" className="checkoutIcon-img">
<p className="shippingOptions-p">3D Secure
<p className="cardDescription">
Reduce the risk of fraud by authenticating card holders.

<a href="https://developer.paypal.com/docs/checkout/advanced/customize/3d-secure/">View 3D Secure</a>
</div>
</Col>
</Row>

