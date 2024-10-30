# PayPal SDK Migration Guide

This guide highlights how to migrate to the latest version of the PayPal SDK.

## Table of Contents

1. CardPayments
1. PayPalWebPayments
1. PayPalNativePayments

### CardPayments

#### AuthLauncher

The `CardAuthLauncher.presentAuthChallenge` method now supports `ComponentActivity`. The return type for this method has been to a non-nullable `CardPresentAuthChallengeResult`.

Consider using a Kotlin `when` expression to determine the outcome of an auth challenge presentation:

```kotlin
// TODO: add presentAuthChallenge snippet
```

<details>
<summary>Comparison with v1</summary>

TODO: enter v1 comparison write-up
</details>



### PayPalWebPayments

### PayPalNativePayments
