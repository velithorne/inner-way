import { HeadBucketCommand, S3Client } from '@aws-sdk/client-s3';
import { Redis } from 'ioredis';
import pg from 'pg';
import type { Config } from '../config';
import type { DependencyCheck } from './health-service';

/**
 * Real connectivity probes for the three P0 backing services. Each opens a short-lived
 * connection so /health reflects whether a *new* connection can be made right now, and
 * there is no connection lifecycle to manage.
 *
 * These are infrastructure probes only. No external commerce integration (Shopify, Printify,
 * suppliers, 3PLs) exists in P0.
 */

export function postgresCheck(config: Pick<Config, 'DATABASE_URL' | 'HEALTH_CHECK_TIMEOUT_MS'>) {
  return {
    name: 'postgres',
    async run() {
      const client = new pg.Client({
        connectionString: config.DATABASE_URL,
        connectionTimeoutMillis: config.HEALTH_CHECK_TIMEOUT_MS,
        query_timeout: config.HEALTH_CHECK_TIMEOUT_MS,
      });
      // Without a listener, a late socket error would crash the process.
      client.on('error', () => undefined);
      try {
        await client.connect();
        await client.query('SELECT 1');
      } finally {
        client.end().catch(() => undefined);
      }
    },
  } satisfies DependencyCheck;
}

export function redisCheck(config: Pick<Config, 'REDIS_URL' | 'HEALTH_CHECK_TIMEOUT_MS'>) {
  return {
    name: 'redis',
    async run() {
      const redis = new Redis(config.REDIS_URL, {
        lazyConnect: true,
        enableOfflineQueue: false,
        maxRetriesPerRequest: 0,
        retryStrategy: () => null,
        connectTimeout: config.HEALTH_CHECK_TIMEOUT_MS,
        commandTimeout: config.HEALTH_CHECK_TIMEOUT_MS,
      });
      // With retries disabled, connect() rejects with a generic "Connection is closed"; the
      // underlying socket error (e.g. ECONNREFUSED) only arrives on the 'error' event.
      let socketError: unknown;
      redis.on('error', (error: unknown) => {
        socketError = error;
      });
      try {
        await redis.connect();
        const reply = await redis.ping();
        if (reply !== 'PONG')
          throw Object.assign(new Error('unexpected reply'), { code: 'BAD_PING' });
      } catch (error) {
        throw socketError ?? error;
      } finally {
        redis.disconnect();
      }
    },
  } satisfies DependencyCheck;
}

export function objectStoreCheck(
  config: Pick<
    Config,
    | 'S3_ENDPOINT'
    | 'S3_REGION'
    | 'S3_BUCKET'
    | 'S3_ACCESS_KEY_ID'
    | 'S3_SECRET_ACCESS_KEY'
    | 'S3_FORCE_PATH_STYLE'
  >,
) {
  return {
    name: 'objectStore',
    async run({ signal }) {
      const client = new S3Client({
        endpoint: config.S3_ENDPOINT,
        region: config.S3_REGION,
        forcePathStyle: config.S3_FORCE_PATH_STYLE,
        credentials: {
          accessKeyId: config.S3_ACCESS_KEY_ID,
          secretAccessKey: config.S3_SECRET_ACCESS_KEY,
        },
        maxAttempts: 1,
      });
      try {
        await client.send(new HeadBucketCommand({ Bucket: config.S3_BUCKET }), {
          abortSignal: signal,
        });
      } finally {
        client.destroy();
      }
    },
  } satisfies DependencyCheck;
}

export function createDependencyChecks(config: Config): DependencyCheck[] {
  return [postgresCheck(config), redisCheck(config), objectStoreCheck(config)];
}
