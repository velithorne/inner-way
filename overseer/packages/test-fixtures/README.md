# @overseer/test-fixtures

Fake/local-only fixtures shared by tests (spec §0: core logic runs against fakes).

- Everything here is fake. Never put real credentials, customer data or provider payloads in this package.
- **P0:** `fakeLocalEnv()` and `fakeHealthResponse()`.
