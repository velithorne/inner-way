import type { DependencyCheckResult, DependencyName } from '@overseer/contracts';
import { useEffect, useState } from 'react';
import { environmentBanner, fetchHealth, type HealthState } from './health';

const POLL_INTERVAL_MS = 5000;

const DEPENDENCY_LABELS: Record<DependencyName, string> = {
  postgres: 'PostgreSQL',
  redis: 'Redis',
  objectStore: 'Object store',
};

function useHealth(): HealthState {
  const [state, setState] = useState<HealthState>({ kind: 'loading' });

  useEffect(() => {
    let cancelled = false;
    const poll = async () => {
      const next = await fetchHealth();
      if (!cancelled) setState(next);
    };
    void poll();
    const timer = setInterval(() => void poll(), POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(timer);
    };
  }, []);

  return state;
}

function DependencyTile({ name, result }: { name: DependencyName; result: DependencyCheckResult }) {
  const ok = result.status === 'ok';
  return (
    <li className={`tile ${ok ? 'tile--ok' : 'tile--fail'}`}>
      <span className="tile__name">{DEPENDENCY_LABELS[name]}</span>
      <span className="tile__status">{ok ? 'CONNECTED' : `FAILED (${result.error})`}</span>
      <span className="tile__latency">{result.latencyMs} ms</span>
    </li>
  );
}

export function App() {
  const state = useHealth();

  return (
    <main className="shell">
      <header className="shell__header">
        <h1>OVERSEER</h1>
        <p>AI Commerce Command-Centre</p>
        {state.kind === 'ready' && (
          <span className={`banner banner--${environmentBanner(state.health.environment).tone}`}>
            {environmentBanner(state.health.environment).label}
          </span>
        )}
      </header>

      <section aria-labelledby="infra-heading" className="panel">
        <h2 id="infra-heading">Infrastructure</h2>
        {state.kind === 'loading' && <p className="note">Checking API…</p>}
        {state.kind === 'unreachable' && (
          <p className="note note--fail">API unreachable. Is `pnpm dev:api` running?</p>
        )}
        {state.kind === 'invalid' && (
          <p className="note note--fail">API responded with an unexpected body.</p>
        )}
        {state.kind === 'ready' && (
          <ul className="tiles">
            {(Object.keys(DEPENDENCY_LABELS) as DependencyName[]).map((name) => (
              <DependencyTile key={name} name={name} result={state.health.checks[name]} />
            ))}
          </ul>
        )}
      </section>

      <footer className="shell__footer">
        Phase P0 - repository &amp; safety spine. No facility, agents or integrations yet.
      </footer>
    </main>
  );
}
