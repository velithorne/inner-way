#!/usr/bin/env bash
# Same steps as .github/workflows/android-apk-eas.yml — run on your machine when you have
# EXPO_TOKEN (and optionally gh + GITHUB_TOKEN for creating a GitHub Release locally).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ -z "${EXPO_TOKEN:-}" ]]; then
  echo "Set EXPO_TOKEN first (Expo → https://expo.dev/settings/access-tokens )"
  echo "  export EXPO_TOKEN=xxxxxxxx"
  exit 1
fi

if ! grep -q '"projectId"' app.json 2>/dev/null; then
  echo "Link this app to EAS once (writes expo.extra.eas.projectId in app.json):"
  echo "  npx eas-cli login"
  echo "  npx eas-cli init --non-interactive"
  echo "Then commit app.json and re-run this script."
  exit 1
fi

echo "==> EAS Build (preview / APK) — same as GitHub Actions"
npx eas-cli build -p android --profile preview --non-interactive --wait --json > /tmp/eas-build.json

APK_URL="$(jq -r 'if type == "array" then .[0] else . end | .artifacts.applicationArchiveUrl // empty' /tmp/eas-build.json)"
BUILD_PAGE="$(jq -r 'if type == "array" then .[0] else . end | .url // empty' /tmp/eas-build.json)"

if [[ -z "$APK_URL" || "$APK_URL" == "null" ]]; then
  echo "Could not read APK URL from build output:"
  cat /tmp/eas-build.json
  exit 1
fi

OUT="${1:-$ROOT/phantom.apk}"
echo "==> Download APK → $OUT"
curl -fsSL -L -o "$OUT" "$APK_URL"
ls -la "$OUT"

echo ""
echo "Expo build page: $BUILD_PAGE"
echo "Local APK:       $OUT"
echo ""
echo "To match CI fully, push to GitHub and add EXPO_TOKEN as a repo secret, then:"
echo "  Actions → Android APK release (EAS + GitHub) → Run workflow"
echo ""

if command -v gh >/dev/null 2>&1 && gh auth status >/dev/null 2>&1; then
  read -r -p "Create GitHub Release with this APK? [y/N] " ans
  if [[ "${ans:-}" =~ ^[yY]$ ]]; then
    TAG="android-$(date +%Y%m%d-%H%M%S)"
    gh release create "$TAG" "$OUT" --title "PHANTOM $TAG" --notes "Android APK (EAS). Expo: $BUILD_PAGE"
    echo "Release: $(gh repo view --json url -q .url)/releases/tag/$TAG"
  fi
else
  echo "(Install GitHub CLI \`gh\` and \`gh auth login\` to optionally create a release from this script.)"
fi
