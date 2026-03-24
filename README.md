# Aura Shell

**Aura Shell** is a launcher-capable Android shell built with Kotlin and Jetpack Compose. It provides a command-first home surface, default launcher support, app launching, MRU “recent” tracking, and placeholders for later intent and voice layers.

## Download the latest APK (sideload)

Prebuilt **debug** APKs are attached to **GitHub Releases** when a maintainer pushes a **version tag** (see below).

**Always use the latest release** (replace `owner/repo` if you forked):

[https://github.com/velithorne/inner-way/releases/latest](https://github.com/velithorne/inner-way/releases/latest)

Open **Assets** and download `AuraShell-<tag>-debug.apk`, then install on your device (allow install from your browser/Files app if prompted).

### For maintainers: ship an APK after each phase

1. Bump `versionCode` / `versionName` in `app/build.gradle.kts` and merge your phase work to `main` (or your release branch).
2. Tag the release commit and push the tag (use a new `v…` name each time):

   ```bash
   git tag v1.0.2-phase2
   git push origin v1.0.2-phase2
   ```

3. GitHub Actions (**Release APK** workflow) builds `assembleDebug` and uploads the APK to a Release for that tag. **Releases do not happen automatically on every commit** — only when you push a matching `v*` tag, so each phase gets a clear, downloadable build.

If the workflow fails, check the **Actions** tab on GitHub for logs.

## Phase 2 — typed command router

- **Command layer** (`CommandLayerActivity`): Full-screen Compose UI with command field (keyboard-friendly), result area, and **recent command history** (last ~18 commands, SharedPreferences).
- **Local router** (`CommandRouter` + `CommandDispatch`): Deterministic parsing — **open/launch** apps, **search apps for …**, **show/hide apps** (coordinates launcher bottom sheet via `MainActivity` + `DrawerRequest`), **show recents** / **open recent …**, **help**. No network, no ML.
- **History** (`CommandHistoryStore`): Persists typed commands for replay chips.

## What Phase 1 includes

- **Home / launcher**: Manifest declares `MAIN` + `HOME` + `DEFAULT` so the app can be chosen as the default Home app.
- **Main screen**: Date/time header, command-first layout (Phase 1.1+), command bar, modules, compact recents, app drawer in bottom sheet.
- **Command layer**: Opens from the command bar; Phase 2 adds typed routing (see above).
- **Installed apps**: Queries launchable activities via `PackageManager`, shows icon + label, launches with `getLaunchIntentForPackage` + `FLAG_ACTIVITY_NEW_TASK`.
- **Recents**: **Not** the system recents list (third-party launchers cannot read that). Aura stores a **most-recently-used list of apps launched from Aura** in app-private storage (`RecentAppsStore`). This is documented and structured for evolution.
- **Pinned cards**: Visual placeholders only (subtle snackbar on tap).

## What is intentionally deferred

- AI / LLM / cloud APIs / semantic “understanding” beyond pattern matching  
- Notifications, file indexing, cloud sync  
- Accessibility automation, voice recognition (mic is placeholder)  
- Widgets, background services

## Requirements

- **JDK 17**  
- **Android SDK** with API **35** platform and build-tools (Android Studio or command-line tools)

## How to run

1. Open the project in **Android Studio** (or use Gradle from the CLI).
2. Set `sdk.dir` in `local.properties` if needed, for example:
   `sdk.dir=/path/to/Android/sdk`
3. Build and install a debug build:
   ```bash
   ./gradlew installDebug
   ```
   Or run the `app` configuration from Android Studio on a connected device or emulator.

## How to set Aura Shell as the default launcher

1. Install the app on a physical device or emulator.
2. Open **Settings → Apps → Default apps → Home app** (wording varies by OEM).
3. Select **Aura Shell**.

Alternatively, press **Home** after install; Android may show the Home app picker—choose Aura Shell and tap **Always**.

## Project layout (high level)

| Area | Path |
|------|------|
| Launcher / Home intent filters | `app/src/main/AndroidManifest.xml` |
| Main UI entry | `app/src/main/java/com/aura/shell/MainActivity.kt` |
| Home Compose screen | `app/src/main/java/com/aura/shell/ui/home/HomeScreen.kt` |
| ViewModel & state | `app/src/main/java/com/aura/shell/ui/home/HomeViewModel.kt` |
| App query & launch | `app/src/main/java/com/aura/shell/data/LauncherRepository.kt` |
| MRU “recents” persistence | `app/src/main/java/com/aura/shell/data/RecentAppsStore.kt` |
| Command layer + router | `command/`, `ui/command/CommandLayerScreen.kt`, `CommandLayerActivity.kt` |
| Theme | `app/src/main/java/com/aura/shell/ui/theme/` |

## Android limitations (launchers on modern devices)

- **System recent tasks** are not exposed to arbitrary third-party launchers; use OEM/system UI or privileged APIs. Aura uses an **MRU list of launches from this app** instead.
- **Query visibility**: On Android 11+, package visibility may require `<queries>` for some intents; querying `MAIN`/`LAUNCHER` activities is the standard approach for launchers.
- **Default launcher**: User must explicitly set the default Home app; the app cannot force itself as default.
- **Background execution**: Not used in Phase 1; no services required for the shell UX described here.

## License

See repository license if applicable.
