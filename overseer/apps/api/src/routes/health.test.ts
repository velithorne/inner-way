import { HealthResponseSchema } from '@overseer/contracts';
import { fakeLocalEnv } from '@overseer/test-fixtures';
import { afterEach, describe, expect, it } from 'vitest';
import { buildApp } from '../app';
import { loadConfig } from '../config';
import type { DependencyCheck } from '../services/health-service';

const config = loadConfig(fakeLocalEnv({ HEALTH_CHECK_TIMEOUT_MS: '200' }));
const FIXED_NOW = new Date('2026-10-01T12:00:00.000Z');

const ok = (name: DependencyCheck['name']): DependencyCheck => ({
  name,
  run: () => Promise.resolve(),
});

const apps: ReturnType<typeof buildApp>[] = [];
function appWith(checks: DependencyCheck[]) {
  const app = buildApp({ config, checks, now: () => FIXED_NOW });
  apps.push(app);
  return app;
}

afterEach(async () => {
  await Promise.all(apps.splice(0).map((app) => app.close()));
});

describe('GET /health', () => {
  it('returns 200 and a contract-valid body when every dependency is up', async () => {
    const app = appWith([ok('postgres'), ok('redis'), ok('objectStore')]);
    const response = await app.inject({ method: 'GET', url: '/health' });

    expect(response.statusCode).toBe(200);
    expect(response.headers['cache-control']).toBe('no-store');
    const body = HealthResponseSchema.parse(response.json());
    expect(body).toMatchObject({
      status: 'ok',
      service: 'overseer-api',
      environment: 'local',
      time: '2026-10-01T12:00:00.000Z',
    });
  });

  it('returns 503 and names the failing dependency', async () => {
    const down: DependencyCheck = {
      name: 'postgres',
      run: () => Promise.reject(Object.assign(new Error('nope'), { code: 'ECONNREFUSED' })),
    };
    const app = appWith([down, ok('redis'), ok('objectStore')]);
    const response = await app.inject({ method: 'GET', url: '/health' });

    expect(response.statusCode).toBe(503);
    const body = HealthResponseSchema.parse(response.json());
    expect(body.status).toBe('fail');
    expect(body.checks.postgres).toMatchObject({ status: 'fail', error: 'ECONNREFUSED' });
    expect(body.checks.redis.status).toBe('ok');
  });

  it('returns 503 when a dependency check is missing (fails closed)', async () => {
    const app = appWith([ok('postgres'), ok('redis')]);
    const response = await app.inject({ method: 'GET', url: '/health' });
    expect(response.statusCode).toBe(503);
  });

  it('never includes connection strings or credentials in the response', async () => {
    const leaky: DependencyCheck = {
      name: 'objectStore',
      run: () => Promise.reject(new Error(`denied for ${config.S3_SECRET_ACCESS_KEY}`)),
    };
    const app = appWith([ok('postgres'), ok('redis'), leaky]);
    const response = await app.inject({ method: 'GET', url: '/health' });

    for (const secret of [
      config.S3_SECRET_ACCESS_KEY,
      config.S3_ACCESS_KEY_ID,
      config.DATABASE_URL,
      'overseer_local_only',
    ]) {
      expect(response.body).not.toContain(secret);
    }
  });

  it('generates a correlation id and echoes a well-formed inbound one', async () => {
    const app = appWith([ok('postgres'), ok('redis'), ok('objectStore')]);

    const generated = await app.inject({ method: 'GET', url: '/health' });
    expect(generated.headers['x-correlation-id']).toMatch(/^[0-9a-f-]{36}$/);

    const inbound = '0b2c5c0e-3f5e-4b1e-9d52-6f1d6d8d2f10';
    const echoed = await app.inject({
      method: 'GET',
      url: '/health',
      headers: { 'x-correlation-id': inbound },
    });
    expect(echoed.headers['x-correlation-id']).toBe(inbound);
  });

  it('replaces a malformed inbound correlation id', async () => {
    const app = appWith([ok('postgres'), ok('redis'), ok('objectStore')]);
    const response = await app.inject({
      method: 'GET',
      url: '/health',
      headers: { 'x-correlation-id': 'evil\ninjected log line' },
    });
    expect(response.headers['x-correlation-id']).toMatch(/^[0-9a-f-]{36}$/);
  });

  it('logs structured JSON carrying correlation_id, with credentials redacted', async () => {
    const lines: string[] = [];
    const app = buildApp({
      config: loadConfig(fakeLocalEnv({ LOG_LEVEL: 'info' })),
      checks: [ok('postgres'), ok('redis'), ok('objectStore')],
      logStream: { write: (line) => void lines.push(line) },
    });
    apps.push(app);

    // Fastify does not log headers by default; force them into a log line so the test
    // exercises the redaction config rather than passing vacuously.
    app.get('/_log-headers', (request) => {
      request.log.info({ headers: request.headers }, 'headers logged');
      return {};
    });

    const inbound = '0b2c5c0e-3f5e-4b1e-9d52-6f1d6d8d2f10';
    await app.inject({
      method: 'GET',
      url: '/_log-headers',
      headers: {
        'x-correlation-id': inbound,
        authorization: 'Bearer super-secret-token-123',
        cookie: 'session=super-secret-cookie-456',
      },
    });

    const records = lines.map((line) => JSON.parse(line) as Record<string, unknown>);
    expect(records.length).toBeGreaterThan(0);
    expect(records.every((record) => record.correlation_id === inbound)).toBe(true);
    const raw = lines.join('');
    expect(raw).toContain('[Redacted]');
    expect(raw).not.toContain('super-secret-token-123');
    expect(raw).not.toContain('super-secret-cookie-456');
  });
});
