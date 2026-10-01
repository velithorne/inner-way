import { randomUUID } from 'node:crypto';
import Fastify, { LogController, type FastifyInstance } from 'fastify';
import type { Config } from './config';
import { registerHealthRoute } from './routes/health';
import type { DependencyCheck } from './services/health-service';

export interface AppDeps {
  config: Config;
  checks: readonly DependencyCheck[];
  now?: () => Date;
  /** Log destination; defaults to stdout. Tests inject a stream to assert on log content. */
  logStream?: { write(line: string): void };
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function buildApp({ config, checks, now, logStream }: AppDeps): FastifyInstance {
  const app = Fastify({
    logger: {
      level: config.LOG_LEVEL,
      // Secrets never enter logs (spec §0).
      // Covers headers logged at the top level or one level down (e.g. under `req`/`res`).
      redact: [
        'headers.authorization',
        'headers.cookie',
        'headers["set-cookie"]',
        '*.headers.authorization',
        '*.headers.cookie',
        '*.headers["set-cookie"]',
      ],
      ...(logStream ? { stream: logStream } : {}),
    },
    // Correlation IDs trace work across components (spec §2). An inbound ID is honoured only
    // when it is a well-formed UUID, so callers cannot inject arbitrary text into logs.
    requestIdHeader: false,
    logController: new LogController({ requestIdLogLabel: 'correlation_id' }),
    genReqId: (request) => {
      const inbound = request.headers['x-correlation-id'];
      return typeof inbound === 'string' && UUID.test(inbound) ? inbound : randomUUID();
    },
  });

  app.addHook('onSend', async (request, reply) => {
    reply.header('x-correlation-id', request.id);
  });

  registerHealthRoute(app, { config, checks, ...(now ? { now } : {}) });
  return app;
}
