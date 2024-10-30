# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. CardPayments
1. PayPalWebPayments
1. PayPalNativePayments

### CardPayments

We have refactored the `CardClient` API to improve the developer experience.

#### Activity Reference no Longer Required in CardClient Constructor

The `CardClient` constructor no longer requires an activity reference. An activity reference is now required _only_ when the SDK will attempt to launch a Chrome Custom Tab.

##### The Old Way: Version 1

The old `CardClient` constructor requires an activity reference to register lifecycle observers so the SDK can parse incoming deep links internally when the host application comes to the foregound.

Automatic parsing of deep links can have a positive affect on the developer experience, but we've found that internal deep link parsing can be problematic for some app architectures.

```kotlin
// BAD: v1
val config = CoreConfig("<CLIENT_ID>", Environment.LIVE)
val cardClient = CardClient(requireActivity(), config)
```

##### The New Way: Version 2

The new `CardClient` constructor is less restrictive. For example, it should now be easier to create a `CardClient` instance from within a Jetpack `ViewModel`:

```kotlin
// GOOD: v2
val config = CoreConfig("<CLIENT_ID>", Environment.LIVE)
val cardClient = CardClient(requireContext(), config)
```

### PayPalWebPayments

We have refactored the `PayPalWebClient` API to improve the developer experience.

### PayPalNativePayments

We have removed `PayPalNativeClient` and all associated classes because the PayPal Native Checkout dependency this module uses has been sunset.
