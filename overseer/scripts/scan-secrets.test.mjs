import { describe, expect, it } from 'vitest';
import { isForbiddenEnvFile, scanRepository, scanText } from './scan-secrets.mjs';

// Sample secrets are assembled at runtime so this file does not itself trip the scanner.
const aws = 'AKIA' + 'ABCDEFGHIJKLMNOP';
const shopify = 'shpat_' + 'a'.repeat(32);
const github = 'ghp_' + 'A1b2'.repeat(9);
const privateKey = '-----BEGIN ' + 'RSA PRIVATE KEY-----';
const connString = 'postgres://admin:' + 'Tr0ub4dor-real-pass' + '@db.internal/app';
const assignment = 'PRINTIFY_API_' + 'TOKEN=' + 'Zk39dLq72mXv81nBcT55wRyH';
const jwt = 'secret_token = ' + 'eyJhbGciOiJIUzI1NiJ9' + '.' + 'eyJzdWIiOiIxMjM0NTY3ODkwIn0';

const rulesFor = (text) => scanText(text, 'f.txt').map((finding) => finding.rule);

describe('scanText', () => {
  it.each([
    ['aws-access-key-id', `key = ${aws}`],
    ['shopify-token', `token: ${shopify}`],
    ['github-token', `x ${github}`],
    ['private-key-block', privateKey],
    ['credentialed-connection-string', connString],
    ['secret-assignment', assignment],
    ['secret-assignment', jwt],
  ])('flags %s', (rule, text) => {
    expect(rulesFor(text)).toContain(rule);
  });

  it('reports file and line but never the matched value', () => {
    const findings = scanText(`ok\nkey = ${aws}\n`, 'config.ts');
    expect(findings).toEqual([{ file: 'config.ts', line: 2, rule: 'aws-access-key-id' }]);
    expect(JSON.stringify(findings)).not.toContain(aws);
  });

  it('allows documented fake local values', () => {
    expect(rulesFor('POSTGRES_PASSWORD=overseer_local_only')).toEqual([]);
    expect(rulesFor('S3_SECRET_ACCESS_KEY=overseer_local_only_secret')).toEqual([]);
    expect(
      rulesFor('DATABASE_URL=postgres://overseer:overseer_local_only@127.0.0.1:5432/o'),
    ).toEqual([]);
    expect(rulesFor('PASSWORD=${POSTGRES_PASSWORD:-changeme-please}')).toEqual([]);
  });

  it('does not flag references to secrets, only literals', () => {
    expect(rulesFor('secretAccessKey: config.S3_SECRET_ACCESS_KEY,')).toEqual([]);
    expect(rulesFor('const apiToken = process.env.PRINTIFY_API_TOKEN;')).toEqual([]);
    expect(rulesFor('PASSWORD=POSTGRES_ADMIN_PASSWORD')).toEqual([]);
  });

  it('honours an explicit allow marker on the line', () => {
    expect(rulesFor(`key = ${aws} # secret-scan:allow test vector`)).toEqual([]);
  });

  it('ignores short or ordinary assignments', () => {
    expect(rulesFor('const tokenCount = 5;')).toEqual([]);
    expect(rulesFor('password: short')).toEqual([]);
  });
});

describe('isForbiddenEnvFile', () => {
  it('forbids real env files but allows the template', () => {
    expect(isForbiddenEnvFile('.env')).toBe(true);
    expect(isForbiddenEnvFile('apps/api/.env.production')).toBe(true);
    expect(isForbiddenEnvFile('.env.local')).toBe(true);
    expect(isForbiddenEnvFile('.env.example')).toBe(false);
    expect(isForbiddenEnvFile('src/environment.ts')).toBe(false);
  });
});

describe('scanRepository', () => {
  it('flags an env file that is present in the candidate list', () => {
    expect(scanRepository(['.env'])).toEqual([
      { file: '.env', line: 0, rule: 'env-file-not-ignored' },
    ]);
  });
});
