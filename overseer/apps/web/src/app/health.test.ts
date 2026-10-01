import { fakeHealthResponse } from '@overseer/test-fixtures';
import { describe, expect, it } from 'vitest';
import { environmentBanner, fetchHealth } from './health';

const respond =
  (body: unknown, status = 200): typeof fetch =>
  () =>
    Promise.resolve(new Response(JSON.stringify(body), { status }));

describe('fetchHealth', () => {
  it('returns a validated healthy response', async () => {
    const state = await fetchHealth(respond(fakeHealthResponse()));
    expect(state).toEqual({ kind: 'ready', health: fakeHealthResponse() });
  });

  it('treats a 503 with a valid body as a degraded result, not an error', async () => {
    const degraded = fakeHealthResponse({
      status: 'fail',
      checks: {
        postgres: { status: 'fail', latencyMs: 5, error: 'ECONNREFUSED' },
        redis: { status: 'ok', latencyMs: 1 },
        objectStore: { status: 'ok', latencyMs: 1 },
      },
    });
    const state = await fetchHealth(respond(degraded, 503));
    expect(state).toEqual({ kind: 'ready', health: degraded });
  });

  it('reports unreachable when the network request fails', async () => {
    const down = (() => Promise.reject(new TypeError('failed to fetch'))) as typeof fetch;
    expect(await fetchHealth(down)).toEqual({ kind: 'unreachable' });
  });

  it('reports invalid for a body that violates the contract', async () => {
    expect(await fetchHealth(respond({ status: 'ok' }))).toEqual({ kind: 'invalid' });
  });

  it('reports invalid for a non-JSON body', async () => {
    const html = (() => Promise.resolve(new Response('<html>', { status: 502 }))) as typeof fetch;
    expect(await fetchHealth(html)).toEqual({ kind: 'invalid' });
  });
});

describe('environmentBanner', () => {
  it('always marks production prominently', () => {
    expect(environmentBanner('production')).toEqual({ label: 'PRODUCTION', tone: 'prominent' });
    expect(environmentBanner('staging').tone).toBe('caution');
    expect(environmentBanner('local').tone).toBe('quiet');
  });
});
