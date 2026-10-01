import { z } from 'zod';

/** Deployment environments (spec §10.2). */
export const EnvironmentSchema = z.enum(['local', 'staging', 'production']);
export type Environment = z.infer<typeof EnvironmentSchema>;

/**
 * Result of probing a single infrastructure dependency.
 *
 * `error` is a short, sanitised code (e.g. `ECONNREFUSED`, `TIMEOUT`). It must never contain
 * a connection string, hostname-with-credentials or any other secret (spec §0: secrets never
 * enter logs, receipts or responses).
 */
export const DependencyCheckResultSchema = z.discriminatedUnion('status', [
  z.object({
    status: z.literal('ok'),
    latencyMs: z.number().nonnegative(),
  }),
  z.object({
    status: z.literal('fail'),
    latencyMs: z.number().nonnegative(),
    error: z.string().regex(/^[A-Za-z0-9_.-]{1,64}$/),
  }),
]);
export type DependencyCheckResult = z.infer<typeof DependencyCheckResultSchema>;

export const DependencyNameSchema = z.enum(['postgres', 'redis', 'objectStore']);
export type DependencyName = z.infer<typeof DependencyNameSchema>;

/** Response body of `GET /health`. HTTP 200 when `status` is `ok`, 503 otherwise. */
export const HealthResponseSchema = z.object({
  status: z.enum(['ok', 'fail']),
  service: z.literal('overseer-api'),
  environment: EnvironmentSchema,
  time: z.iso.datetime(),
  checks: z.object({
    postgres: DependencyCheckResultSchema,
    redis: DependencyCheckResultSchema,
    objectStore: DependencyCheckResultSchema,
  }),
});
export type HealthResponse = z.infer<typeof HealthResponseSchema>;
