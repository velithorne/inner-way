import { buildApp } from './app';
import { ConfigError, loadConfig } from './config';
import { createDependencyChecks } from './services/dependency-checks';

function loadConfigOrExit() {
  try {
    return loadConfig(process.env);
  } catch (error) {
    if (error instanceof ConfigError) {
      console.error(error.message);
      process.exit(1);
    }
    throw error;
  }
}

const config = loadConfigOrExit();
const app = buildApp({ config, checks: createDependencyChecks(config) });

for (const signal of ['SIGINT', 'SIGTERM'] as const) {
  process.once(signal, () => {
    app.close().then(
      () => process.exit(0),
      () => process.exit(1),
    );
  });
}

try {
  await app.listen({ host: config.API_HOST, port: config.API_PORT });
  app.log.info({ environment: config.OVERSEER_ENV }, 'overseer-api started');
} catch (error) {
  app.log.error(error, 'failed to start overseer-api');
  process.exit(1);
}
