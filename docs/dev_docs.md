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

Click on the **Card payments** or **PayPal Wallet** tab to get started.

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
        val result = cardClient.approveOrder(this, cardRequest)
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
<ContentTab label="PayPal Wallet">

**Tab: PayPal Wallet**

## Use PayPal UI to streamline creating frontend PayPal buttons

PayPal's default integration provides a set of ready-made PayPal-branded buttons to accept debit and credit card payments. You can apply custom styles to these buttons to change how they show up on your site.

Follow these steps to add PayPal buttons to your integration.

### 1. Add PayPalUI to your app

In your <code>build.gradle</code> file, add the following dependency:

```groovy
dependencies {
  implementation "com.paypal.android:paypal-ui:<CURRENT-VERSION>"
}
```


### 2. Create a PayPal button

The PayPalUI module gives you 3 buttons you can use on your site, with a set of customizations like color, edges, size, and labels:
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
        // start the PayPal flow
}
```

    
### 4. Add the PayPal Mobile SDK for Android to your app

Add the following dependency in the <code>build.gradle</code> file for your app:

```groovy
dependencies {
  implementation "com.paypal.android:paypal-web-checkout:<CURRENT-VERSION>"
}
```


### 5. Configure your app to handle browser switching

The PayPal payment flow uses a browser switch. You need to register a custom URL scheme to allow web applications to deep link back into your app. These flows include PayPal checkout and card checkout flows that result in a 3D Secure challenge.

Edit your app's <code>AndroidManifest.xml</code> to include an <code>intent-filter</code> and set the <code>android:scheme</code> on your Activity to route the deep link back to your app:

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


### 6. Initiate the PayPal Mobile SDK for Android

Request an <code>ACCESS_TOKEN</code> using the <code>CLIENT_ID</code> and <code>CLIENT_SECRET</code> from your app in the PayPal Developer Portal dashboard. You can also get an <code>ACCESS_TOKEN</code> using our Authentication API. <a href="https://developer.paypal.com/api/rest/authentication/">Learn more about authentication through PayPal</a>.

**cURL**
```bash
curl -X POST 'https://api-m.sandbox.paypal.com/v1/oauth2/token' \\\n -u $CLIENT_ID:$CLIENT_SECRET \\\n -H 'Content-Type: application/x-www-form-urlencoded' \\\n -d '"grant_type=client_credentials&response_type=token&return_authn_schemes=true"'
```

**Ruby**
```bash
require 'net/http'
require 'uri'
require 'json'\n
uri = URI.parse("https://api-m.sandbox.paypal.com/v1/oauth2/token")
request = Net::HTTP::Post.new(uri)
request.content_type = "application/x-www-form-urlencoded"
request["Authorization"] = "Basic base64(<CLIENT_ID>:<CLIENT_SECRET>)"
request.body = JSON.dump("grant_type=client_credentials&response_type=token&return_authn_schemes=true")\n
req_options = {
    use_ssl: uri.scheme == "https",
}\n
response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http|
    http.request(request)
