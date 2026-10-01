import type { HealthResponse } from '@overseer/contracts';
import type { FastifyInstance } from 'fastify';
import type { Config } from '../config';
import { overallStatus, runHealthChecks, type DependencyCheck } from '../services/health-service';

export interface HealthRouteDeps {
  config: Pick<Config, 'OVERSEER_ENV' | 'HEALTH_CHECK_TIMEOUT_MS'>;
  checks: readonly DependencyCheck[];
  now?: () => Date;
}

/** `GET /health` - 200 when Postgres, Redis and the object store are reachable, else 503. */
export function registerHealthRoute(app: FastifyInstance, deps: HealthRouteDeps): void {
  const now = deps.now ?? (() => new Date());

  app.get('/health', async (_request, reply) => {
    const checks = await runHealthChecks(deps.checks, deps.config.HEALTH_CHECK_TIMEOUT_MS);
    const status = overallStatus(checks);
    const body: HealthResponse = {
      status,
      service: 'overseer-api',
      environment: deps.config.OVERSEER_ENV,
      time: now().toISOString(),
      checks,
    };
    return reply
      .code(status === 'ok' ? 200 : 503)
      .header('cache-control', 'no-store')
      .send(body);
  });
}
