import { describe, expect, it } from 'vitest';
import {
  assertAllHealthy,
  assertObjectStoreDown,
  isComposeV2OrNewer,
  parseComposePs,
  parseEnvFile,
  pollUntil,
  servicesNotReady,
} from './p0-health-gate-lib.mjs';

const ok = { status: 'ok', latencyMs: 3 };
const healthy = () => ({
  status: 'ok',
  service: 'overseer-api',
  checks: { postgres: ok, redis: ok, objectStore: ok },
});
const storeDown = (error) => ({
  status: 'fail',
  service: 'overseer-api',
  checks: { postgres: ok, redis: ok, objectStore: { status: 'fail', latencyMs: 4, error } },
});

describe('assertAllHealthy', () => {
  it('accepts a fully healthy response', () => {
    expect(() => assertAllHealthy(healthy())).not.toThrow();
  });

  it('rejects when any dependency is not ok, naming it', () => {
    const body = healthy();
    body.checks.redis = { status: 'fail', latencyMs: 1, error: 'ECONNREFUSED' };
    body.status = 'fail';
    expect(() => assertAllHealthy(body)).toThrow(/redis/);
  });

  it('rejects an overall "ok" that hides a failed dependency', () => {
    const body = healthy();
    body.checks.objectStore = { status: 'fail', latencyMs: 1, error: 'X' };
    expect(() => assertAllHealthy(body)).toThrow(/objectStore/);
  });

  it('rejects garbage', () => {
    expect(() => assertAllHealthy(null)).toThrow();
    expect(() => assertAllHealthy({})).toThrow();
  });
});

describe('assertObjectStoreDown', () => {
  it('accepts only the object store failing with ECONNREFUSED', () => {
    expect(() => assertObjectStoreDown(storeDown('ECONNREFUSED'))).not.toThrow();
  });

  it('rejects an unexpected error code', () => {
    expect(() => assertObjectStoreDown(storeDown('HTTP_500'))).toThrow(/HTTP_500/);
  });

  it('rejects when postgres or redis also failed', () => {
    const body = storeDown('ECONNREFUSED');
    body.checks.postgres = { status: 'fail', latencyMs: 1, error: 'ECONNREFUSED' };
    expect(() => assertObjectStoreDown(body)).toThrow(/postgres should still be/);
  });

  it('rejects a healthy response (outage not observed)', () => {
    expect(() => assertObjectStoreDown(healthy())).toThrow(/overall status/);
  });

  it('rejects a response that leaks a configured secret', () => {
    const body = storeDown('ECONNREFUSED');
    body.leak = 'prefix-fake-secret-value';
    expect(() => assertObjectStoreDown(body, { secrets: ['fake-secret-value'] })).toThrow(/secret/);
  });
});

describe('pollUntil', () => {
  const clock = () => {
    let t = 0;
    return { now: () => t, sleep: (ms) => Promise.resolve((t += ms)) };
  };

  it('returns as soon as the probe is done', async () => {
    const c = clock();
    let calls = 0;
    const result = await pollUntil(() => Promise.resolve({ done: ++calls === 3, value: 'v' }), {
      timeoutMs: 1000,
      intervalMs: 100,
      label: 'x',
      ...c,
    });
    expect(result).toMatchObject({ value: 'v', attempts: 3, elapsedMs: 200 });
  });

  it('is bounded and quotes the last observation on timeout', async () => {
    const c = clock();
    await expect(
      pollUntil(() => Promise.resolve({ done: false, value: 0, observed: 'HTTP 503' }), {
        timeoutMs: 500,
        intervalMs: 100,
        label: 'healthy /health',
        ...c,
      }),
    ).rejects.toThrow(/Timed out after 500 ms waiting for: healthy \/health.*HTTP 503/);
  });

  it('treats a throwing probe as not-yet-ready, not as a crash', async () => {
    const c = clock();
    let calls = 0;
    const result = await pollUntil(
      () => {
        if (++calls < 3) throw Object.assign(new Error('boom'), { code: 'ECONNREFUSED' });
        return Promise.resolve({ done: true, value: 'up' });
      },
      { timeoutMs: 1000, intervalMs: 50, label: 'x', ...c },
    );
    expect(result.value).toBe('up');
  });
});

describe('compose helpers', () => {
  it('detects Compose v2+', () => {
    expect(isComposeV2OrNewer('2.29.7')).toBe(true);
    expect(isComposeV2OrNewer('v5.3.1')).toBe(true);
    expect(isComposeV2OrNewer('1.29.2')).toBe(false);
    expect(isComposeV2OrNewer('garbage')).toBe(false);
  });

  it('parses NDJSON and array forms of `compose ps --format json`', () => {
    const a = { Service: 'redis', State: 'running', Health: 'healthy' };
    const b = { Service: 'objectstore', State: 'running', Health: '' };
    expect(parseComposePs(`${JSON.stringify(a)}\n${JSON.stringify(b)}\n`)).toHaveLength(2);
    expect(parseComposePs(JSON.stringify([a, b]))).toHaveLength(2);
    expect(parseComposePs('')).toEqual([]);
  });

  it('reports services that are missing, stopped or unhealthy', () => {
    const rows = [
      { service: 'postgres', state: 'running', health: 'healthy' },
      { service: 'redis', state: 'running', health: 'starting' },
      { service: 'objectstore', state: 'exited', health: '' },
    ];
    expect(servicesNotReady(rows, ['postgres', 'redis', 'objectstore', 'ghost'])).toEqual([
      'redis',
      'objectstore',
      'ghost',
    ]);
    expect(servicesNotReady(rows.slice(0, 1), ['postgres'])).toEqual([]);
  });
});

describe('parseEnvFile', () => {
  it('parses values and ignores comments and blanks', () => {
    expect(parseEnvFile('# c\n\nA=1\nB = two words \nbad line\n')).toEqual({
      A: '1',
      B: 'two words',
    });
  });
});
