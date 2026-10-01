# OVERSEER - Claude Code operating contract

Source of truth: _OVERSEER AI Commerce Command-Centre - Complete Build Package v1.1_ (§0, §11).
This file restates the rules that govern every Claude Code session in this directory.

## Canonical project rules (§0)

- **Human authority is absolute.** Agents may research, propose, generate and prepare;
  irreversible or external actions require the policy-defined gate.
- No agent may publish a Shopify product, create a supplier purchase order, release a bulk
  payment, send a Printify order to production, spend advertising money, post marketing
  content, or change another agent's policy unless the corresponding capability is explicitly
  enabled.
- Every meaningful state transition emits an **append-only receipt**. No receipt = the
  transition did not happen.
- Every product has lineage: source evidence -> research finding -> strategy concept ->
  design/specification version -> validation -> human approval -> fulfilment route ->
  publication -> shipment/performance observations.
- Approved/LOCKED design versions and approved/LOCKED supplier specifications are
  **immutable**. A change creates a new version and requires re-validation.
- Agents never infer approval from silence, timeout, UI closure, previous approval, supplier
  history, or a similar product.
- External integrations are adapters behind interfaces. Core workflow logic runs against
  fakes in tests.
- Development proceeds in local vertical slices. Shopify, Printify and supplier/3PL writes
  stay **disabled** until their named gates are accepted.
- No autonomous ad spend in v1. Marketing produces drafts and campaign packages only.
- No copying competitor artwork and no counterfeit/branded-imitation sourcing.
- **Secrets never enter prompts, logs, receipts, screenshots, source control, browser
  localStorage or agent memory.**
- Any action that spends money must declare currency, amount, beneficiary/provider, purpose,
  subject version and approval record before execution.

## Operating contract for every session (§11)

1. Work only on the named phase.
2. Read `docs/gates/<phase>.md` before editing.
3. Do not add dependencies without explaining why (record them in an ADR).
4. Do not touch production integration credentials.
5. Never enable external writes or spending unless the phase explicitly authorises it.
6. Run the phase test command before claiming completion.
7. Produce an evidence report: files changed, tests, migrations, known gaps.
8. **STOP after the gate report.** Do not begin the next phase.
9. The user performs visual and physical-sample acceptance; Claude does not self-approve.
10. If a gate fails, fix only the gate failure and rerun the relevant tests.

The standard prompt wrapper is in `docs/runbooks/phase-prompt-wrapper.md`.

## Repository conventions

- pnpm workspace, TypeScript `strict`, ESM. Workspace packages ship TypeScript source
  (`exports` -> `src/index.ts`); only deployables (`apps/*`) have a build step.
- Core commands (run from `overseer/`):
  `pnpm lint` - `pnpm format:check` - `pnpm typecheck` - `pnpm test` - `pnpm build` -
  `pnpm secrets:scan` - `pnpm check` (runs all but build) - `pnpm infra:up` / `pnpm infra:down` -
  `pnpm gate:p0:health` (live-infrastructure proof; needs `pnpm build` and `pnpm infra:up`).
- Schema or contract changes require an ADR (`docs/ADR/`). Any change to approval or spending
  semantics requires workflow + security tests and manual acceptance.
- Never commit `.env` or any real credential. `.env.example` holds fake/local values only.
  `pnpm secrets:scan` must stay green.
- Tests must not make network calls to external providers. Local infra (Postgres/Redis/S3
  stand-in) is reached only from the explicit health/integration paths.

## Current phase

P0 - Repository & Safety Spine. See `docs/gates/P0.md`. Phases are tracked in the build
package §12; do not start P1 until the user explicitly accepts the P0 gate.
