# TICKET-001 Execution Checklist

- [x] gradle/libs.versions.toml — add versions + plugin aliases
- [x] build.gradle.kts — add plugin declarations (apply false)
- [x] app/build.gradle.kts — apply plugins, fix compileSdk/targetSdk
- [x] Verification: ./gradlew assembleDebug → BUILD SUCCESSFUL
- [x] Verification: grep for kapt → zero results
