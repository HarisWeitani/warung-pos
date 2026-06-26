# Warung POS — Pre-Implementation Analysis

**Date:** 2026-06-26  
**Sources read:** claude-instruction.md, warung-pos-prd.md, warung-pos-architecture.md, warung-pos-roadmap.md, warung-pos-tickets.md, warung-pos-research.md

---

## 1. Project Summary

**What it is:** A personal Android POS app for a small Indonesian food stall. Two operators, 1–3 Android devices, portrait-only, distributed as a private APK (never Play Store).

**Tech stack (locked):**
- Kotlin + Jetpack Compose, single Gradle module (`:app`)
- Room = single source of truth (all UI reads from Room via Flow)
- Firebase RTDB = background sync mirror (never read by UI directly)
- Firebase Auth = email/password, 2 accounts, role via custom claim
- Hilt (KSP only, KAPT banned), WorkManager, kotlinx-serialization
- No Retrofit, no Firestore, no images, no Paging 3

**Architecture:** Clean Architecture by package (not Gradle module):
- `domain/` — Pure Kotlin models, repository interfaces, use cases
- `data/` — Room entities + DAOs, Firebase data sources, SyncCoordinator, repository impls, mappers
- `feature/` — Compose screens + ViewModels
- `core/` — DI, navigation, theme, SessionManager, NetworkMonitor, utilities

**Key rules:**
- Money is always `Long` (Rupiah) in Room entities; `Rupiah` value class in domain layer; never `Float`/`Double`
- All PKs are client-generated String UUIDs (no autoincrement)
- RTDB writes are always field-level paths — never whole-object replacement
- `SyncCoordinator.notifyPendingSync()` must be called after every repository write
- Enums stored in Room as `.name` strings (never ordinals)
- Zero hardcoded strings in Composables — both `strings.xml` + `values-id/strings.xml` in same commit

**Build order:** 6 phases — Setup → Data Layer → Domain Layer → UI Layer → Testing → Release (~87 MVP tickets, est. 17–24 days with Claude Code assistance)

---

## 2. Contradictions and Missing Information

### C-1 (Critical): KAPT still used in `warung-pos-architecture.md` Section 8
The architecture doc's dependency list uses `kapt(...)` syntax for Hilt and WorkManager:
```kotlin
kapt("com.google.dagger:hilt-android-compiler:2.52")
kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.52")
```
The claude-instruction.md explicitly bans KAPT (Section 2.1) and mandates KSP only.  
**Resolution:** claude-instruction.md wins. Use `ksp(...)` everywhere. The architecture doc dependency list is stale.

### C-2 (Critical): `ShiftRepository.closeShift()` signature disagrees across documents
- Roadmap P3-003: `closeShift(shiftId: String, countedCash: Rupiah)` — only 2 params
- TICKET-043: `closeShift(shiftId, countedCash, expectedCash, variance)` — 4 params

Who calculates `expectedCash` and `variance`? The use case (`CloseShiftUseCase`) or the repository?

### C-3 (Moderate): `SyncRepository` in architecture.md is undefined everywhere else
`domain/repository/SyncRepository.kt` appears in the architecture doc's package tree (Section 3) but is never mentioned in the roadmap, tickets, PRD, or claude-instruction. No use case calls it. No `SyncRepositoryImpl` exists. Likely an oversight from an earlier design.

### C-4 (Moderate): `PosExceptions.kt` — multiple classes in one file violates the rules
TICKET-041 puts all exception classes in a single `domain/exception/PosExceptions.kt`.  
claude-instruction.md Section 16.2: **"One class per file. No exceptions for production code."**  
Each exception class needs its own file.

### C-5 (Moderate): ExpenseCategory count discrepancy
- PRD Section FR-EXPENSE-2 lists **6** default categories: `GAS, ELECTRICITY, PACKAGING, WAGES, RENT, OTHER`
- Roadmap P2-025 / TICKET-037 repeatedly says **"5 default ExpenseCategory values"**

One of these is wrong. PRD lists 6, roadmap says 5.

### C-6 (Minor): `VoidOrderItemUseCase` package location
- Architecture doc package tree: `domain/usecase/void/VoidOrderItemUseCase.kt`
- Roadmap P3-008 / TICKET-052: listed under bill use cases

Minor but will cause confusion at creation time.

