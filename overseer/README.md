# OVERSEER - AI Commerce Command-Centre

Build package v1.1. Current phase: **P0 - Repository & Safety Spine** (see `docs/gates/P0.md`).

OVERSEER visualises an AI commerce workflow as a connected industrial facility. Agents are
workers, not authorities: the backend enforces deterministic workflow state, human approval
gates and receipts. Read `CLAUDE.md` for the operating rules.

## Layout

```
apps/api            Fastify API (config, /health)
apps/web            Vite + React shell
packages/contracts  shared Zod schemas
packages/workflow   state machine           (scaffold - P1)
packages/receipts   append-only receipts    (scaffold - P1)
packages/test-fixtures  fakes shared by tests
infra/docker        container assets        (placeholder - P18)
docs/               ADRs, contracts, gates, runbooks, supplier specs
scripts/            repository tooling (secret scanner)
```

## Quick start

See `docs/runbooks/local-development.md`.

```bash
pnpm install --frozen-lockfile
cp .env.example .env
pnpm infra:up && pnpm dev:api
pnpm check
```

No Shopify, Printify, supplier, 3PL, AI-generation, procurement or marketing integration exists
in this phase, and external writes/spending are not possible.
