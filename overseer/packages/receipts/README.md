# @overseer/receipts

Append-only receipt/audit repository (spec §4).

- **P0:** empty scaffold.
- **P1:** `Receipt` envelope, append-only repository, atomic receipt + state commit. Corrections are compensating receipts; no update/delete is exposed to the application role.
