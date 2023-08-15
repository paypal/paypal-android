# Fraud Protection

The FraudProtection module in the PayPal SDK enables merchants to collect user device data and associate it with payment transactions to reduce the risk of processing a fraudulent transaction.

Follow these steps to add fraud protection:

1. [Setup a PayPal Developer Account](#setup-a-paypal-developer-account)
2. [Add FraudProtection Module](#add-fraudprotection-module)
3. [Go Live](#go-live)

## Setup a PayPal Developer Account

You will need to set up authorization to use the PayPal Payments SDK. 
Follow the steps in [Get Started](https://developer.paypal.com/api/rest/#link-getstarted) to create a client ID. 

You will need a server integration to create an order and capture funds using [PayPal Orders v2 API](https://developer.paypal.com/docs/api/orders/v2). 
For initial setup, the `curl` commands below can be used in place of a server SDK.

## Add FraudProtection Module

### 1. Add the Payments SDK FraudProtection module to your app

![Maven Central](https://img.shields.io/maven-central/v/com.paypal.android/fraud-protection?style=for-the-badge) ![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.paypal.android/fraud-protection?server=https%3A%2F%2Foss.sonatype.org&style=for-the-badge)

In your app's `build.gradle` file, add the following dependency:

```groovy
dependencies {
   implementation "com.paypal.android:fraud-protection:<CURRENT-VERSION>"
}
```

### 2. Initiate the Payments SDK

Create a `CoreConfig` using a [client id](https://developer.paypal.com/api/rest/):

```kotlin
val config = CoreConfig("<CLIENT_ID>", environment = Environment.SANDBOX)
```

Create a `PayPalDataCollector` to retrieve a PayPal Client Metadata ID (CMID). You can use a CMID when calling Authorize or Capture to add fraud protection to your transactions.

```kotlin
val payPalDataCollector = PayPalDataCollector(config)
val cmid = payPalDataCollector.getClientMetadataId(androidContext)
```

### 3. Send **PayPal-Client-Metadata-Id** Header on Capture / Authorize

To enable fraud protection, add a `PayPal-Client-Metadata-Id` HTTP header to your call to Capture (or Authorize), and set its value to the `cmid` value obtained in the previous step:

```bash
# for capture
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/capture' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--header 'PayPal-Client-Metadata-Id: <CMID>' \
--data-raw ''

# for authorize
curl --location --request POST 'https://api.sandbox.paypal.com/v2/checkout/orders/<ORDER_ID>/authorize' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <ACCESS_TOKEN>' \
--header 'PayPal-Client-Metadata-Id: <CMID>' \
--data-raw ''
```

## Go Live

Follow [these instructions](https://developer.paypal.com/api/rest/production/) to prepare your integration to go live.