### C-7 (Minor): `observeForDate` vs `observeForDateRange` naming
- Roadmap P2-013: `observeForDate(startEpoch, endEpoch)`
- TICKET-025: `observeForDateRange(start, end)`
Same method, different names. Pick one.

### C-8 (Minor): `StockRepository` stub uses `List<Any>` instead of domain type
TICKET-043 says StockRepository stubs compile with `observeAll(): Flow<List<Any>>`. But `StockItem` domain model IS defined (TICKET-040). The stub should use `Flow<List<StockItem>>`, not `List<Any>`.

### Missing: Bill #N counter mechanism is completely unspecified
PRD FR-BILL-4: labels are "Bill #1", "Bill #2" per session. ConfirmOrderUseCase comment says "counter per shift — passed from ViewModel or tracked in repo." Neither the roadmap, tickets, nor architecture doc specifies:
- Where the counter lives
- Whether it's per-device or global
- What prevents Device 1 and Device 2 from both creating "Bill #1" in the same shift

### Missing: EncryptedSharedPreferences file naming
Both `SessionManager` (stores `deviceId`) and `AppPreferences` (stores language) use `EncryptedSharedPreferences`. No document specifies whether they share a file or use separate files, or what the key names are. On older Android versions, sharing a single EncryptedSharedPreferences instance across singletons can cause threading issues.

---

## 3. Architectural Risks

### R-1 (High): Multi-device offline shift open produces split-brain state
FR-SHIFT-2: "Only one shift can be OPEN at a time across all devices." The enforcement is `/appConfig/openShiftId` in RTDB. But if both devices are offline simultaneously and both operators try to open a shift, both will succeed locally (Room allows it). On reconnect, one device's shift will overwrite the other's `/appConfig/openShiftId` via LWW. The losing device will have an "open" shift in its local Room that is not the authoritative shift in RTDB. 

This is a real-world risk: "stall opens, internet is down, owner opens shift on Device 1, staff also opens shift on Device 2 before sync." The SyncWorker will eventually resolve the RTDB path but the Room state on the losing device is stale.

### R-2 (High): Z-Report is never synced to RTDB — loss risk
`ZReportEntity` has no sync fields and is explicitly not synced (by design). If a device is lost, stolen, or wiped after shift close but before any connectivity, the Z-report is permanently lost. The research doc acknowledges "connect at least once per day" but this is an operational assumption, not a technical safeguard. For a financial record this is a significant gap.

Suggested mitigation: write the `snapshotJson` to RTDB at a path like `/zReports/{shiftId}` at shift close (one-time write, no sync metadata needed). This was not specified in any document.

### R-3 (High): `MenuRepositoryImpl.observeMenuWithVariants()` is architecturally complex
This method must combine:
- `MenuItemDao.observeAvailable(): Flow<List<MenuItemEntity>>`
- For each item, `VariantDao.observeGroupsForItem(menuItemId): Flow<List<VariantGroupEntity>>`
- For each group, `VariantDao.observeOptionsForGroup(groupId): Flow<List<VariantOptionEntity>>`

Combining multiple nested Flows reactively (so a variant change triggers a menu re-emission) requires a `flatMapLatest` or `combine` chain that is easy to get wrong — common failure modes are: (a) variant changes not propagating to the UI, (b) resubscription storms on large menus, (c) emissions not accounting for deleted variants. This is the most architecturally complex DAO query in the app and needs careful implementation and testing.

### R-4 (Medium): SyncWorker is a monolith touching every entity type
`SyncWorker.doWork()` must query every DAO for PENDING records, serialize each entity type to the correct RTDB path, and update syncStatus on success. This is 10+ entity types in a single worker. Failure modes:
- A bug in one entity's serialization silently leaves it PENDING forever
- The worker processes some entities before a crash, leaving a partially-synced state
- Field names in Kotlin entities must match RTDB JSON keys exactly for `getValue(EntityClass::class.java)` deserialization in `RtdbListener` — a single mismatch causes silent data loss on inbound sync

### R-5 (Medium): Payment transaction window — 15-minute sync gap on crash
`PaymentRepositoryImpl.processPayment()` commits a Room `@Transaction` (bill PAID + payments inserted), then calls `syncCoordinator.notifyPendingSync()`. If the app crashes between the Room commit and the sync notification, the records stay PENDING until the next periodic WorkManager sync (15-minute backup). During those 15 minutes, Device 2 will not see the bill as PAID. The RTDB listener cannot push a change that hasn't been written to RTDB yet.

