# Runbook: local development

Prerequisites: Node >= 22, pnpm 10 (`corepack enable`), Docker with Compose v2.

## First run

```bash
cd overseer
pnpm install --frozen-lockfile
cp .env.example .env            # fake/local values only - never put real credentials here
pnpm infra:up                   # docker compose up -d --wait  (Postgres, Redis, object store)
pnpm dev:api                    # http://127.0.0.1:4000
pnpm dev:web                    # http://127.0.0.1:5173 (proxies /api -> API)
```

Health check:

```bash
curl -s http://127.0.0.1:4000/health | jq
# 200 {"status":"ok", "checks": {"postgres":..., "redis":..., "objectStore":...}}
# 503 {"status":"fail", ...} if any dependency is down; `error` is a short code, never a secret
```

Stop everything: `pnpm infra:down` (add `-v` to also delete local data volumes).

## Quality commands

| Command             | Purpose                                                                   |
| ------------------- | ------------------------------------------------------------------------- |
| `pnpm lint`         | ESLint (type-aware)                                                       |
| `pnpm format:check` | Prettier check (`pnpm format` to fix)                                     |
| `pnpm typecheck`    | `tsc --noEmit` in every workspace project                                 |
| `pnpm test`         | Vitest across the workspace                                               |
| `pnpm build`        | Production builds: API bundle (`apps/api/dist`) and web (`apps/web/dist`) |
| `pnpm secrets:scan` | Secret scanner; prints rule/file/line only, never the value               |
| `pnpm check`        | format:check + lint + typecheck + test + secrets:scan                     |

## The P0 live-infrastructure gate (same proof CI runs)

```bash
pnpm install --frozen-lockfile
pnpm build
pnpm infra:up                 # canonical: docker compose up -d --wait
pnpm gate:p0:health           # 200 -> stop object store -> 503 -> restart -> 200
pnpm infra:down
```

Works unchanged in Windows PowerShell with Docker Desktop (Compose v2). It uses `.env` if present,
else the committed fake values in `.env.example`, and runs its own API on port 4010 so it does not
collide with `pnpm dev:api`. Evidence is written to `.data/p0-health-gate.json`. CI runs exactly
this (`.github/workflows/p0-gate.yml`).

## Manual checks for P0 acceptance

1. Fresh clone: `pnpm install --frozen-lockfile` succeeds with no production credentials.
2. `git check-ignore -v .env` reports it is ignored; `git status` never lists `.env`.
3. With `pnpm infra:up` and `pnpm dev:api`, `/health` returns 200 and all three checks `ok`.
4. `docker compose stop redis` -> `/health` returns 503 with `redis` failed; `docker compose
start redis` -> back to 200.
5. Set `OVERSEER_ENV=local` and `DATABASE_URL` to a non-local host -> the API refuses to start
   and prints the variable name only.
6. Open http://127.0.0.1:5173 and confirm the three dependency tiles show CONNECTED. (Visual
   acceptance is yours; this is a placeholder shell, not the facility UI.)

## Troubleshooting

- `pnpm infra:up` waits for the object store's healthcheck; if `/health` still shows `objectStore`
  failing for a moment after a restart, retry (the JVM-based store takes a few seconds).
- `objectStore: HTTP_500` means the object store is up but cannot use its storage; do not mount a
  root-owned volume into it (see ADR 0001 amendment).

- Port already in use: change `POSTGRES_PORT` / `REDIS_PORT` / `S3_PORT` in `.env` and update
  the matching `DATABASE_URL` / `REDIS_URL` / `S3_ENDPOINT`.
- `pnpm dev:api` exits with "Invalid configuration": the message lists the variable names to fix.
