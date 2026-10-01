import { describe, expect, it } from 'vitest';
import {
  overallStatus,
  runHealthChecks,
  toSafeErrorCode,
  type DependencyCheck,
} from './health-service';

const ok = (name: DependencyCheck['name']): DependencyCheck => ({
  name,
  run: () => Promise.resolve(),
});

const failing = (name: DependencyCheck['name'], error: Error): DependencyCheck => ({
  name,
  run: () => Promise.reject(error),
});

describe('runHealthChecks', () => {
  it('reports ok for every healthy dependency', async () => {
    const checks = await runHealthChecks([ok('postgres'), ok('redis'), ok('objectStore')], 500);
    expect(checks.postgres.status).toBe('ok');
    expect(checks.redis.status).toBe('ok');
    expect(checks.objectStore.status).toBe('ok');
    expect(overallStatus(checks)).toBe('ok');
  });

  it('reports a single failing dependency without hiding the others', async () => {
    const checks = await runHealthChecks(
      [
        ok('postgres'),
        failing('redis', Object.assign(new Error('x'), { code: 'ECONNREFUSED' })),
        ok('objectStore'),
      ],
      500,
    );
    expect(checks.postgres.status).toBe('ok');
    expect(checks.redis).toMatchObject({ status: 'fail', error: 'ECONNREFUSED' });
    expect(checks.objectStore.status).toBe('ok');
    expect(overallStatus(checks)).toBe('fail');
  });

  it('times out a hung dependency', async () => {
    const hung: DependencyCheck = { name: 'redis', run: () => new Promise<void>(() => undefined) };
    const checks = await runHealthChecks([ok('postgres'), hung, ok('objectStore')], 50);
    expect(checks.redis).toMatchObject({ status: 'fail', error: 'TIMEOUT' });
    expect(overallStatus(checks)).toBe('fail');
  });

  it('fails closed when a dependency has no registered check', async () => {
    const checks = await runHealthChecks([ok('postgres'), ok('redis')], 500);
    expect(checks.objectStore).toMatchObject({ status: 'fail', error: 'NOT_CONFIGURED' });
    expect(overallStatus(checks)).toBe('fail');
  });

  it('does not expose error messages that contain secrets', async () => {
    const leaky = new Error('password authentication failed for postgres://u:hunter2-fake@host/db');
    const checks = await runHealthChecks(
      [failing('postgres', leaky), ok('redis'), ok('objectStore')],
      500,
    );
    expect(JSON.stringify(checks)).not.toContain('hunter2');
    expect(checks.postgres).toMatchObject({ status: 'fail', error: 'UNKNOWN' });
  });
});

describe('toSafeErrorCode', () => {
  it('prefers a safe error code', () => {
    expect(toSafeErrorCode({ code: '28P01', message: 'secret' })).toBe('28P01');
  });

  it('falls back to a safe error name', () => {
    expect(toSafeErrorCode({ name: 'NotFound' })).toBe('NotFound');
  });

  it('reports the HTTP status for body-less SDK errors named "Unknown"', () => {
    expect(toSafeErrorCode({ name: 'Unknown', $metadata: { httpStatusCode: 403 } })).toBe(
      'HTTP_403',
    );
  });

  it('rejects codes with unsafe characters', () => {
    expect(toSafeErrorCode({ code: 'has spaces and=secret' })).toBe('UNKNOWN');
  });

  it('returns UNKNOWN for non-error values', () => {
    expect(toSafeErrorCode('boom')).toBe('UNKNOWN');
    expect(toSafeErrorCode(undefined)).toBe('UNKNOWN');
    expect(toSafeErrorCode(new Error('plain'))).toBe('UNKNOWN');
  });
});
