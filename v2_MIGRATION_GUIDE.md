# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. CardPayments
1. PayPalWebPayments
1. PayPalNativePayments

### CardPayments

We have refactored the `CardClient` API to improve the developer experience.

#### Activity Reference no Longer Required in CardClient Constructor

The `CardClient` constructor no longer requires an activity reference. We require an activity reference only when we're launching a Chrome Custom Tab, e.g. `CardClient.presentAuthChallenge()`.

##### Version 2: The New Way

The updated `CardClient` constructor is less restrictive. For example, it should now be easier to construct a `CardClient` within a Jetpack `ViewModel`:

```kotlin
// GOOD: v2
val config = CoreConfig("<CLIENT_ID>", Environment.LIVE)
val cardClient = CardClient(requireContext(), config)
```

##### Version 1: The Old Way

The v1 `CardClient` constructor requires an activity reference to register a lifecycle observer for the SDK to parse incoming deep links internally.

Automatic parsing of deep links can have a positive affect on the developer experience, but we've found that internal deep link parsing can be problematic for some app architectures:

```kotlin
// BAD: v1
val config = CoreConfig("<CLIENT_ID>", Environment.LIVE)
val cardClient = CardClient(requireActivity(), config)
```

</details>

### PayPalWebPayments

We have refactored the `PayPalWebClient` API to improve the developer experience.

### PayPalNativePayments

We have removed `PayPalNativeClient` and all associated classes because the PayPal Native Checkout dependency this module uses has been sunset.