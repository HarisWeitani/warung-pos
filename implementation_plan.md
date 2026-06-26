# Implementation Plan — TICKET-002: Gradle Dependencies

## Already present (from TICKET-001)
- `hilt-android` + `ksp(hilt-compiler)` implementation entries
- Compose BOM + UI + Material3 + Activity + Core-KTX + Lifecycle-runtime-ktx
- JUnit4, Espresso, Compose UI test

## Fetched versions
| Library | Version |
|---|---|
| Firebase BOM | 34.15.0 |
| Room | 2.8.4 |
| WorkManager | 2.11.2 |
| Navigation Compose | 2.9.8 (stable) |
| hilt-work + hilt-navigation-compose (androidx.hilt) | 1.3.0 |
| kotlinx-coroutines | 1.11.0 |
| kotlinx-serialization-json | 1.11.0 |
| security-crypto | 1.1.0 |
| Turbine | 1.2.1 |

## Files
| File | Action |
|---|---|
| `gradle/libs.versions.toml` | MODIFY — add all versions + library entries |
| `app/build.gradle.kts` | MODIFY — add all implementation/ksp/test deps |

## Deviation from ticket AC
- **MockK removed** — architecture doc was updated; Fake test doubles are used instead
- `hilt-android` + `ksp(hilt-compiler)` already present from TICKET-001 (no duplicate)
