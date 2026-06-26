# Implementation Plan — TICKET-001: Gradle Plugins and Build Toolchain

## Observations from current state

| Item | Current | Target |
|---|---|---|
| AGP | 9.2.1 (keep — higher than spec) | — |
| Kotlin | 2.2.10 (keep) | — |
| compileSdk | 36 with preview syntax | **35** (ticket AC) |
| targetSdk | 36 | **35** (ticket AC) |
| KSP | absent | add |
| Hilt plugin | absent | add |
| kotlin.android plugin | absent | add |
| kotlin.serialization plugin | absent | add |
| google-services plugin | absent | add |
| google-services.json | ✅ already in /app | — |

**Not in scope for TICKET-001:** namespace, applicationId, Kotlin source files. Deferred to TICKET-003.

## Files

| File | Action |
|---|---|
| `gradle/libs.versions.toml` | MODIFY — add ksp/hilt/googleServices versions + kotlin.android/kotlin.serialization/ksp/hilt/google-services plugin aliases |
| `build.gradle.kts` | MODIFY — add plugin declarations (apply false) |
| `app/build.gradle.kts` | MODIFY — apply new plugins, fix compileSdk=35, targetSdk=35 |

## Exact changes

### 1. gradle/libs.versions.toml

Add under `[versions]`:
```toml
ksp = "2.2.10-1.0.29"
hilt = "2.56.1"
googleServices = "4.4.2"
```
> Note: ksp version format is `{kotlinVersion}-{release}`. Verify against https://github.com/google/ksp/releases if build fails.

Add under `[plugins]`:
```toml
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

### 2. build.gradle.kts (project-level) — full replacement

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}
```

### 3. app/build.gradle.kts — plugin block + compileSdk/targetSdk

Plugin block becomes:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}
```

android block changes:
- `compileSdk { version = release(36) { ... } }` → `compileSdk = 35`
- `targetSdk = 36` → `targetSdk = 35`

## Verification

```bash
./gradlew assembleDebug
grep -r "kapt" app/build.gradle.kts gradle/libs.versions.toml build.gradle.kts  # must be zero
```

## Deferred to later tickets

- **Namespace** (`com.wfx.warungpos` vs `com.warungpos` from architecture doc) → TICKET-003
- **Dependencies** (Room, Hilt, WorkManager, etc.) → TICKET-002
- **WorkManager InitializationProvider** removal → TICKET-003
