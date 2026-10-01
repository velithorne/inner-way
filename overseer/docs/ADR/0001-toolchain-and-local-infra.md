# ADR 0001: Monorepo toolchain and local infrastructure

- Status: accepted (pending user acceptance of the P0 gate)
- Date: 2026-10-01
- Phase: P0

## Context

Build package v1.1 §2 fixes the stack (React + TypeScript + Vite, Fastify, PostgreSQL, Redis +
BullMQ, S3-compatible storage, Zod, Drizzle, Vitest + Playwright). P0 only needs the spine:
workspace, strict TypeScript, quality commands, local infra, a health endpoint and secret
safety. CLAUDE.md rule 3 requires every added dependency to be explained.

## Decisions

### 1. Location

OVERSEER lives in `overseer/` inside the existing `velithorne/inner-way` repository, which
already contains an unrelated Expo app at its root. `overseer/` is a self-contained pnpm
workspace (its own `pnpm-workspace.yaml` and lockfile), so nothing at the repository root is
touched. It can be lifted into its own repository unchanged.

### 2. Workspace packages ship TypeScript source

`packages/*` export `./src/index.ts` directly ("just-in-time" packages). Vite, Vitest, tsx and
tsup consume them without a per-package build, which removes build-ordering problems for
lint/typecheck/test. `apps/web` is built by Vite; `apps/api` is bundled by tsup with
`@overseer/*` inlined (`noExternal`), producing a single `dist/server.js`.

### 3. TypeScript pinned to 6.0.x

`typescript-eslint` 8.71 declares `typescript >=4.8.4 <6.1.0`. TypeScript 7.x was current at
the time of writing but would be an unmet peer, so `typescript` is `^6.0.3`. Revisit when
typescript-eslint supports 7.

### 4. Local S3-compatible storage is Adobe S3Mock, not MinIO

The spec asks for "S3-compatible local object storage" (it does not name MinIO). MinIO's
official images are no longer published on Docker Hub (`minio/minio` returns "not found") and
quay.io was not reachable from the build environment, so S3Mock (`adobe/s3mock`, pinned to
`5.2.3`) is used. It creates the bucket at start-up from an env var, so no init container is
needed.

**Consequence:** S3Mock does not validate credentials or IAM policies, so credential
mistakes cannot be caught by the local object store. Production uses a real S3-compatible
service (P18), and the S3 adapter is the only code that talks to it.

**Amendment (2026-10-01, found by running the real stack).** The first compose definition
mounted a named volume at `/data` and set `COM_ADOBE_TESTING_S3MOCK_STORE_ROOT=/data`. S3Mock
runs as a non-root user (uid 1000) and a fresh named volume is root-owned, so every bucket
operation failed with HTTP 500 and `/health` reported `objectStore: HTTP_500`. The volume and the
store-root/retain settings were removed (storage is ephemeral; the bucket is recreated at each
start) and a `wget` healthcheck against the documented always-available `/favicon.ico` was added
so `docker compose up --wait` and the CI readiness polling are meaningful. A persistent object
store volume can return in a later phase, with correct ownership.

### 5. Local safety guard

When `OVERSEER_ENV=local`, the API refuses to start if `DATABASE_URL`, `REDIS_URL` or
`S3_ENDPOINT` point at a non-local host (spec §10.2: no production credentials/resources in
local). Config errors list variable names only, never values.

## Dependencies added (and why)

| Package                                                                                  | Scope          | Reason                                                          |
| ---------------------------------------------------------------------------------------- | -------------- | --------------------------------------------------------------- |
| `typescript`                                                                             | root dev       | strict typechecking (§2)                                        |
| `eslint`, `@eslint/js`, `typescript-eslint`, `globals`                                   | root dev       | lint, including type-aware rules such as `no-floating-promises` |
| `prettier`                                                                               | root dev       | formatting                                                      |
| `vitest`                                                                                 | root dev       | unit/integration tests (§2)                                     |
| `@types/node`                                                                            | root dev       | Node typings for API and scripts                                |
| `zod`                                                                                    | contracts, api | shared runtime schemas and env validation (§2)                  |
| `fastify`                                                                                | api            | HTTP API (§2)                                                   |
| `pg`                                                                                     | api            | Postgres connectivity probe (Drizzle arrives in P1)             |
| `ioredis`                                                                                | api            | Redis connectivity probe (also BullMQ's client in P2)           |
| `@aws-sdk/client-s3`                                                                     | api            | real S3 `HeadBucket` probe against any S3-compatible endpoint   |
| `tsx`, `tsup`, `@types/pg`                                                               | api dev        | dev runner, production bundle, typings                          |
| `react`, `react-dom`, `vite`, `@vitejs/plugin-react`, `@types/react`, `@types/react-dom` | web            | frontend shell (§2)                                             |

Not added in P0 (deferred to their phases): Drizzle ORM, BullMQ, Playwright, React Flow, any
Shopify/Printify/supplier/AI SDK.

## Alternatives considered

- Per-package `tsc` builds with project references: more moving parts and ordering rules for
  no P0 benefit.
- Short-lived-connection health probes vs. long-lived pools: probes open a fresh connection so
  `/health` reflects whether a new connection can be made now, with no lifecycle to manage.
  Real pools arrive with Drizzle in P1.
- RustFS / SeaweedFS / LocalStack instead of S3Mock: RustFS is preview-only, SeaweedFS needs a
  multi-process setup, LocalStack has licensing/auth friction.
