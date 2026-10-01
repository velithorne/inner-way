import { createServer, type IncomingMessage, type Server, type ServerResponse } from 'node:http';
import type { AddressInfo } from 'node:net';
import { fakeLocalEnv } from '@overseer/test-fixtures';
import { afterEach, describe, expect, it } from 'vitest';
import { loadConfig, type Config } from '../config';
import { objectStoreCheck, postgresCheck, redisCheck } from './dependency-checks';
import { runHealthChecks } from './health-service';

/** Probe-adapter tests that need no real infrastructure: a stub S3 server and a closed port. */

const servers: Server[] = [];

function listen(handler: (req: IncomingMessage, res: ServerResponse) => void): Promise<number> {
  return new Promise((resolve) => {
    const server = createServer(handler);
    servers.push(server);
    server.listen(0, '127.0.0.1', () => resolve((server.address() as AddressInfo).port));
  });
}

async function closedPort(): Promise<number> {
  const port = await listen(() => undefined);
  const server = servers.pop();
  await new Promise<void>((resolve) => server?.close(() => resolve()));
  return port;
}

afterEach(async () => {
  await Promise.all(
    servers
      .splice(0)
      .map((server) => new Promise<void>((resolve) => server.close(() => resolve()))),
  );
});

function configFor(overrides: Record<string, string>): Config {
  return loadConfig(fakeLocalEnv({ HEALTH_CHECK_TIMEOUT_MS: '1500', ...overrides }));
}

async function resultFor(check: Parameters<typeof runHealthChecks>[0][number], key: string) {
  const checks = await runHealthChecks([check], 3000);
  return checks[key as keyof typeof checks];
}

describe('objectStoreCheck', () => {
  it('passes when the bucket exists (HEAD bucket -> 200)', async () => {
    const seen: string[] = [];
    const port = await listen((req, res) => {
      seen.push(`${req.method} ${req.url}`);
      res.writeHead(200).end();
    });
    const config = configFor({ S3_ENDPOINT: `http://127.0.0.1:${port}` });

    expect(await resultFor(objectStoreCheck(config), 'objectStore')).toMatchObject({
      status: 'ok',
    });
    expect(seen).toEqual(['HEAD /overseer-local/']);
  });

  it('fails with a safe code when the bucket is missing (404)', async () => {
    const port = await listen((_req, res) => res.writeHead(404).end());
    const config = configFor({ S3_ENDPOINT: `http://127.0.0.1:${port}` });

    const result = await resultFor(objectStoreCheck(config), 'objectStore');
    expect(result).toMatchObject({ status: 'fail', error: 'NotFound' });
  });

  it('fails when the endpoint is unreachable', async () => {
    const port = await closedPort();
    const config = configFor({ S3_ENDPOINT: `http://127.0.0.1:${port}` });

    const result = await resultFor(objectStoreCheck(config), 'objectStore');
    expect(result.status).toBe('fail');
  });
});

describe('postgresCheck', () => {
  it('fails with ECONNREFUSED when nothing is listening, without leaking the URL', async () => {
    const port = await closedPort();
    const config = configFor({
      DATABASE_URL: `postgres://overseer:overseer_local_only@127.0.0.1:${port}/overseer`,
    });

    const result = await resultFor(postgresCheck(config), 'postgres');
    expect(result).toMatchObject({ status: 'fail', error: 'ECONNREFUSED' });
    expect(JSON.stringify(result)).not.toContain('overseer_local_only');
  });
});

describe('redisCheck', () => {
  it('fails when nothing is listening', async () => {
    const port = await closedPort();
    const config = configFor({ REDIS_URL: `redis://127.0.0.1:${port}` });

    const result = await resultFor(redisCheck(config), 'redis');
    expect(result).toMatchObject({ status: 'fail', error: 'ECONNREFUSED' });
  });
});
