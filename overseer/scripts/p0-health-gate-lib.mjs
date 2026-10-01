// Pure helpers for the P0 live-infrastructure gate (scripts/p0-health-gate.mjs).
// Kept free of process/docker side effects so they can be unit-tested.

/** Error codes accepted for an object store that has been stopped (connection-level failures). */
export const OBJECT_STORE_DOWN_CODES = Object.freeze(['ECONNREFUSED', 'ECONNRESET', 'TIMEOUT']);

const DEPENDENCIES = /** @type {const} */ (['postgres', 'redis', 'objectStore']);
const SAFE_ERROR_CODE = /^[A-Za-z0-9_.-]{1,64}$/;

/** Parses `KEY=VALUE` lines (comments and blanks ignored). Quotes are not interpreted. */
export function parseEnvFile(text) {
  /** @type {Record<string, string>} */
  const env = {};
  for (const rawLine of text.split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const eq = line.indexOf('=');
    if (eq <= 0) continue;
    env[line.slice(0, eq).trim()] = line.slice(eq + 1).trim();
  }
  return env;
}

/** `docker compose version --short` -> true when the Compose major version is >= 2. */
export function isComposeV2OrNewer(shortVersion) {
  const match = /^v?(\d+)\./.exec(shortVersion.trim());
  return match !== null && Number(match[1]) >= 2;
}

/**
 * Repeatedly calls `probe` until it returns `{ done: true, value }`. Bounded: throws once
 * `timeoutMs` has elapsed, quoting the last observation so the failure is diagnosable.
 *
 * @template T
 * @param {() => Promise<{ done: boolean; value: T; observed?: string }>} probe
 * @param {{ timeoutMs: number; intervalMs: number; label: string;
 *           sleep?: (ms: number) => Promise<void>; now?: () => number }} options
 * @returns {Promise<{ value: T; attempts: number; elapsedMs: number }>}
 */
export async function pollUntil(probe, options) {
  const sleep = options.sleep ?? ((ms) => new Promise((resolve) => setTimeout(resolve, ms)));
  const now = options.now ?? (() => Date.now());
  const started = now();
  let attempts = 0;
  let lastObserved = 'no observation';
  for (;;) {
    attempts += 1;
    let result;
    try {
      result = await probe();
    } catch (error) {
      result = { done: false, value: undefined, observed: `probe threw ${errorCode(error)}` };
    }
    if (result.observed !== undefined) lastObserved = result.observed;
    if (result.done) return { value: result.value, attempts, elapsedMs: now() - started };
    if (now() - started >= options.timeoutMs) {
      throw new Error(
        `Timed out after ${options.timeoutMs} ms waiting for: ${options.label} ` +
          `(${attempts} attempts; last observation: ${lastObserved})`,
      );
    }
    await sleep(options.intervalMs);
  }
}

function errorCode(error) {
  const code = typeof error === 'object' && error !== null ? error.code : undefined;
  return typeof code === 'string' && SAFE_ERROR_CODE.test(code) ? code : 'error';
}

function checkOf(body, name) {
  return body && typeof body === 'object' && body.checks ? body.checks[name] : undefined;
}

/** Throws unless `body` is a /health response where every dependency is `ok`. */
export function assertAllHealthy(body) {
  const problems = [];
  if (body?.status !== 'ok') problems.push(`status is ${JSON.stringify(body?.status)}, not "ok"`);
  if (body?.service !== 'overseer-api') problems.push('service is not "overseer-api"');
  for (const name of DEPENDENCIES) {
    const check = checkOf(body, name);
    if (check?.status !== 'ok') {
      problems.push(`${name} is ${JSON.stringify(check?.status ?? 'missing')}, not "ok"`);
    } else if (typeof check.latencyMs !== 'number') {
      problems.push(`${name} has no numeric latencyMs`);
    }
  }
  if (problems.length > 0) throw new Error(`/health not fully healthy: ${problems.join('; ')}`);
}

/**
 * Throws unless `body` shows ONLY the object store failing, with an accepted non-secret
 * connection-level error code, and none of `secrets` anywhere in the response.
 *
 * @param {unknown} body
 * @param {{ secrets?: string[] }} [options]
 */
export function assertObjectStoreDown(body, options = {}) {
  const problems = [];
  const b = /** @type {any} */ (body);
  if (b?.status !== 'fail')
    problems.push(`overall status is ${JSON.stringify(b?.status)}, not "fail"`);
  const store = checkOf(b, 'objectStore');
  if (store?.status !== 'fail') {
    problems.push(`objectStore is ${JSON.stringify(store?.status ?? 'missing')}, not "fail"`);
  } else if (
    typeof store.error !== 'string' ||
    !SAFE_ERROR_CODE.test(store.error) ||
    !OBJECT_STORE_DOWN_CODES.includes(store.error)
  ) {
    problems.push(
      `objectStore.error ${JSON.stringify(store.error)} is not one of ${OBJECT_STORE_DOWN_CODES.join(', ')}`,
    );
  }
  for (const name of /** @type {const} */ (['postgres', 'redis'])) {
    if (checkOf(b, name)?.status !== 'ok') problems.push(`${name} should still be "ok"`);
  }
  const serialised = JSON.stringify(body);
  for (const secret of options.secrets ?? []) {
    if (secret && serialised.includes(secret))
      problems.push('response contains a configured secret value');
  }
  if (problems.length > 0)
    throw new Error(`/health during object-store outage: ${problems.join('; ')}`);
}

/**
 * Parses `docker compose ps --format json` output (NDJSON on current Compose, a JSON array
 * on older releases) into `{ service, state, health }` rows.
 */
export function parseComposePs(output) {
  const text = output.trim();
  if (!text) return [];
  let rows;
  try {
    const parsed = JSON.parse(text);
    rows = Array.isArray(parsed) ? parsed : [parsed];
  } catch {
    rows = text
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => JSON.parse(line));
  }
  return rows.map((row) => ({
    service: String(row.Service ?? ''),
    state: String(row.State ?? ''),
    health: String(row.Health ?? ''),
  }));
}

/** Services that are not yet running, or have a healthcheck that is not yet `healthy`. */
export function servicesNotReady(rows, required) {
  return required.filter((service) => {
    const row = rows.find((candidate) => candidate.service === service);
    if (!row || row.state !== 'running') return true;
    return row.health !== '' && row.health !== 'healthy';
  });
}
