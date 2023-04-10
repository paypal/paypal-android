# Remove GraphQL Generics

Status: Proposed

## Context

We have limited coverage in our GraphQL portion of the codebase. The way it's currently written does have the benefit of being DRY, but at the cost of being difficult to test. The current implementation also relies heavily on Generics for strong typing which can make the code difficult to read and maintain.

## Decision

We should refactor the GraphQL portion of the code base to be centered around the actual JSON request. Each GraphQL API class can enforce strong typing by parsing the response internally.

## Consequences

The code will become less DRY, but the task to create a JSONObject GraphQL query isn't overly complex. In this scenario, we may benefit less from DRY code than we do from having readible code that's easy to follow.

