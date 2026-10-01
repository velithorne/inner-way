import { fakeLocalEnv } from '@overseer/test-fixtures';
import { describe, expect, it } from 'vitest';
import { ConfigError, loadConfig } from './config';

describe('loadConfig', () => {
  it('parses the fake local environment', () => {
    const config = loadConfig(fakeLocalEnv());
    expect(config.OVERSEER_ENV).toBe('local');
    expect(config.API_PORT).toBe(4000);
    expect(config.S3_FORCE_PATH_STYLE).toBe(true);
    expect(config.HEALTH_CHECK_TIMEOUT_MS).toBe(2000);
  });

  it('defaults to a loopback bind address', () => {
    const env = fakeLocalEnv();
    delete env.API_HOST;
    expect(loadConfig(env).API_HOST).toBe('127.0.0.1');
  });

  it('rejects a missing required variable by name', () => {
    const env = fakeLocalEnv();
    delete env.DATABASE_URL;
    expect(() => loadConfig(env)).toThrow(ConfigError);
    expect(() => loadConfig(env)).toThrow(/DATABASE_URL/);
  });

  it('rejects the wrong URL protocol', () => {
    expect(() => loadConfig(fakeLocalEnv({ REDIS_URL: 'http://127.0.0.1:6379' }))).toThrow(
      /REDIS_URL/,
    );
  });

  it('rejects non-local service hosts when OVERSEER_ENV=local', () => {
    const env = fakeLocalEnv({
      DATABASE_URL: 'postgres://prod_user:hunter2-fake-secret@db.prod.example.com:5432/overseer',
    });
    expect(() => loadConfig(env)).toThrow(/DATABASE_URL: must point at a local host/);
  });

  it('allows non-local service hosts in staging', () => {
    const env = fakeLocalEnv({
      OVERSEER_ENV: 'staging',
      DATABASE_URL: 'postgres://u:fake-pw@db.staging.example.com:5432/overseer',
    });
    expect(loadConfig(env).OVERSEER_ENV).toBe('staging');
  });

  it('never echoes secret values in its error message', () => {
    const env = fakeLocalEnv({
      DATABASE_URL: 'postgres://prod_user:hunter2-fake-secret@db.prod.example.com:5432/overseer',
      S3_SECRET_ACCESS_KEY: '',
    });
    let message = '';
    try {
      loadConfig(env);
    } catch (error) {
      message = (error as Error).message;
    }
    expect(message).not.toBe('');
    expect(message).not.toContain('hunter2-fake-secret');
    expect(message).not.toContain('prod_user');
  });
});
