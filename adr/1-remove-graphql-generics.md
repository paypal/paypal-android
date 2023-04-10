# Remove GraphQL Generics

**Status: Proposed**

## Context

We have [limited test coverage][1] in our GraphQL portion of the codebase. The way it's [currently written][2] does have the benefit of being DRY, allthough it does come with a cost of being difficult to test.

Here is a snippet of the abstract base class `Query`:

> <img src="./figure-query-abstract-base-class.png" height="400" alt="Query Abstract Base Class Source Code">

The current implementation also relies heavily on Generics for strong typing which can make the code difficult to read and maintain. The design is very similar to the iOS implementation. Generics are used on iOS to conform to a `Codable` interface for JSON parsing. There is no `Codable` equivalent on Android.

## Decision

We should refactor the GraphQL portion of the code base to be centered around the actual JSON request. Each GraphQL API class can enforce strong typing by parsing the response internally.

## Consequences

The code will become less DRY, but the task to create a JSONObject GraphQL query isn't overly complex. In this scenario, we may benefit less from DRY code than we do from having readible code that's easy to follow.

[1]: https://github.com/paypal/Android-SDK/blob/1fa0b256c00dc0b95872c21cc4865e6f58d4dd88/CorePayments/src/test/java/com/paypal/android/corepayments/graphql/fundingEligibility/FundingEligibilityQueryTest.kt#L12
[2]: https://github.com/paypal/Android-SDK/blob/1fa0b256c00dc0b95872c21cc4865e6f58d4dd88/CorePayments/src/main/java/com/paypal/android/corepayments/graphql/fundingEligibility/FundingEligibilityQuery.kt#L10
