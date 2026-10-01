#!/usr/bin/env node
// P0 live-infrastructure gate: proves GET /health against REAL Postgres, Redis and an
// S3-compatible object store started from docker-compose.yml (no mocks).
//
//   node scripts/p0-health-gate.mjs wait-infra   bounded wait for compose services (step 12)
//   node scripts/p0-health-gate.mjs [run]        steps 10 and 13-19 (default)
//
// Prerequisites: `pnpm build` and `pnpm infra:up`. Cross-platform (Linux/macOS/Windows).
// Uses fake local values only: `.env` if present, else `.env.example`.
//
// Evidence is printed, written to .data/p0-health-gate.json and, in GitHub Actions,
// appended to the job summary. Nothing printed contains a credential: /health responses carry
// only status, latency and short error codes, and the script asserts that.

import { spawn, spawnSync } from 'node:child_process';
import { appendFileSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import {
  assertAllHealthy,
  assertObjectStoreDown,
  isComposeV2OrNewer,
  parseComposePs,
  parseEnvFile,
  pollUntil,
  servicesNotReady,
} from './p0-health-gate-lib.mjs';

const ROOT = path.resolve(import.meta.dirname, '..');
const REQUIRED_SERVICES = ['postgres', 'redis', 'objectstore'];
const OBJECT_STORE_SERVICE = 'objectstore';
const API_ENTRY = path.join(ROOT, 'apps', 'api', 'dist', 'server.js');
const EVIDENCE_PATH = path.join(ROOT, '.data', 'p0-health-gate.json');
// A dedicated port so a developer's own `pnpm dev:api` (4000) can never be mistaken for ours.
const API_PORT = Number(process.env.P0_GATE_API_PORT ?? 4010);
const HEALTH_URL = `http://127.0.0.1:${API_PORT}/health`;

const BUDGET = {
  infraMs: 120_000,
  apiListenMs: 30_000,
  healthyMs: 90_000,
  outageMs: 30_000,
  recoveryMs: 120_000,
  intervalMs: 2_000,
};

const stamp = () => new Date().toISOString();
const log = (message) => console.log(`[${stamp()}] ${message}`);
const step = (n, title) => console.log(`\n=== STEP ${n}: ${title} ===`);

function compose(args) {
  const result = spawnSync('docker', ['compose', ...args], { cwd: ROOT, encoding: 'utf8' });
  return {
    status: result.status ?? -1,
    stdout: result.stdout ?? '',
    stderr: result.stderr ?? '',
    error: result.error,
  };
}

function loadFakeLocalEnv() {
  const file = existsSync(path.join(ROOT, '.env'))
    ? path.join(ROOT, '.env')
    : path.join(ROOT, '.env.example');
  return { file, values: parseEnvFile(readFileSync(file, 'utf8')) };
}

function portIsFree(port) {
  return new Promise((resolve) => {
    const socket = net.createConnection({ host: '127.0.0.1', port });
    socket.once('connect', () => (socket.destroy(), resolve(false)));
    socket.once('error', () => resolve(true));
  });
}

async function fetchHealth() {
  const response = await fetch(HEALTH_URL, {
    headers: { accept: 'application/json' },
    signal: AbortSignal.timeout(10_000),
  });
  let body;
  try {
    body = await response.json();
  } catch {
    body = undefined;
  }
  return { status: response.status, body, correlationId: response.headers.get('x-correlation-id') };
}

const compact = (body) =>
  body?.checks
    ? Object.entries(body.checks)
        .map(([name, c]) => `${name}=${c.status}${c.error ? `(${c.error})` : ''}`)
        .join(' ')
    : 'no body';

async function waitForInfra(label, timeoutMs = BUDGET.infraMs) {
  const outcome = await pollUntil(
    () => {
      const ps = compose(['ps', '--all', '--format', 'json']);
      if (ps.status !== 0) {
        return Promise.resolve({
          done: false,
          value: [],
          observed: `compose ps exited ${ps.status}`,
        });
      }
      const rows = parseComposePs(ps.stdout);
      const pending = servicesNotReady(rows, REQUIRED_SERVICES);
      return Promise.resolve({
        done: pending.length === 0,
        value: rows,
        observed: pending.length ? `not ready: ${pending.join(', ')}` : 'all ready',
      });
    },
    {
      timeoutMs,
      intervalMs: BUDGET.intervalMs,
      label: `${label}: ${REQUIRED_SERVICES.join(', ')} running and healthy`,
    },
  );
  log(`${label}: ready after ${outcome.elapsedMs} ms (${outcome.attempts} polls)`);
  return outcome;
}

async function commandWaitInfra() {
  step(12, 'wait for infrastructure (bounded health polling)');
  try {
    await waitForInfra('infrastructure');
  } catch (error) {
    console.error(String(error.message));
    console.error(compose(['ps', '--all']).stdout);
    console.error(compose(['logs', '--tail', '40']).stdout);
    process.exitCode = 1;
  }
}

function startApi(envFile) {
  const tail = [];
  const keep = (chunk) => {
    for (const line of chunk.toString().split(/\r?\n/).filter(Boolean)) {
      tail.push(line);
      if (tail.length > 60) tail.shift();
    }
  };
  const child = spawn(process.execPath, [`--env-file=${envFile}`, API_ENTRY], {
    cwd: ROOT,
    // Process env wins over --env-file, so this pins the gate's port.
    env: { ...process.env, API_HOST: '127.0.0.1', API_PORT: String(API_PORT), LOG_LEVEL: 'info' },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  child.stdout.on('data', keep);
  child.stderr.on('data', keep);
  return { child, tail };
}

async function stopApi(api) {
  if (!api || api.child.exitCode !== null) return;
  const exited = new Promise((resolve) => api.child.once('exit', resolve));
  api.child.kill('SIGTERM');
  const timer = setTimeout(() => api.child.kill('SIGKILL'), 5000);
  await exited;
  clearTimeout(timer);
}

function summarise(evidence) {
  const row = (label, value) => `| ${label} | ${value} |`;
  const lines = [
    '## P0 live-infrastructure gate',
    '',
    `Result: **${evidence.result}**${evidence.failure ? ` - ${evidence.failure}` : ''}`,
    '',
    '| Check | Evidence |',
    '| --- | --- |',
    row('Docker / Compose', `${evidence.docker ?? '?'} / compose ${evidence.compose ?? '?'}`),
  ];
  for (const s of evidence.steps) lines.push(row(s.name, s.detail));
  return `${lines.join('\n')}\n`;
}

async function commandRun() {
  const evidence = { startedAt: stamp(), result: 'FAIL', steps: [], api: { port: API_PORT } };
  const record = (name, detail, extra = {}) => {
    evidence.steps.push({ name, detail, ...extra });
    log(`EVIDENCE ${name}: ${detail}`);
  };
  let api;

  try {
    // --- STEP 10: tooling -------------------------------------------------------------
    step(10, 'verify Docker and Docker Compose v2');
    const docker = spawnSync('docker', ['--version'], { encoding: 'utf8' });
    if (docker.status !== 0) throw new Error('`docker` is not available');
    const composeVersion = compose(['version', '--short']);
    if (composeVersion.status !== 0)
      throw new Error('`docker compose` (Compose v2 plugin) is not available');
    evidence.docker = docker.stdout.trim();
    evidence.compose = composeVersion.stdout.trim();
    if (!isComposeV2OrNewer(evidence.compose)) {
      throw new Error(`Docker Compose ${evidence.compose} is older than v2`);
    }
    record('Tooling', `${evidence.docker}; Docker Compose ${evidence.compose}`);

    if (!existsSync(API_ENTRY))
      throw new Error('apps/api/dist/server.js missing - run `pnpm build` first');
    const { file: envFile, values: env } = loadFakeLocalEnv();
    if ((env.OVERSEER_ENV ?? 'local') !== 'local')
      throw new Error('gate refuses a non-local OVERSEER_ENV');
    log(`using fake local config: ${path.basename(envFile)}`);
    if (!(await portIsFree(API_PORT))) {
      throw new Error(`port ${API_PORT} is already in use; set P0_GATE_API_PORT to a free port`);
    }
    const secrets = [env.S3_SECRET_ACCESS_KEY, env.S3_ACCESS_KEY_ID, env.POSTGRES_PASSWORD].filter(
      Boolean,
    );

    step(12, 'confirm infrastructure is ready (bounded)');
    await waitForInfra('infrastructure');

    // --- STEP 13: start the API ------------------------------------------------------
    step(13, `start the OVERSEER API (local config, port ${API_PORT})`);
    api = startApi(envFile);
    const listen = await pollUntil(
      async () => {
        if (api.child.exitCode !== null) {
          throw new Error(`API exited early (code ${api.child.exitCode}):\n${api.tail.join('\n')}`);
        }
        try {
          const r = await fetchHealth();
          return { done: true, value: r, observed: `HTTP ${r.status}` };
        } catch (error) {
          return {
            done: false,
            value: null,
            observed: `no response (${error.cause?.code ?? error.name})`,
          };
        }
      },
      { timeoutMs: BUDGET.apiListenMs, intervalMs: 500, label: 'API to accept connections' },
    ).catch((error) => {
      throw new Error(`${error.message}\nAPI log tail:\n${api.tail.join('\n')}`);
    });
    if (!listen.value.correlationId) throw new Error('responding process is not the OVERSEER API');
    record(
      'API start',
      `pid ${api.child.pid} listening on 127.0.0.1:${API_PORT} after ${listen.elapsedMs} ms`,
    );

    // --- STEP 14: all healthy -> 200 --------------------------------------------------
    step(14, 'GET /health -> HTTP 200 with PostgreSQL, Redis and object store healthy');
    const healthy = await pollUntil(
      async () => {
        const r = await fetchHealth();
        let done = false;
        if (r.status === 200) {
          try {
            assertAllHealthy(r.body);
            done = true;
          } catch {
            /* keep polling */
          }
        }
        return { done, value: r, observed: `HTTP ${r.status} ${compact(r.body)}` };
      },
      {
        timeoutMs: BUDGET.healthyMs,
        intervalMs: BUDGET.intervalMs,
        label: 'HTTP 200, all dependencies ok',
      },
    );
    const h = healthy.value.body.checks;
    record('Postgres live health', `ok, ${h.postgres.latencyMs} ms (HTTP 200)`, { http: 200 });
    record('Redis live health', `ok, ${h.redis.latencyMs} ms (HTTP 200)`, { http: 200 });
    record('Object-store live health', `ok, ${h.objectStore.latencyMs} ms (HTTP 200)`, {
      http: 200,
    });
    record(
      'Overall /health',
      `HTTP 200, status ok, ${healthy.attempts} poll(s), ${healthy.elapsedMs} ms`,
    );

    // --- STEP 15: stop ONLY the object store -------------------------------------------
    step(15, `stop only the object-store service (${OBJECT_STORE_SERVICE})`);
    const stop = compose(['stop', OBJECT_STORE_SERVICE]);
    if (stop.status !== 0) throw new Error(`docker compose stop failed: ${stop.stderr.trim()}`);
    const rows = parseComposePs(compose(['ps', '--all', '--format', 'json']).stdout);
    const stateOf = (name) => rows.find((r) => r.service === name)?.state ?? 'missing';
    if (stateOf(OBJECT_STORE_SERVICE) === 'running')
      throw new Error('object store still running after stop');
    for (const other of ['postgres', 'redis']) {
      if (stateOf(other) !== 'running')
        throw new Error(`${other} is ${stateOf(other)} - only the object store may stop`);
    }
    record(
      'Object store stopped',
      `${OBJECT_STORE_SERVICE}=${stateOf(OBJECT_STORE_SERVICE)}, postgres/redis still running`,
    );

    // --- STEP 16: outage -> 503 -------------------------------------------------------
    step(16, 'GET /health -> HTTP 503 with a non-secret object-store error code');
    const outage = await pollUntil(
      async () => {
        const r = await fetchHealth();
        let done = false;
        if (r.status === 503) {
          try {
            assertObjectStoreDown(r.body, { secrets });
            done = true;
          } catch {
            /* keep polling */
          }
        }
        return { done, value: r, observed: `HTTP ${r.status} ${compact(r.body)}` };
      },
      {
        timeoutMs: BUDGET.outageMs,
        intervalMs: 1000,
        label: 'HTTP 503 with only objectStore failing',
      },
    );
    const store = outage.value.body.checks.objectStore;
    record(
      'Object-store failure -> 503',
      `HTTP 503, objectStore=fail, error=${store.error}, postgres=ok, redis=ok, no secrets in body`,
      { http: 503, error: store.error },
    );

    // --- STEPS 17-18: restart and wait --------------------------------------------------
    step(17, 'restart the object-store service');
    const start = compose(['start', OBJECT_STORE_SERVICE]);
    if (start.status !== 0) throw new Error(`docker compose start failed: ${start.stderr.trim()}`);
    record('Object store restarted', `${OBJECT_STORE_SERVICE} start issued`);

    step(18, 'wait for the object store to recover (bounded)');
    await waitForInfra('recovery', BUDGET.recoveryMs);

    // --- STEP 19: recovery -> 200 -----------------------------------------------------
    step(19, 'GET /health -> HTTP 200 after recovery');
    const recovered = await pollUntil(
      async () => {
        const r = await fetchHealth();
        let done = false;
        if (r.status === 200) {
          try {
            assertAllHealthy(r.body);
            done = true;
          } catch {
            /* keep polling */
          }
        }
        return { done, value: r, observed: `HTTP ${r.status} ${compact(r.body)}` };
      },
      {
        timeoutMs: BUDGET.recoveryMs,
        intervalMs: BUDGET.intervalMs,
        label: 'HTTP 200 after object-store recovery',
      },
    );
    record(
      'Object-store recovery -> 200',
      `HTTP 200, ${compact(recovered.value.body)}, ${recovered.attempts} poll(s), ${recovered.elapsedMs} ms`,
      { http: 200 },
    );

    evidence.result = 'PASS';
  } catch (error) {
    evidence.failure = String(error.message).split('\n')[0];
    console.error(`\nGATE FAILURE: ${error.message}`);
    if (api?.tail.length) console.error(`\nAPI log tail:\n${api.tail.join('\n')}`);
    process.exitCode = 1;
  } finally {
    step(20, 'shut down the API (infrastructure is stopped by `pnpm infra:down`)');
    await stopApi(api);
    log('API stopped');
    evidence.finishedAt = stamp();
    mkdirSync(path.dirname(EVIDENCE_PATH), { recursive: true });
    writeFileSync(EVIDENCE_PATH, `${JSON.stringify(evidence, null, 2)}\n`);
    if (process.env.GITHUB_STEP_SUMMARY)
      appendFileSync(process.env.GITHUB_STEP_SUMMARY, summarise(evidence));
    console.log(`\nP0 HEALTH GATE: ${evidence.result}`);
  }
}

const command = process.argv[2] ?? 'run';
if (command === 'wait-infra') await commandWaitInfra();
else if (command === 'run') await commandRun();
else {
  console.error(`unknown command: ${command} (expected: run | wait-infra)`);
  process.exitCode = 2;
}