For a paid bill, this means another operator on Device 2 could attempt to process payment on the same bill. The ConflictResolver and RTDB `runTransaction()` on status change will prevent double-payment at the RTDB level, but the UX (Device 2 showing the bill as still OPEN) is confusing.

### R-6 (Medium): `selectedVariantsJson` and Firebase deserialization
`OrderItemEntity.selectedVariantsJson` is a `String` column holding serialized JSON. When `RtdbListener` receives this entity from RTDB via `getValue(OrderItemEntity::class.java)`, Firebase's reflection-based deserializer will set it as a plain String — this should work. However, if `kotlinx.serialization` escapes the JSON differently than Firebase's serialization format, or if the field is renamed/missing, the deserialization produces a null without throwing. Silent null → crash later when the mapper tries to `Json.decodeFromString(null)`.

### R-7 (Low-Medium): ConflictResolver timestamp collision
LWW uses `incoming.updatedAt > existing.updatedAt`. Both timestamps are `System.currentTimeMillis()` on the respective devices. If two devices write the same entity within the same millisecond (unlikely but possible), the tie goes to REJECT (local wins). This means the remote write is silently discarded even though it may represent a legitimate update. At warung scale with 2 devices this is extremely unlikely but worth knowing.

### R-8 (Low-Medium): First-run seeding is not idempotent on multi-device
`FirstRunManager` seeds 5 payment methods on first launch, guarded by a boolean in `AppPreferences`. Both Device 1 and Device 2 will independently seed payment methods with freshly-generated UUIDs. After sync, RTDB and Room will contain 10 payment method rows (5 from each device), all enabled, with duplicate names (Tunai × 2, QRIS × 2, etc.). The UI will show duplicate payment methods on the payment screen.

---

## 4. Resolved Decisions (Pre-Implementation)

| # | Question | Decision |
|---|---|---|
| Q1 | Bill #N counter — no multi-device | `SELECT COUNT(*) + 1 FROM bills WHERE shiftId = :shiftId` in `ConfirmOrderUseCase`. Labels are cosmetic; no cross-device sync needed. |
| Q2 | `ShiftRepository.closeShift()` signature | `CloseShiftUseCase` computes `expectedCash` and `variance` (business logic = use case). Repository signature: `closeShift(shiftId, countedCash, expectedCash, variance)`. |
| Q3 | Keep `SyncRepository` | Yes, keep it. It is the domain interface `SyncCoordinator` implements, exposing `pendingCount: Flow<Int>` and `syncState: StateFlow<SyncState>` so ViewModels (SyncStatusBar) don't import the data layer directly. |
| Q4 | Exception file structure | All exception classes go in a single `domain/exception/PosExceptions.kt`. This is an explicit owner override of the one-class-per-file rule. |
| Q5 | ZReport RTDB backup | Yes — `ShiftRepositoryImpl.closeShift()` makes a direct one-time write to `/zReports/{shiftId}` via `FirebaseRtdbDataSource` after saving to Room. No sync fields needed on `ZReportEntity`. |
| Q6 | Multi-device shift open conflict | Not a concern — only the owner opens shifts, from one device. No technical guard needed. |
| Q7 | Duplicate payment methods on first-run seeding | **Fix applied:** Use hardcoded fixed UUIDs for all 5 default payment methods (same UUID on every device). `FirstRunManager` uses `OnConflictStrategy.IGNORE` on insert — second device's seed is a no-op. |
| Q8 | ExpenseCategory count | PRD wins: **6 categories** — `GAS, ELECTRICITY, PACKAGING, WAGES, RENT, OTHER`. Roadmap's "5" is a typo. |
| Q9 | `observeMenuWithVariants()` implementation | Use Room `@Relation` + `@Transaction` data class (`MenuItemWithVariants`) assembled in a single DAO query. Simpler and more efficient than combining multiple Flows. |
| Q10 | SyncCoordinator start on cold boot | `SyncCoordinator` observes `SessionManager.currentUser` — if non-null on cold start (cached token), start `RtdbListener` immediately. Explicit login also triggers start. |
