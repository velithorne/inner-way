import { EnvironmentSchema } from '@overseer/contracts';
import { z } from 'zod';

/**
 * Hosts accepted for backing services when `OVERSEER_ENV=local`.
 * Includes docker-compose service names. Anything else is treated as a possible
 * non-local (staging/production) resource and rejected (spec §10.2: no production
 * credentials or resources in local development).
 */
const LOCAL_HOSTS = new Set([
  'localhost',
  '127.0.0.1',
  '[::1]',
  'postgres',
  'redis',
  'objectstore',
  'host.docker.internal',
]);

const SERVICE_URL_KEYS = ['DATABASE_URL', 'REDIS_URL', 'S3_ENDPOINT'] as const;

const booleanString = z.enum(['true', 'false']).transform((value) => value === 'true');

export const ConfigSchema = z
  .object({
    OVERSEER_ENV: EnvironmentSchema.default('local'),
    LOG_LEVEL: z
      .enum(['fatal', 'error', 'warn', 'info', 'debug', 'trace', 'silent'])
      .default('info'),
    API_HOST: z.string().min(1).default('127.0.0.1'),
    API_PORT: z.coerce.number().int().min(1).max(65535).default(4000),
    HEALTH_CHECK_TIMEOUT_MS: z.coerce.number().int().min(100).max(30_000).default(2000),

    DATABASE_URL: z.url({ protocol: /^postgres(ql)?$/ }),
    REDIS_URL: z.url({ protocol: /^rediss?$/ }),

    S3_ENDPOINT: z.url({ protocol: /^https?$/ }),
    S3_REGION: z.string().min(1).default('us-east-1'),
    S3_BUCKET: z.string().min(3).max(63),
    S3_ACCESS_KEY_ID: z.string().min(1),
    S3_SECRET_ACCESS_KEY: z.string().min(1),
    S3_FORCE_PATH_STYLE: booleanString.default(true),
  })
  .superRefine((config, ctx) => {
    if (config.OVERSEER_ENV !== 'local') return;
    for (const key of SERVICE_URL_KEYS) {
      const hostname = new URL(config[key]).hostname;
      if (!LOCAL_HOSTS.has(hostname)) {
        ctx.addIssue({
          code: 'custom',
          path: [key],
          message: 'must point at a local host when OVERSEER_ENV=local',
        });
      }
    }
  });

export type Config = z.infer<typeof ConfigSchema>;

/**
 * Raised when the environment is invalid. The message lists variable names and rule
 * violations only - never the offending values, which may be secrets.
 */
export class ConfigError extends Error {
  constructor(public readonly problems: readonly string[]) {
    super(`Invalid configuration:\n${problems.map((p) => `  - ${p}`).join('\n')}`);
    this.name = 'ConfigError';
  }
}

export function loadConfig(env: Readonly<Record<string, string | undefined>>): Config {
  const result = ConfigSchema.safeParse(env);
  if (result.success) return result.data;
  throw new ConfigError(
    result.error.issues.map((issue) => `${issue.path.join('.') || '(root)'}: ${issue.message}`),
  );
}
