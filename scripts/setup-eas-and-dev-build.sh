#!/usr/bin/env bash
# Link PHANTOM to Expo (EAS) and build the Android development client APK in one go.
# Prerequisites:
#   - Expo account: https://expo.dev/signup
#   - Either: export EXPO_TOKEN=...  (https://expo.dev/settings/access-tokens)
#   - Or: run `npx eas-cli login` once before this script (interactive).
#
# Usage:
#   chmod +x scripts/setup-eas-and-dev-build.sh
#   export EXPO_TOKEN=xxxxxxxx    # recommended for CI-like non-interactive use
#   ./scripts/setup-eas-and-dev-build.sh
#
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if ! command -v npx >/dev/null 2>&1; then
  echo "Need npx (Node.js)."
  exit 1
fi

echo "==> Install dependencies"
npm ci

if [[ -n "${EXPO_TOKEN:-}" ]]; then
  echo "==> Using EXPO_TOKEN for EAS (non-interactive)"
else
  echo "==> No EXPO_TOKEN — checking EAS login"
  if ! npx eas-cli whoami >/dev/null 2>&1; then
    echo "Run: npx eas-cli login"
    echo "Or set EXPO_TOKEN from https://expo.dev/settings/access-tokens"
    exit 1
  fi
fi

echo "==> Link project (writes expo.extra.eas.projectId in app.json)"
npx eas-cli init --non-interactive --force

if git diff --quiet app.json 2>/dev/null; then
  echo "app.json unchanged (project may already be linked)."
else
  echo "==> Commit app.json with new EAS project id"
  git add app.json
  git commit -m "chore(eas): link PHANTOM to Expo project (projectId)" || true
fi

echo "==> Build development client APK (install this on your phone — not Expo Go)"
npx eas-cli build -p android --profile development --non-interactive --wait

echo ""
echo "Done. Next:"
echo "  1. Download the APK from the URL above (or Expo dashboard)."
echo "  2. Install on device."
echo "  3. From repo: npm start"
echo "  4. Open PHANTOM from the dev client app (Metro)."
echo ""
