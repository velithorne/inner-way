import { HealthResponseSchema, type Environment, type HealthResponse } from '@overseer/contracts';

export type HealthState =
  | { kind: 'loading' }
  | { kind: 'ready'; health: HealthResponse }
  /** The API could not be reached at all (network error / proxy down). */
  | { kind: 'unreachable' }
  /** The API answered, but not with a valid /health contract body. */
  | { kind: 'invalid' };

/**
 * Fetches and validates /health. A 503 with a valid body is a normal "degraded" result
 * (some dependency is down), not an error, so the body is parsed regardless of status.
 */
export async function fetchHealth(
  fetchImpl: typeof fetch = fetch,
  url = '/api/health',
): Promise<HealthState> {
  let response: Response;
  try {
    response = await fetchImpl(url, { headers: { accept: 'application/json' } });
  } catch {
    return { kind: 'unreachable' };
  }

  let payload: unknown;
  try {
    payload = await response.json();
  } catch {
    return { kind: 'invalid' };
  }

  const parsed = HealthResponseSchema.safeParse(payload);
  return parsed.success ? { kind: 'ready', health: parsed.data } : { kind: 'invalid' };
}

export interface EnvironmentBanner {
  label: string;
  /** `prominent` is reserved for production, which must always be visibly marked (spec §10.2). */
  tone: 'quiet' | 'caution' | 'prominent';
}

export function environmentBanner(environment: Environment): EnvironmentBanner {
  switch (environment) {
    case 'local':
      return { label: 'LOCAL', tone: 'quiet' };
    case 'staging':
      return { label: 'STAGING', tone: 'caution' };
    case 'production':
      return { label: 'PRODUCTION', tone: 'prominent' };
  }
}
