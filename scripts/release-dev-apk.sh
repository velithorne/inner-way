#!/usr/bin/env bash
# Build debug APK (DEV_SIMULATION profile — BuildConfig.DEBUG true) and create a GitHub release.
# Usage: ./scripts/release-dev-apk.sh [optional-tag-suffix]
# Requires: gh CLI, logged in; run from repo root.

set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew assembleDebug --no-daemon -q

VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -1 | sed -E "s/.*versionName = \"([^\"]+)\".*/\1/")
VERSION_CODE=$(grep -E 'versionCode\s*=' app/build.gradle.kts | head -1 | sed -E 's/.*versionCode = ([0-9]+).*/\1/')
SUFFIX="${1:-dev-debug}"
TAG="v${VERSION_NAME}-${SUFFIX}"
SHORT_SHA=$(git rev-parse --short HEAD)

echo "Creating release $TAG (versionCode $VERSION_CODE, $SHORT_SHA)..."

gh release create "$TAG" \
  "app/build/outputs/apk/debug/app-debug.apk" \
  --repo "${GITHUB_REPOSITORY:-velithorne/inner-way}" \
  --title "Velithorne Vessel ${VERSION_NAME} — Dev mode (debug APK)" \
  --notes "**Dev simulation mode** — this APK is a **debug** build (\`BuildConfig.DEBUG = true\`): accelerated growth profile, fresh specimen on new dev build, dev UI hints.

**Not** the long-term release pacing (use the separate release-type APK if you need \`RELEASE_REALTIME\`).

- **versionName:** \`${VERSION_NAME}\`
- **versionCode:** \`${VERSION_CODE}\`
- **APK:** \`app-debug.apk\` (install this file from Assets)
- **Commit:** \`${SHORT_SHA}\`

Build: \`./gradlew assembleDebug\`" \
  --target "$(git branch --show-current)"

echo "Done: https://github.com/${GITHUB_REPOSITORY:-velithorne/inner-way}/releases/tag/${TAG}"
