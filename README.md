# Aura Shell (Phase 1)

**Aura Shell** is a launcher-capable Android shell built with Kotlin and Jetpack Compose. Phase 1 delivers a minimal, polished home experience: default launcher support, installed app launching, a “recently launched from Aura” strip, pinned placeholders, and a universal command bar placeholder for later phases.

## What Phase 1 includes

- **Home / launcher**: Manifest declares `MAIN` + `HOME` + `DEFAULT` so the app can be chosen as the default Home app.
- **Main screen**: Date/time header, subtitle, command bar placeholder, pinned cards, recent strip, and full installed-apps list (alphabetical).
- **Command layer placeholder**: Tapping the command bar opens a small stub screen (`CommandLayerActivity`) describing Phase 2; the home bar is styled as the future intent router surface.
- **Installed apps**: Queries launchable activities via `PackageManager`, shows icon + label, launches with `getLaunchIntentForPackage` + `FLAG_ACTIVITY_NEW_TASK`.
- **Recents**: **Not** the system recents list (third-party launchers cannot read that). Aura stores a **most-recently-used list of apps launched from Aura** in app-private storage (`RecentAppsStore`). This is documented and structured for evolution.
- **Pinned cards**: Visual placeholders only (subtle snackbar on tap).

## What is intentionally deferred

- AI / LLM / chat / semantic search  
- Notifications, file indexing, cloud sync  
- Accessibility automation, voice, widgets  
- Real command routing (Phase 2+)

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
| Command stub screen | `app/src/main/java/com/aura/shell/CommandLayerActivity.kt` |
| Theme | `app/src/main/java/com/aura/shell/ui/theme/` |

## Android limitations (launchers on modern devices)

- **System recent tasks** are not exposed to arbitrary third-party launchers; use OEM/system UI or privileged APIs. Aura uses an **MRU list of launches from this app** instead.
- **Query visibility**: On Android 11+, package visibility may require `<queries>` for some intents; querying `MAIN`/`LAUNCHER` activities is the standard approach for launchers.
- **Default launcher**: User must explicitly set the default Home app; the app cannot force itself as default.
- **Background execution**: Not used in Phase 1; no services required for the shell UX described here.

## License

See repository license if applicable.
