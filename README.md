# Warung POS

Offline-first Android point-of-sale app for a single Indonesian food stall (warung), built with
Jetpack Compose, Room, Hilt, and Firebase Realtime Database for cross-device sync.

## Prerequisites

- Android Studio (latest stable) with Android SDK 37 installed
- JDK 17 (bundled with recent Android Studio)
- A Firebase project with Realtime Database and Authentication (Email/Password) enabled
- [Firebase CLI](https://firebase.google.com/docs/cli) (`npm install -g firebase-tools`) — only
  needed to deploy `firebase/database.rules.json`
- A physical device or emulator running API 26+ (minSdk 26)

## Getting started

1. Clone the repo:
   ```bash
   git clone https://github.com/HarisWeitani/warung-pos.git
   cd warung-pos
   ```
2. In the [Firebase console](https://console.firebase.google.com/), create (or reuse) a project,
   register an Android app with package name `com.wfx.warungpos`, and download the generated
   `google-services.json`.
3. Place that file at `app/google-services.json` (already gitignored — never commit it).
4. Open the project in Android Studio and let Gradle sync. The first sync downloads AGP 9.2.1,
   Kotlin 2.2.10, and KSP 2.2.10-2.0.2 — verify your Android Studio version supports these.
5. Complete the one-time Firebase setup in [`docs/firebase-setup.md`](docs/firebase-setup.md)
   (Auth accounts, custom claims, seeding `appConfig`, deploying RTDB rules) before first run —
   the app will not let any user past the login screen without a valid Auth account + role claim.

## Building

Debug build (installs straight to a connected device/emulator):
```bash
./gradlew installDebug
```

Run unit tests (JVM, no device needed):
```bash
./gradlew testDebugUnitTest
```

Run instrumented tests (Room DAO integration tests, requires a connected device/emulator):
```bash
./gradlew connectedDebugAndroidTest
```

Release build:
```bash
./gradlew assembleRelease
```
Without a configured keystore (see below) this produces an **unsigned** release APK suitable for
local verification only. To produce an installable signed APK, configure signing first.

## Release signing

`app/build.gradle.kts` reads keystore credentials from `local.properties` (which is gitignored
and never committed) rather than hardcoding them. Add these four keys to your local
`local.properties`:
```properties
RELEASE_STORE_FILE=/absolute/path/to/release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```
If you don't have a release keystore yet, generate one with `keytool`:
```bash
keytool -genkeypair -v -keystore release.jks -alias warungpos -keyalg RSA -keysize 2048 -validity 10000
```
Keep this file and its passwords safe — losing them means you can never publish an update under
the same `applicationId` again. With all four keys present, `./gradlew assembleRelease` produces
a signed, R8-minified APK at `app/build/outputs/apk/release/`.

## How to release a new version

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Build the signed release APK: `./gradlew assembleRelease`.
3. Distribute the APK directly to the store owner/staff devices (sideload — there is no Play
   Store distribution for this app).
4. Update `appConfig/minVersionCode` in the Firebase RTDB console to the new `versionCode` **only
   once all devices have updated** — any device below this value is shown a non-dismissable
   "update required" screen on next launch. See [`docs/firebase-setup.md`](docs/firebase-setup.md)
   for how to edit this value.

## Architecture

Clean Architecture organized by package (not Gradle modules) under `app/src/main/java/com/wfx/warungpos/`:
- `core/` — cross-cutting utilities, navigation, DI modules, common enums
- `domain/` — `model/`, `repository/` (interfaces), `usecase/`, `exception/` — no Android/Firebase
  dependencies
- `data/` — Room entities/DAOs/mappers, repository implementations, Firebase sync (`RtdbListener`,
  `SyncWorker`, `ConflictResolver`)
- `feature/` — one package per screen area (order, payment, shift, menu, settings, reports, etc.),
  each with a `ViewModel` + Composable screen

Sync model: every write goes to Room first (`syncStatus = PENDING`), then a `WorkManager` job
pushes pending rows to RTDB. `RtdbListener` listens for remote changes and applies them locally
via `ConflictResolver` (last-write-wins by `updatedAt`, with status-regression guards — e.g. a
remote "OPEN" can never overwrite a local "PAID" bill).

## RTDB path structure

These paths (and their owner/staff write permissions) are defined in
[`firebase/database.rules.json`](firebase/database.rules.json) and mirrored in
`RtdbPaths.kt`:

| Path | Read | Write |
|---|---|---|
| `appConfig` | any authenticated user | server-only (no client writes) |
| `menuCategories/{id}` | any authenticated user | owner or staff |
| `menuItems/{id}` | any authenticated user | owner or staff |
| `variantGroups/{id}` | any authenticated user | owner or staff |
| `variantOptions/{id}` | any authenticated user | owner or staff |
| `tables/{id}` | any authenticated user | owner only |
| `paymentMethods/{id}` | any authenticated user | owner only |
| `shifts/{id}` | any authenticated user | owner or staff |
| `bills/{id}` | any authenticated user | owner or staff |
| `orderItems/{id}` | any authenticated user | owner or staff |
| `payments/{id}` | any authenticated user | owner or staff |
| `expenses/{id}` | any authenticated user | owner or staff |
| `stockItems/{id}` | any authenticated user | owner only |
| `stockBatches/{id}` | any authenticated user | owner only |
| `opnames/{id}` | any authenticated user | owner only |

`appConfig/minVersionCode` (a single integer) drives the version gate — see "How to release a new
version" above.

## Testing conventions

- Unit tests (`app/src/test/`): plain JUnit4, `kotlinx-coroutines-test`, fake repositories under
  `fake/` — no mocking framework. Use cases depend on `SessionProvider` (a minimal interface for
  `currentUserId`/`currentUserRole`/`deviceId`), not the concrete Firebase-backed `SessionManager`,
  so they're testable without Robolectric.
- Instrumented tests (`app/src/androidTest/`): in-memory Room database
  (`Room.inMemoryDatabaseBuilder`), run via `./gradlew connectedDebugAndroidTest` against a
  connected device or emulator.
