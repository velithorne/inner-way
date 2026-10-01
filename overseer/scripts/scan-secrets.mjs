#!/usr/bin/env node
// Dependency-free secret scanner (spec §0: secrets never enter source control; P0 gate:
// "No secret committed; .env ignored").
//
// Scans every tracked or not-ignored file under the current directory. Findings print the
// rule, file and line ONLY - the matched value is never printed, so a leak is not repeated
// into CI logs. Exits 1 when anything is found.
//
// A line containing `secret-scan:allow` is skipped. Values containing a fake marker
// (e.g. `local_only`, `changeme`, `fake`) and plain code references (`config.X`) are
// treated as placeholders. The aim is to catch literals, not variable names.

import { execFileSync } from 'node:child_process';
import { readFileSync, statSync } from 'node:fs';
import { basename } from 'node:path';
import { pathToFileURL } from 'node:url';

const ALLOW_MARKER = 'secret-scan:allow';
const FAKE_VALUE =
  /local_only|changeme|change_me|example|placeholder|your_|dummy|fake|<[^>]+>|\$\{/i;
// `config.S3_SECRET_ACCESS_KEY` or `SOME_ENV_NAME` is a reference to a secret, not a secret.
// JWTs also look like dotted identifiers, so anything starting `eyJ` is never treated as one.
const CODE_REFERENCE = /^(?:[A-Za-z_$][\w$]*\.)+[A-Za-z_$][\w$]*$|^[A-Z][A-Z0-9_]{7,}$/;

function isPlaceholder(value) {
  if (FAKE_VALUE.test(value)) return true;
  return !value.startsWith('eyJ') && CODE_REFERENCE.test(value);
}

/** @type {ReadonlyArray<{ id: string; pattern: RegExp; valueGroup?: number }>} */
export const RULES = [
  {
    id: 'private-key-block',
    pattern: /-----BEGIN (?:RSA |EC |DSA |OPENSSH |PGP )?PRIVATE KEY-----/,
  },
  { id: 'aws-access-key-id', pattern: /\b(?:AKIA|ASIA)[0-9A-Z]{16}\b/ },
  { id: 'shopify-token', pattern: /\bshp(?:at|ss|ca|pa)_[a-fA-F0-9]{32}\b/ },
  {
    id: 'github-token',
    pattern: /\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{50,})\b/,
  },
  { id: 'llm-api-key', pattern: /\bsk-(?:ant-)?[A-Za-z0-9_-]{32,}\b/ },
  { id: 'slack-token', pattern: /\bxox[baprs]-[A-Za-z0-9-]{10,}\b/ },
  { id: 'stripe-live-key', pattern: /\b[sr]k_live_[A-Za-z0-9]{16,}\b/ },
  {
    id: 'credentialed-connection-string',
    pattern:
      /\b(?:postgres(?:ql)?|redis|rediss|mysql|mongodb(?:\+srv)?|amqps?):\/\/[^\s:@/]+:([^\s@/]+)@/,
    valueGroup: 1,
  },
  {
    id: 'secret-assignment',
    pattern:
      /\b[A-Za-z0-9_]*(?:secret|password|passwd|token|api[_-]?key|private[_-]?key)[A-Za-z0-9_]*\s*[:=]\s*["']?([A-Za-z0-9+/_=.-]{16,})["']?/i,
    valueGroup: 1,
  },
];

/** `.env` and `.env.*` files must never be committed, except the fake-values template. */
export function isForbiddenEnvFile(path) {
  const name = basename(path);
  return (name === '.env' || name.startsWith('.env.')) && name !== '.env.example';
}

/**
 * @param {string} text
 * @param {string} filename
 * @returns {Array<{ file: string; line: number; rule: string }>}
 */
export function scanText(text, filename) {
  const findings = [];
  const lines = text.split(/\r?\n/);
  lines.forEach((line, index) => {
    if (line.includes(ALLOW_MARKER)) return;
    for (const rule of RULES) {
      const match = rule.pattern.exec(line);
      if (!match) continue;
      const value = rule.valueGroup === undefined ? undefined : match[rule.valueGroup];
      if (value !== undefined && isPlaceholder(value)) continue;
      findings.push({ file: filename, line: index + 1, rule: rule.id });
    }
  });
  return findings;
}

function looksBinary(buffer) {
  return buffer.subarray(0, 8192).includes(0);
}

function listCandidateFiles() {
  const out = execFileSync(
    'git',
    ['ls-files', '--cached', '--others', '--exclude-standard', '-z'],
    {
      encoding: 'utf8',
      maxBuffer: 64 * 1024 * 1024,
    },
  );
  return out.split('\0').filter(Boolean);
}

export function scanRepository(files = listCandidateFiles()) {
  const findings = [];
  for (const file of files) {
    if (isForbiddenEnvFile(file)) {
      findings.push({ file, line: 0, rule: 'env-file-not-ignored' });
      continue;
    }
    if (basename(file) === 'pnpm-lock.yaml') continue;
    let stat;
    try {
      stat = statSync(file);
    } catch {
      continue; // deleted but still indexed
    }
    if (!stat.isFile() || stat.size > 1_000_000) continue;
    const buffer = readFileSync(file);
    if (looksBinary(buffer)) continue;
    findings.push(...scanText(buffer.toString('utf8'), file));
  }
  return findings;
}

function main() {
  const findings = scanRepository();
  if (findings.length === 0) {
    console.log('secret scan: no findings');
    return 0;
  }
  console.error(`secret scan: ${findings.length} finding(s) (values intentionally not shown)`);
  for (const { file, line, rule } of findings) {
    console.error(`  ${file}${line ? `:${line}` : ''}  ${rule}`);
  }
  return 1;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  process.exitCode = main();
}