end\n
# response.code
# response.body
```

**Python**
```bash
import requests\n
headers = {
  'Authorization': 'Basic base64(<CLIENT_ID>:<CLIENT_SECRET>)',
  'Content-Type': 'application/x-www-form-urlencoded',
}\n
data = '"grant_type=client_credentials&response_type=token&return_authn_schemes=true"'\n
response = requests.post('https://api-m.sandbox.paypal.com/v1/oauth2/token', headers = headers, data = data)
```

**NodeJS (request)**
```javascript
var request = require('request');\n
var headers = {
  'Authorization': 'Basic base64(<CLIENT_ID>:<CLIENT_SECRET>)',
  'Content-Type': 'application/x-www-form-urlencoded'
};\n
var dataString = '"grant_type=client_credentials&response_type=token&return_authn_schemes=true"';\n
var options = {
  url: 'https://api-m.sandbox.paypal.com/v1/oauth2/token',
  method: 'POST',
  headers: headers,
  body: dataString
};\n
function callback(error, response, body) {
  if (!error && response.statusCode == 200) {
    console.log(body);
  }
}\n
request(options, callback);
```

**Java**
```bash
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;\n
class Main {\n
  public static void main(String[] args) throws IOException {
    URL url = new URL("https://api-m.sandbox.paypal.com/v1/oauth2/token");
    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setRequestMethod("POST");\n
    httpConn.setRequestProperty("Authorization", "Basic base64(<CLIENT_ID>:<CLIENT_SECRET>)");
    httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");\n
    httpConn.setDoOutput(true);
    OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
    writer.write("\"grant_type=client_credentials&response_type=token&return_authn_schemes=true\"");
    writer.flush();
    writer.close();
    httpConn.getOutputStream().close();\n
    InputStream responseStream = httpConn.getResponseCode() / 100 == 2
      ? httpConn.getInputStream()
      : httpConn.getErrorStream();
    Scanner s = new Scanner(responseStream).useDelimiter("\\A");
    String response = s.hasNext() ? s.next() : "";
    System.out.println(response);
  }
}
```

**Go**
```javascript
package main
import(
  "fmt"
  "io/ioutil"
  "log"
  "net/http"
  "strings"
)
func main() {
    client: = &http.Client {}
    var data = strings.NewReader(\`"grant_type=client_credentials&response_type=token&return_authn_schemes=true"\`)
    req, err := http.NewRequest("POST", "https://api-m.sandbox.paypal.com/v1/oauth2/token", data)
    if err != nil {
      log.Fatal(err)
    }
    req.Header.Set("Authorization", "Basic base64(<CLIENT_ID>:<CLIENT_SECRET>)")
    req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
    resp, err := client.Do(req)
    if err != nil {
      log.Fatal(err)
    }
    defer resp.Body.Close()
    bodyText, err := ioutil.ReadAll(resp.Body)
    if err != nil {
      log.Fatal(err)
    }
    fmt.Printf("%s", bodyText)
}
```

> **Note:** This access token is only for the sandbox environment. When you're ready to go live, you'll need to grab the live access token. To do so, replace the request sandbox URL with: https://api-m.paypal.com/v1/oauth2/token.


### 7. Use `CoreConfig` to create `PayPalWebCheckoutClient`

Create a <code>CoreConfig</code> using the <code>ACCESS_TOKEN</code> you requested from the PayPal Developer Dashboard in step 6, "Initiate the PayPal Mobile SDK for Android". Set the return URL from the URL scheme you configured in the <code>ActivityManifest.xml</code> during step 5, "Configure your app to handle browser switching". Create a <code>PayPalClient</code> to approve an order with a PayPal payment method. Then set a listener on your <code>PayPalWebCheckoutClient</code> to handle the results.

```kotlin
val config = CoreConfig("<ACCESS_TOKEN>", environment = Environment.SANDBOX)\n
val returnUrl = "custom-url-scheme"\n
val payPalWebCheckoutClient = PayPalWebCheckoutClient(requireActivity(), config, returnUrl)\n
payPalWebCheckoutClient.listener = object : PayPalWebCheckoutListener {
  override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
  // order was successfully approved and is ready to be captured/authorized (see step 8)
  }
  override fun onPayPalWebFailure(error: PayPalSDKError) {
  // handle the error
  }
  override fun onPayPalWebCanceled() {
  // the user canceled the flow
  }
}
```


### 8. Create an order

When a buyer enters the payment flow, call the <a href="https://developer.paypal.com/docs/api/orders/v2">Orders v2 API</a> to create an order and obtain an order ID:

**cURL**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/' \\\n --header 'Content-Type: application/json' \\\n --header 'Authorization: Bearer <ACCESS_TOKEN>' \\\n --data-raw '{
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

**Ruby**
```ruby
require 'net/http'
require 'uri'\n
uri = URI.parse("https://api-m.sandbox.paypal.com/v2/checkout/orders/")
request = Net::HTTP::Post.new(uri)
request.content_type = "application/json"
request["Authorization"] = "Bearer <ACCESS_TOKEN>"\n
req_options = {
  use_ssl: uri.scheme == "https",
}\n
response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http |
    http.request(request)
end\n
# response.code
# response.body
```

**Python**
```python
import requests\n
headers = {
  'Authorization': 'Bearer <ACCESS_TOKEN>',
}\n
json_data = {
  'intent': '<CAPTURE|AUTHORIZE>',
  'purchase_units': [{
    'amount': {
      'currency_code': 'USD',
      'value': '5.00',
      },
    },
  ],
}\n
response = requests.post('https://api-m.sandbox.paypal.com/v2/checkout/orders/', headers = headers, json = json_data)
```

**NodeJS (request)**
```javascript
var request = require('request');\n
var headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <ACCESS_TOKEN>'
};\n
var dataString = '{
"intent": "<CAPTURE|AUTHORIZE>",
"purchase_units": [
    {
      "amount": {
        "currency_code": "USD",
        "value": "5.00"
      }
    }
  ]
}';\n
var options = {
  url: 'https://api-m.sandbox.paypal.com/v2/checkout/orders/',
  method: 'POST',
  headers: headers,
  body: dataString
};\n
function callback(error, response, body) {
  if (!error && response.statusCode == 200) {
    console.log(body);
  }
}\n
request(options, callback);'
```

**Java**
```java
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;\n
class Main {\n
  public static void main(String[] args) throws IOException {
    URL url = new URL("https://api-m.sandbox.paypal.com/v2/checkout/orders/");
    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setRequestMethod("POST");
    httpConn.setRequestProperty("Content-Type", "application/json");
    httpConn.setRequestProperty("Authorization", "Bearer <ACCESS_TOKEN>");
    httpConn.setDoOutput(true);
    OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
    writer.write("{\n      \"intent\": \"<CAPTURE|AUTHORIZE>\",\n      \"purchase_units\": [\n          {\n              \"amount\": {\n                  \"currency_code\": \"USD\",\n                  \"value\": \"5.00\"\n              }\n          }\n      ]\n  }");
    writer.flush();
    writer.close();
    httpConn.getOutputStream().close();
    InputStream responseStream = httpConn.getResponseCode() / 100 == 2
      ? httpConn.getInputStream()
      : httpConn.getErrorStream();
    Scanner s = new Scanner(responseStream).useDelimiter("\\A");
    String response = s.hasNext() ? s.next() : "";
    System.out.println(response);
  }
}
```

**Go**
```javascript
package main
import(
  "fmt"
  "io/ioutil"
  "log"
  "net/http"
  "strings"
)
func main() {
    client: = &http.Client {}
    var data = strings.NewReader(\`{
      "intent": "<CAPTURE|AUTHORIZE>",
      "purchase_units": [
          {
              "amount": {
                  "currency_code": "USD",
                  "value": "5.00"
              }
          }
      ]
  }\`)
  req, err := http.NewRequest("POST", "https://api-m.sandbox.paypal.com/v2/checkout/orders/", data)
  if err != nil {
    log.Fatal(err)
  }
  req.Header.Set("Content-Type", "application/json")
  req.Header.Set("Authorization", "Bearer <ACCESS_TOKEN>")
  resp, err := client.Do(req)
  if err != nil {
    log.Fatal(err)
  }
  defer resp.Body.Close()
  bodyText, err := ioutil.ReadAll(resp.Body)
  if err != nil {
    log.Fatal(err)
  }
  fmt.Printf("%s", bodyText)
}
```

The <code>id</code> field of the response passes the order ID to send to your client.

    
### 9. Create a request object for launching the PayPal flow

Configure your <code>PayPalWebCheckoutRequest</code> and include the order ID generated in step 6, "Create an order":

```kotlin
val payPalWebCheckoutRequest = PayPalWebCheckoutRequest("<ORDER_ID>")
```

You can also specify the funding source for your order, either <code>PayPal</code> (default), <code>PayLater</code>, or <code>PayPalCredit</code>. For more information go to the <a href="https://developer.paypal.com/docs/checkout/pay-later/us/">Pay Later</a> page.

When a buyer uses your application's UI to start a PayPal payment flow, the order is picked up by the listener that you created for `PayPalWebCheckoutClient` in step 5, "Use `CoreConfig` to create `CardClient`". Call `payPalClient.start(payPalWebCheckoutRequest)` to start the PayPal checkout web flow. When the buyer completes the PayPal payment flow, the result is returned to the `PayPalWebCheckoutClient` listener from step 5.

### 10. Authorize and capture the order

Once PayPal successfully returns your order, authorize and capture the order using the <a href="https://developer.paypal.com/docs/api/orders/v2/"><code>authorize</code> endpoint of the Orders V2 API</a>.

> **Note:** If you've already completed the "Authorize and capture the order" step from the "Card fields" card payment integration, you can use the same endpoints.

Call the <a href="https://developer.paypal.com/docs/api/orders/v2/#orders_authorize"> <code>authorize</code> endpoint of the Orders V2 API</a> to place funds on hold:

**cURL**
```bash
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \\\n --header 'Content-Type: application/json' \\\n --header 'Authorization: Bearer <ACCESS_TOKEN>' \\\n --data-raw ''
```

**Ruby**
```ruby
require 'net/http'
require 'uri'\n
uri = URI.parse("https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize")
request = Net::HTTP::Post.new(uri)
request.content_type = "application/json"
request["Authorization"] = "Bearer <ACCESS_TOKEN>"\n
req_options = {
  use_ssl: uri.scheme == "https",
}\n
response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http |
    http.request(request)
end\n
# response.code
# response.body
```

**Python**
```python
import requests\n
headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <ACCESS_TOKEN>',
}\n
response = requests.post('https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize', headers = headers)
```

**NodeJS (request)**
```javascript
var request = require('request');\n
var headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <ACCESS_TOKEN>'
};\n
var options = {
  url: 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize',
  method: 'POST',
  headers: headers
};\n
function callback(error, response, body) {
  if (!error && response.statusCode == 200) {
    console.log(body);
  }
}\n
request(options, callback);
```

**Java**
```java
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;\n
class Main {\n
  public static void main(String[] args) throws IOException {
    URL url = new URL("https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize");
    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setRequestMethod("POST");
    httpConn.setRequestProperty("Content-Type", "application/json");
    httpConn.setRequestProperty("Authorization", "Bearer <ACCESS_TOKEN>");
    InputStream responseStream = httpConn.getResponseCode() / 100 == 2
      ? httpConn.getInputStream()
      : httpConn.getErrorStream();
    Scanner s = new Scanner(responseStream).useDelimiter("\\A");
    String response = s.hasNext() ? s.next() : "";
    System.out.println(response);
  }
}
```

**Go**
```javascript
package main
import(
  "fmt"
  "io/ioutil"
  "log"
  "net/http"
)
func main() {
  client: = &http.Client {}
  req, err := http.NewRequest("POST", "https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize", nil)
  if err != nil {
    log.Fatal(err)
  }
  req.Header.Set("Content-Type", "application/json")
  req.Header.Set("Authorization", "Bearer <ACCESS_TOKEN>")
  resp, err := client.Do(req)
  if err != nil {
    log.Fatal(err)
  }
  defer resp.Body.Close()
  bodyText, err := ioutil.ReadAll(resp.Body)
  if err != nil {
    log.Fatal(err)
  }
  fmt.Printf("%s", bodyText)
}
```

Call the <a href="https://developer.paypal.com/docs/api/orders/v2/#orders_capture"><code>capture</code> endpoint of the Orders V2 API</a> to capture funds immediately:

**cURL**
```kotlin
curl --location --request POST 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \\\n --header 'Content-Type: application/json' \\\n --header 'Authorization: Bearer <ACCESS_TOKEN>' \\\n --data-raw ''
```

**Ruby**
```ruby
require 'net/http'
require 'uri'\n
uri = URI.parse("https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture")
request = Net::HTTP::Post.new(uri)
request.content_type = "application/json"
request["Authorization"] = "Bearer <ACCESS_TOKEN>"\n
req_options = {
  use_ssl: uri.scheme == "https",
}\n
response = Net::HTTP.start(uri.hostname, uri.port, req_options) do |http |
    http.request(request)
end\n
# response.code
# response.body
```

**Python**
```python
import requests\n
headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <ACCESS_TOKEN>',
}\n
response = requests.post('https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture', headers = headers)
```

**NodeJS (request)**
```javascript
var request = require('request');\n
var headers = {
  'Content-Type': 'application/json',
  'Authorization': 'Bearer <ACCESS_TOKEN>'
};\n
var options = {
  url: 'https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture',
  method: 'POST',
  headers: headers
};\n
function callback(error, response, body) {
  if (!error && response.statusCode == 200) {
    console.log(body);
  }
}\n
request(options, callback);
```

**Java**
```java
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;\n
class Main {\n
  public static void main(String[] args) throws IOException {
    URL url = new URL("https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture");
    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setRequestMethod("POST");
    httpConn.setRequestProperty("Content-Type", "application/json");
    httpConn.setRequestProperty("Authorization", "Bearer <ACCESS_TOKEN>");
    InputStream responseStream = httpConn.getResponseCode() / 100 == 2
      ? httpConn.getInputStream()
      : httpConn.getErrorStream();
    Scanner s = new Scanner(responseStream).useDelimiter("\\A");
    String response = s.hasNext() ? s.next() : "";
    System.out.println(response);
  }
}
```

**Go**
```javascript
package main
import(
  "fmt"
  "io/ioutil"
  "log"
  "net/http"
)
func main() {
  client: = &http.Client {}
  req, err := http.NewRequest("POST", "https://api-m.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture", nil)
  if err != nil {
    log.Fatal(err)
  }
  req.Header.Set("Content-Type", "application/json")
  req.Header.Set("Authorization", "Bearer <ACCESS_TOKEN>")
  resp, err := client.Do(req)
  if err != nil {
    log.Fatal(err)
  }
  defer resp.Body.Close()
  bodyText, err := ioutil.ReadAll(resp.Body)
  if err != nil {
    log.Fatal(err)
  }
  fmt.Printf("%s", bodyText)
}
```


### 11. Test purchases

To ensure that your PayPal Payments Standard payment buttons work correctly, PayPal recommends that you test them using the PayPal Sandbox before you place them on your live website.

[Learn more about testing your PayPal buttons on your website](https://developer.paypal.com/api/nvp-soap/paypal-payments-standard/ht-test-pps-buttons/#link-testbuttonsonyourwebsite).

When your tests are complete, log in to your [PayPal](https://www.paypal.com/?_ga=1.43245989.1625369690.1652045188) Business account and create the buttons for your website. For detailed instructions, see [Create a payment button](https://developer.paypal.com/api/nvp-soap/paypal-payments-standard/integration-guide/create-payment-button/).

> **Tip:** If you process payments that require [Strong Customer Authentication](https://www.paypal.com/uk/webapps/mpp/psd2) (SCA), you'll need to provide additional context about the transaction with [payment indicators](https://developer.paypal.com/docs/checkout/advanced/customize/sca-payment-indicators/).

### 12. Enable 3D Secure for your PayPal Wallet integration

If you're based in Europe, you may need to comply with [The Second Payment Services Directive (PSD2)](https://www.paypal.com/uk/webapps/mpp/PSD2?_ga=1.18434873.1625369690.1652045188). PayPal recommends that you include 3D Secure as part of your integration and also pass the cardholder's billing address as part of the transaction processing.

A seller needs to request Strong Consumer Authentication (SCA) for a <code>CardRequest</code> that requires buyers to provide additional authentication information using 3DS. To include SCA, add the following to <code>CardRequest</code>:

```kotlin
cardRequest.threeDSecureRequest = ThreeDSecureRequest(
  sca = SCA.SCA_ALWAYS,
  returnUrl = "myapp://return_url"
  cancelUrl = "myapp://cancel_url"
)
```


> **Important:** Notice the <code>myapp://</code> portion of the <code>returnUrl</code> and <code>cancelUrl</code> in the <code>ThreeDSecureRequest</code> code snippet. You also need to register the <code>myapp</code> custom URL scheme in your app's <code>AndroidManifest.xml</code>.

In the PayPal sandbox, you can use test cards to simulate various scenarios to generate a 3DS response in the order details response.

Get 3DS test cards on our [3D Secure test scenarios page](https://developer.paypal.com/docs/checkout/advanced/customize/3d-secure/test/).

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

