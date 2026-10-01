import type { DependencyCheckResult, DependencyName, HealthResponse } from '@overseer/contracts';

export interface DependencyCheckContext {
  /** Aborted when the per-check timeout elapses. */
  signal: AbortSignal;
}

/** A probe of one backing service. It throws on failure; it never returns details. */
export interface DependencyCheck {
  name: DependencyName;
  run(context: DependencyCheckContext): Promise<void>;
}

const DEPENDENCY_NAMES: readonly DependencyName[] = ['postgres', 'redis', 'objectStore'];
const SAFE_ERROR_CODE = /^[A-Za-z0-9_.-]{1,64}$/;

/**
 * Reduces an arbitrary thrown value to a short code safe to expose.
 * Error *messages* are never used: driver messages can embed hosts, users or credentials.
 */
export function toSafeErrorCode(error: unknown): string {
  if (typeof error === 'object' && error !== null) {
    const { code, name, $metadata } = error as {
      code?: unknown;
      name?: unknown;
      $metadata?: { httpStatusCode?: unknown };
    };
    if (typeof code === 'string' && SAFE_ERROR_CODE.test(code)) return code;
    // AWS SDK errors for body-less responses (e.g. a 403 on HEAD) are named "Unknown";
    // the HTTP status is the useful, non-sensitive signal.
    const status = $metadata?.httpStatusCode;
    if (name === 'Unknown' && typeof status === 'number') return `HTTP_${status}`;
    if (typeof name === 'string' && SAFE_ERROR_CODE.test(name) && name !== 'Error') return name;
  }
  return 'UNKNOWN';
}

async function runOne(check: DependencyCheck, timeoutMs: number): Promise<DependencyCheckResult> {
  const started = performance.now();
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const elapsed = () => Math.round(performance.now() - started);

  const timedOut = new Promise<never>((_resolve, reject) => {
    controller.signal.addEventListener('abort', () => reject(new Error('timeout')), { once: true });
  });
  timedOut.catch(() => undefined);

  try {
    const work = check.run({ signal: controller.signal });
    // A check that outlives its timeout must not surface as an unhandled rejection later.
    work.catch(() => undefined);
    await Promise.race([work, timedOut]);
    return { status: 'ok', latencyMs: elapsed() };
  } catch (error) {
    return {
      status: 'fail',
      latencyMs: elapsed(),
      error: controller.signal.aborted ? 'TIMEOUT' : toSafeErrorCode(error),
    };
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Runs every check concurrently. Fails closed: a dependency with no registered check is
 * reported as failed rather than silently omitted.
 */
export async function runHealthChecks(
  checks: readonly DependencyCheck[],
  timeoutMs: number,
): Promise<HealthResponse['checks']> {
  const entries = await Promise.all(
    DEPENDENCY_NAMES.map(async (name): Promise<[DependencyName, DependencyCheckResult]> => {
      const check = checks.find((candidate) => candidate.name === name);
      if (!check) return [name, { status: 'fail', latencyMs: 0, error: 'NOT_CONFIGURED' }];
      return [name, await runOne(check, timeoutMs)];
    }),
  );
  return Object.fromEntries(entries) as HealthResponse['checks'];
}

export function overallStatus(checks: HealthResponse['checks']): HealthResponse['status'] {
  return Object.values(checks).every((result) => result.status === 'ok') ? 'ok' : 'fail';
}
