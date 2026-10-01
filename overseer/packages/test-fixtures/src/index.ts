import type { HealthResponse } from '@overseer/contracts';

/** Fake, local-only environment for tests. None of these values are real credentials. */
export function fakeLocalEnv(overrides: Record<string, string> = {}): Record<string, string> {
  return {
    OVERSEER_ENV: 'local',
    API_HOST: '127.0.0.1',
    API_PORT: '4000',
    LOG_LEVEL: 'silent',
    DATABASE_URL: 'postgres://overseer:overseer_local_only@127.0.0.1:5432/overseer',
    REDIS_URL: 'redis://127.0.0.1:6379',
    S3_ENDPOINT: 'http://127.0.0.1:9000',
    S3_REGION: 'us-east-1',
    S3_BUCKET: 'overseer-local',
    S3_ACCESS_KEY_ID: 'overseer_local_only',
    S3_SECRET_ACCESS_KEY: 'overseer_local_only_secret',
    S3_FORCE_PATH_STYLE: 'true',
    ...overrides,
  };
}

/** A healthy `/health` response with every dependency up. */
export function fakeHealthResponse(overrides: Partial<HealthResponse> = {}): HealthResponse {
  return {
    status: 'ok',
    service: 'overseer-api',
    environment: 'local',
    time: '2026-10-01T00:00:00.000Z',
    checks: {
      postgres: { status: 'ok', latencyMs: 1 },
      redis: { status: 'ok', latencyMs: 1 },
      objectStore: { status: 'ok', latencyMs: 1 },
    },
    ...overrides,
  };
}
