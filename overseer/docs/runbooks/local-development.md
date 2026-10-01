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

- Right after `pnpm infra:up`, `/health` may report `objectStore` failing for a few seconds
  (`ECONNREFUSED`) while the object store boots. Retry.

- Port already in use: change `POSTGRES_PORT` / `REDIS_PORT` / `S3_PORT` in `.env` and update
  the matching `DATABASE_URL` / `REDIS_URL` / `S3_ENDPOINT`.
- `pnpm dev:api` exits with "Invalid configuration": the message lists the variable names to fix.
