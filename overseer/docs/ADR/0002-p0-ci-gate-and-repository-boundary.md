# ADR 0002: P0 CI gate and repository boundary

- Status: accepted (CI gate); repository boundary **pending a user action** (see below)
- Date: 2026-10-01
- Phase: P0

## Context

P0 criterion 4 (`/health` against the real Postgres, Redis and object store, including an
outage) could not be fully proven in the build sandbox, and the project is being developed from
machines where Node, Git, Docker and pnpm cannot be installed. The user asked for the proof to
run on GitHub-hosted CI, without weakening `/health` and without mocks. OVERSEER is also meant to
become a standalone project rather than live inside the Biofield Scanner (`inner-way`) repo.

## Decision 1: one live-infrastructure gate, shared by CI and local use

- `scripts/p0-health-gate.mjs` (Node, cross-platform) performs the proof against the services
  from `docker-compose.yml`: start the API with the fake local config, `GET /health` -> 200 with
  all three dependencies ok, stop **only** `objectstore`, `GET /health` -> 503 with only
  `objectStore` failing and an accepted non-secret error code (`ECONNREFUSED`, `ECONNRESET`,
  `TIMEOUT`) and no configured secret anywhere in the body, restart it, wait, `GET /health` -> 200.
  Every wait is bounded (`pollUntil`), the API runs on a dedicated port (4010) so a developer's
  own dev server is never mistaken for it, and evidence is written to
  `.data/p0-health-gate.json` and the Actions job summary.
- The same compose file and the same canonical `pnpm infra:up` are used locally and in CI. Locally:
  `pnpm build && pnpm infra:up && pnpm gate:p0:health` (works in PowerShell too).
- The pure logic is unit-tested (`scripts/p0-health-gate-lib.test.mjs`); the orchestration is
  exercised for real by running it.

## Decision 2: workflow layout

- Canonical workflow: `.github/workflows/p0-gate.yml` **inside** the OVERSEER tree
  (`overseer/.github/workflows/p0-gate.yml` today). It uses `ubuntu-latest` (a full VM with a
  Docker daemon, not the slim runner), the Node version in `.nvmrc`, and the pnpm version in
  `package.json#packageManager`, and asserts the running versions equal the pins.
- GitHub only reads workflows from a repository root, so while OVERSEER lives in a subdirectory of
  `inner-way`, a temporary root copy (`.github/workflows/overseer-p0-gate.yml`) runs it. The two
  differ only in name, triggers, `OVERSEER_DIR` and `defaults.run.working-directory`. The copy is
  deleted when OVERSEER moves out.
- Actions are pinned by major tag (`@v4`), not commit SHA. Pin to SHAs when hardening (P17).
- Image pulls are retried a bounded number of times to absorb Docker Hub anonymous rate limits
  (observed during this work: 100 pulls/hour/IP, shared by the sandbox). If that proves
  insufficient in CI, the remedy is a registry mirror or authenticated pulls, not removing the
  real S3 check.

## Decision 3: repository boundary (standalone repository)

Goal: move `overseer/` to its own repository, keeping its history and every accepted P0 file,
without touching the Biofield Scanner project.

Safest GitHub-only path (no local machine needed):

1. Create an **empty private** repository (no README/licence/.gitignore) for OVERSEER. This is a
   one-time action that requires GitHub authorisation beyond this session's repository scope.
2. `git subtree split --prefix=overseer` produces a branch containing only the history of
   `overseer/`, with `overseer/` as the root (so `overseer/.github/workflows/` becomes
   `.github/workflows/`). Commit messages and authorship are preserved; commit SHAs change because
   the tree is rewritten.
3. Push that branch to the new repository's default branch; its CI runs the canonical workflow
   unchanged.
4. Only after the user accepts the new repository does anything get removed from `inner-way`
   (the temporary workflow copy and, at the user's discretion, `overseer/`). Nothing in the
   Biofield Scanner application is modified by this process.

Status is recorded in `docs/gates/P0.md`.

## Consequences

- The proof is reproducible by anyone with Docker, on CI or on a Windows PC.
- Evidence references the commit under test; after the split the same tree gets a new SHA, so the
  gate is re-run in the new repository for the final record.
- Two near-identical workflow files exist until the split completes.
