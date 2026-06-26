# Warung POS — Implementation Roadmap

**Role:** Technical Project Manager  
**Source documents:** warung-pos-research.md, warung-pos-prd.md, warung-pos-architecture.md  
**Date:** 2026-06-25

---

## How to Use This Document

- Hand this file to Claude Code at the start of each phase along with the architecture doc and PRD
- Each task has an **ID** (e.g. `P1-001`) — reference these in commit messages
- **Scope tags:** `[MVP]` = must work on opening day · `[P2]` = PRD Phase 2 · `[P3]` = PRD Phase 3
- **Complexity:** Low · Medium · High · Critical (High + blocks many others)
- **Never skip the DoD** — Claude Code must verify each criterion before marking done
- Phases 2 and 3 are not strictly sequential within themselves; tasks within a phase can run in parallel where dependencies allow

## Phase Clarification

The 6 phases below describe **how to build** (infrastructure order). They are not the same as PRD phases (which describe **what features** to build). All MVP features span roadmap Phases 1–5. PRD Phase 2 features (stock, full reports, export) are tagged `[P2]` and can be deferred until after the MVP is running in production.

---

## Phase 1: Project Setup

> Goal: A compiling, running app with all infrastructure wired — no features yet, but the skeleton is correct and will never need to be restructured.

---

### P1-001 · Gradle Configuration — Plugins and Build Toolchain `[MVP]`

**Description:** Configure `build.gradle.kts` (project and app level) with all required plugins. Enable KSP (preferred over deprecated KAPT) for Room and Hilt annotation processing. Set `compileSdk = 35`, `targetSdk = 35`. Apply `id("com.google.gms.google-services")`, `id("com.google.dagger.hilt.android")`, `id("org.jetbrains.kotlin.plugin.serialization")`, `id("com.google.devtools.ksp")`. Confirm `google-services.json` is in `/app` root.

**Dependencies:** None — this is the first task.

**Complexity:** Medium

**Definition of Done:**
- [ ] Project syncs in Android Studio without errors
- [ ] KSP plugin active (not KAPT) — no `kapt` block exists
- [ ] `google-services.json` present and recognized (no "file not found" warning)
- [ ] `./gradlew assembleDebug` completes successfully (empty app)
- [ ] Build output shows `compileSdk 35`

---

### P1-002 · Gradle Configuration — All Dependencies `[MVP]`

**Description:** Add all production and test dependencies to `app/build.gradle.kts`. Use BOMs for Compose and Firebase to keep versions in sync. Do NOT add Retrofit or OkHttp. Reference the full dependency list from Section 8 of the architecture doc. Add `kotlinx-serialization-json`. Add `security-crypto` for EncryptedSharedPreferences. Add `kotlinx-coroutines-play-services` (required for Firebase Task → coroutine `await()` extension).

**Dependencies:** P1-001

**Complexity:** Low

**Definition of Done:**
- [ ] All BOM imports present (Compose BOM, Firebase BOM)
- [ ] Room, Hilt, Firebase RTDB, Firebase Auth, WorkManager, Navigation Compose all declared
- [ ] No Retrofit, OkHttp, Glide, Coil, Gson, or Moshi present
- [ ] `kotlinx-serialization-json` declared with matching Kotlin serialization plugin
- [ ] `./gradlew assembleDebug` still compiles clean

---

### P1-003 · Application Class and Hilt Entry Point `[MVP]`

**Description:** Create `WarungPosApplication : Application()` annotated with `@HiltAndroidApp`. Register it in `AndroidManifest.xml`. Create `MainActivity` annotated with `@AndroidEntryPoint` with `setContent { WarungPosApp() }`. `WarungPosApp()` is a top-level composable that will host the nav graph (stubbed for now).

**Dependencies:** P1-001, P1-002

**Complexity:** Low

**Definition of Done:**
- [ ] App launches without crash on emulator
- [ ] `@HiltAndroidApp` on Application class, `@AndroidEntryPoint` on Activity
- [ ] `AndroidManifest.xml` points to `WarungPosApplication`
- [ ] Logcat shows no Hilt initialization errors

---

### P1-004 · Firebase Initialization `[MVP]`

**Description:** Initialize Firebase in `WarungPosApplication.onCreate()`. Enable RTDB offline persistence with a 5 MB cache: `FirebaseDatabase.getInstance().setPersistenceEnabled(true)` and `setPersistenceCacheSizeBytes(5 * 1024 * 1024)`. Persistence must be set before any database reference is created. Add `INTERNET` permission to manifest.

**Dependencies:** P1-003

**Complexity:** Low

**Definition of Done:**
- [ ] `FirebaseApp.initializeApp()` called (or auto-initialized via `google-services` plugin — confirm which)
- [ ] `setPersistenceEnabled(true)` called before any RTDB reference
- [ ] `INTERNET` permission in manifest
- [ ] App launches with no Firebase initialization errors in Logcat
- [ ] Test: connect to RTDB in a temp debug function and verify data is readable from console

---

### P1-005 · Core Utilities — UuidGenerator, DateUtil, CurrencyFormatter `[MVP]`

**Description:** Create three utility objects in `core/util/`:
- `UuidGenerator.kt` — `fun generate(): String = UUID.randomUUID().toString()`
- `DateUtil.kt` — helpers for epoch ms ↔ display string, start-of-day, end-of-day in WIB (UTC+7)
- `CurrencyFormatter.kt` — formats `Long` Rupiah as "Rp 15.000" using `Locale("id", "ID")`. Must produce integer formatting — no decimal places.

**Dependencies:** P1-001

**Complexity:** Low

**Definition of Done:**
- [ ] `UuidGenerator.generate()` returns a valid UUID string
- [ ] `CurrencyFormatter.format(15000L)` returns `"Rp 15.000"` (Indonesian dot-thousands separator)
- [ ] `DateUtil.startOfDay(epochMs)` returns midnight WIB for given timestamp
- [ ] Unit tests pass for all three utilities (add under `test/`)

---

### P1-006 · SessionManager `[MVP]`

**Description:** Create `SessionManager` as a Hilt `@Singleton` in `core/common/`. It wraps `FirebaseAuth` and exposes: `val currentUser: StateFlow<FirebaseUser?>`, `val userRole: StateFlow<UserRole>` where `UserRole = enum { OWNER, STAFF, NONE }`, `val deviceId: String` (stable UUID stored in `EncryptedSharedPreferences`, generated once on first install). Role is read from the Firebase ID token custom claim `role`. Expose `suspend fun refreshRole()` to force token refresh.

**Dependencies:** P1-003, P1-004

**Complexity:** Medium

**Definition of Done:**
- [ ] `SessionManager` is injectable via Hilt
- [ ] `currentUser` emits `null` when not logged in, `FirebaseUser` when logged in
- [ ] `userRole` emits `NONE` when not logged in, `OWNER` or `STAFF` based on custom claim
- [ ] `deviceId` is the same UUID across app restarts (persisted in EncryptedSharedPreferences)
- [ ] Role survives app restart without re-reading token (token cached by Firebase SDK)

---

### P1-007 · NetworkMonitor `[MVP]`

**Description:** Create `NetworkMonitor` as a Hilt `@Singleton` in `core/common/`. Uses `ConnectivityManager` with `NetworkCallback` to expose `val isOnline: StateFlow<Boolean>`. Must correctly handle the case where the app starts with no network (initial value = false) and updates reactively.

**Dependencies:** P1-003

**Complexity:** Medium

**Definition of Done:**
- [ ] `isOnline` emits `true` when WiFi or mobile data is connected
- [ ] `isOnline` emits `false` when airplane mode is on
- [ ] `ACCESS_NETWORK_STATE` permission in manifest
- [ ] No memory leaks — `NetworkCallback` is unregistered when the singleton is destroyed (not applicable since it's application-scoped singleton, but document the lifecycle)

---

### P1-008 · AppPreferences — Language Setting `[MVP]`

**Description:** Create `AppPreferences` in `core/common/` using `EncryptedSharedPreferences`. Stores one key: `LANGUAGE_CODE` (`"en"` or `"id"`). Default: `"id"` (Bahasa Indonesia). Exposes `fun getLanguage(): String` and `fun setLanguage(code: String)`. Used by the UI to apply `LocalConfiguration` override for Compose.

**Dependencies:** P1-003

**Complexity:** Low

**Definition of Done:**
- [ ] Language preference persists across app restarts
- [ ] Default is `"id"` when no preference is set
- [ ] EncryptedSharedPreferences used (not plain SharedPreferences)
- [ ] Setting `"en"` and restarting returns `"en"`

---

### P1-009 · App Theme — Material 3 Design System `[MVP]`

**Description:** Create `core/theme/` with `Theme.kt`, `Color.kt`, `Type.kt`. Use Material 3. Define a color palette appropriate for a food-stall POS — warm, readable, high contrast for outdoor/kitchen use. Define typography scale. Apply `WarungPosTheme` in `MainActivity.setContent`. Ensure all Compose screens use `MaterialTheme.*` tokens, never hardcoded colors.

**Dependencies:** P1-003

**Complexity:** Low

**Definition of Done:**
- [ ] App renders with custom theme, not the default Material 3 placeholder
- [ ] Light mode works (dark mode is optional for MVP)
- [ ] All theme tokens (`MaterialTheme.colorScheme`, `MaterialTheme.typography`) are defined
- [ ] No hardcoded color literals in any Composable

---

### P1-010 · String Resources — Bilingual Setup `[MVP]`

**Description:** Create `res/values/strings.xml` (English, default) and `res/values-id/strings.xml` (Bahasa Indonesia). Populate with app name and placeholder strings for all top-level navigation labels. Enforce the rule from PRD FR-I18N-3a: every string added in development must appear in both files in the same commit. Add a `[TODO-ID]` comment convention for strings not yet translated.

**Dependencies:** P1-001

**Complexity:** Low

**Definition of Done:**
- [ ] Both `strings.xml` files exist
- [ ] App name appears in both
- [ ] All bottom nav labels have entries in both files
- [ ] Language switch mechanism tested: change device locale to Indonesian → verify Indonesian strings appear
- [ ] No hardcoded English strings in any `.kt` file (enforced by grep: `"[A-Z][a-z]"` in Composables should return only technical identifiers, not display text)

---

### P1-011 · Navigation Skeleton — Routes and NavHost `[MVP]`

**Description:** Create `core/navigation/Routes.kt` as a sealed class defining all routes. Create `AppNavGraph.kt` with `NavHost` and `composable()` entries for every screen (all rendering empty `Box` stubs for now). Wire bottom navigation bar to top-level destinations: Order, Tables, Reports (owner-only), More. Reports tab is conditionally shown based on `SessionManager.userRole`. Role guard: if a staff user navigates to an owner-only route directly, redirect to Order screen.

**Dependencies:** P1-003, P1-006, P1-009

**Complexity:** Medium

**Definition of Done:**
- [ ] `Routes.kt` contains all route constants matching the architecture package structure
- [ ] Bottom nav renders with 4 items; Reports hidden when role = STAFF
- [ ] Navigating to each route shows the stub screen without crash
- [ ] Back stack behaves correctly (back from nested screen returns to nav destination, not exits app)
- [ ] Deep-linking to owner-only route with staff role redirects to Order

---

### P1-012 · ProGuard / R8 Rules `[MVP]`

**Description:** Configure `proguard-rules.pro` for:
- `kotlinx-serialization` — keep serializers and serializable classes
- Firebase SDK — keep all `com.google.firebase.**`
- Room — keep all `@Entity`, `@Dao`, `@Database` classes
- Hilt — standard Hilt rules (usually auto-applied by plugin, verify)
- `EncryptedSharedPreferences` — keep if needed

**Dependencies:** P1-002

**Complexity:** Low

**Definition of Done:**
- [ ] `./gradlew assembleRelease` with minification enabled produces an APK that launches without crash
- [ ] Firebase connects in release build
- [ ] Room queries work in release build (no entity class stripped)
- [ ] `kotlinx.serialization` deserialization of `selectedVariantsJson` works in release build

---

### P1-013 · Version Gate Stub `[MVP]`

**Description:** In `AppNavGraph` or a dedicated `AppStartup` composable, add the version gate check. On app start, read `/appConfig/minVersionCode` from RTDB. If `BuildConfig.VERSION_CODE < minVersionCode`, show `UpdateRequiredScreen` (full-screen, non-dismissable). If RTDB is unreachable (offline), skip the check and allow app to proceed (don't block offline use). If online and check passes, proceed normally.

**Dependencies:** P1-004, P1-011

**Complexity:** Medium

**Definition of Done:**
- [ ] Version gate check runs once on app start when online
- [ ] `UpdateRequiredScreen` shows when version code is below minimum (test by temporarily setting a high value in RTDB)
- [ ] App proceeds normally when offline (gate check is skipped gracefully)
- [ ] App proceeds normally when version code >= minimum

---

### ✅ Phase 1 Gate

Before proceeding to Phase 2, verify:
- [ ] `./gradlew assembleDebug` and `./gradlew assembleRelease` both succeed
- [ ] App launches, shows bottom nav, navigates between stub screens
- [ ] Firebase RTDB and Auth are initialized with no errors
- [ ] Theme and bilingual strings are working
- [ ] All utilities are unit-tested

---

## Phase 2: Data Layer

> Goal: All Room entities, DAOs, and the complete database exist and compile. All Firebase data sources and sync infrastructure are wired. Hilt modules bind everything. No domain logic yet — repositories are not implemented in this phase (they bridge domain ↔ data and belong in Phase 3 after domain models exist).

**Important:** Stock entities (`StockItem`, `StockBatch`, `MenuItemIngredient`, `StockOpname`, `StockOpnameLine`) are defined in this phase even though their screens and use cases are PRD Phase 2 features. This is mandatory to avoid Room schema migrations later (see PRD FR-STOCK-0).

---

### P2-001 · Room Type Converters `[MVP]`

**Description:** Create `data/local/db/Converters.kt` with `@TypeConverter` functions for all non-primitive types stored in Room:
- `Long ↔ Instant` / epoch ms (store as Long)
- `String ↔ SyncStatus enum` (PENDING, SYNCED, CONFLICTED)
- `String ↔ BillType enum` (UPFRONT, OPEN_BILL)
- `String ↔ BillStatus enum` (OPEN, PAID, VOID)
- `String ↔ OrderItemStatus enum` (ORDERED, DONE, VOID)
- `String ↔ ShiftStatus enum` (OPEN, CLOSED)
- `String ↔ OpnameStatus enum` (IN_PROGRESS, COMPLETED, CANCELLED)
- `String ↔ VariantSelectionType enum` (SINGLE, MULTIPLE)
- `String ↔ VoidReason enum`
- `String ↔ ExpenseCategory enum`
- `String ↔ VarianceReason enum`

All enums stored as String names (not ordinals — ordinals break on reorder).

**Dependencies:** P1-001

**Complexity:** Low

**Definition of Done:**
- [ ] `@TypeConverters(Converters::class)` annotation present on database class
- [ ] Every enum has both a `to` and `from` converter
- [ ] No ordinal-based converters — only `.name` / `enumValueOf<>()`
- [ ] Compiles without Room processor errors

---

### P2-002 · Sync Metadata — Shared Fields Convention `[MVP]`

**Description:** Define the sync metadata pattern. Since Room does not support Kotlin interface delegation or mixins cleanly, create a convention doc (inline comment or `SyncableEntity.kt` interface) specifying that every syncable entity must have these three fields: `val updatedAt: Long`, `val syncStatus: String` (stored as String, maps to `SyncStatus` enum), `val deviceId: String`. This is a documentation task — the fields will be manually added to each entity. Create `SyncStatus.kt` enum in `core/common/`.

**Dependencies:** P2-001

**Complexity:** Low

**Definition of Done:**
- [ ] `SyncStatus` enum exists: `PENDING, SYNCED, CONFLICTED`
- [ ] Documented convention exists (comment block or companion README) explaining the 3 required fields
- [ ] Verified that the type converter handles `SyncStatus ↔ String` correctly

---

### P2-003 · Room Entities — Menu `[MVP]`

**Description:** Create Room `@Entity` classes in `data/local/entity/`:
- `MenuCategoryEntity` — `id(String PK)`, `name`, `sortOrder(Int)`, sync fields
- `MenuItemEntity` — `id(String PK)`, `categoryId(String FK)`, `name`, `basePrice(Long)`, `isAvailable(Boolean)`, `isSoldOut(Boolean)`, sync fields. FK references `MenuCategoryEntity.id` with `onDelete = SET_NULL`.
- `VariantGroupEntity` — `id(String PK)`, `menuItemId(String FK)`, `name`, `selectionType(String)`, `isRequired(Boolean)`, sync fields
- `VariantOptionEntity` — `id(String PK)`, `variantGroupId(String FK)`, `name`, `priceDelta(Long)`, sync fields

**Dependencies:** P2-001, P2-002

**Complexity:** Low

**Definition of Done:**
- [ ] All 4 entities compile with Room processor
- [ ] FK constraints declared with `ForeignKey` annotation (not just field naming)
- [ ] `@Index` on all FK columns (Room warns without this)
- [ ] No `Float` or `Double` for price fields — only `Long`

---

### P2-004 · Room Entities — Tables and Bills `[MVP]`

**Description:** Create:
- `TableEntity` — `id(String PK)`, `label(String?)`, `isActive(Boolean)`, sync fields
- `BillEntity` — `id(String PK)`, `tableId(String? FK)`, `type(String)`, `status(String)`, `sessionLabel(String)`, `createdAt(Long)`, `paidAt(Long?)`, `subtotal(Long)`, `discountTotal(Long)`, `grandTotal(Long)`, `note(String?)`, `shiftId(String? FK)`, `voidReason(String?)`, `voidedBy(String?)`, sync fields
- `OrderItemEntity` — `id(String PK)`, `billId(String FK)`, `menuItemId(String FK)`, `nameSnapshot(String)`, `priceSnapshot(Long)`, `quantity(Int)`, `selectedVariantsJson(String)`, `lineTotal(Long)`, `status(String)`, `voidReason(String?)`, `voidedBy(String?)`, `createdAt(Long)`, sync fields

`selectedVariantsJson` serializes a `List<SelectedVariant>` using `kotlinx.serialization`.

**Dependencies:** P2-003

**Complexity:** Medium

**Definition of Done:**
- [ ] All 3 entities compile
- [ ] `BillEntity.tableId` FK is nullable (grab-and-go bills have no table)
- [ ] `OrderItemEntity.billId` FK is non-nullable with `onDelete = CASCADE`
- [ ] `selectedVariantsJson` is a plain `String` column — no Room type converter needed
- [ ] `@Index` on `billId`, `tableId`, `shiftId`, `status`

---

### P2-005 · Room Entities — Payment `[MVP]`

**Description:** Create:
- `PaymentMethodEntity` — `id(String PK)`, `name(String)`, `isEnabled(Boolean)`, `sortOrder(Int)`, sync fields
- `PaymentEntity` — `id(String PK)`, `billId(String FK)`, `paymentMethodId(String FK)`, `amount(Long)`, `amountTendered(Long?)`, `changeGiven(Long?)`, `paidAt(Long)`, sync fields

**Dependencies:** P2-004

**Complexity:** Low

**Definition of Done:**
- [ ] Both entities compile
- [ ] `PaymentEntity.billId` FK with `onDelete = CASCADE`
- [ ] `@Index` on `billId`
- [ ] `amountTendered` and `changeGiven` are nullable — only relevant for cash payments

---

### P2-006 · Room Entities — Shift and ZReport `[MVP]`

**Description:** Create:
- `ShiftEntity` — `id(String PK)`, `openedAt(Long)`, `closedAt(Long?)`, `openedBy(String)`, `openingFloat(Long)`, `countedCash(Long?)`, `expectedCash(Long)`, `variance(Long?)`, `status(String)`, sync fields
- `ZReportEntity` — `id(String PK)`, `shiftId(String FK unique)`, `snapshotJson(String)`, `createdAt(Long)`. No sync fields — ZReport is immutable after creation and does not sync (snapshot only).

`snapshotJson` stores the full Z-report as a serialized JSON string. It is never mutated after insert.

**Dependencies:** P2-004

**Complexity:** Low

**Definition of Done:**
- [ ] Both entities compile
- [ ] `ZReportEntity.shiftId` has `@Index(unique = true)` — one report per shift
- [ ] `ShiftEntity.status` uses the `ShiftStatus` type converter
- [ ] `ZReportEntity` has no `syncStatus` field — it is explicitly not synced

---

### P2-007 · Room Entities — Expense `[MVP]`

**Description:** Create:
- `ExpenseEntity` — `id(String PK)`, `category(String)`, `amount(Long)`, `date(Long)` (epoch ms), `note(String?)`, `recordedBy(String)`, sync fields

**Dependencies:** P2-002

**Complexity:** Low

**Definition of Done:**
- [ ] Entity compiles
- [ ] `category` uses `ExpenseCategory` type converter
- [ ] `@Index` on `date` (reports query by date range)

---

### P2-008 · Room Entities — Stock (Schema Only) `[P2]`

**Description:** Create Room entities for stock — these are **schema-only in MVP** (tables exist but are empty until PRD Phase 2 features are built):
- `StockItemEntity` — `id(String PK)`, `name(String)`, `unit(String)`, `currentQuantity(Double)`, `lowStockThreshold(Double)`, `lastCostPrice(Long)`, sync fields
- `StockBatchEntity` — `id(String PK)`, `stockItemId(String FK)`, `quantity(Double)`, `purchasePrice(Long)`, `purchaseDate(Long)`, `supplier(String?)`, sync fields
- `MenuItemIngredientEntity` — composite PK `(menuItemId, stockItemId)`, `quantityUsed(Double)`, sync fields
- `StockOpnameEntity` — `id(String PK)`, `startedAt(Long)`, `completedAt(Long?)`, `status(String)`, `conductedBy(String)`, `note(String?)`, sync fields
- `StockOpnameLineEntity` — `id(String PK)`, `opnameId(String FK)`, `stockItemId(String FK)`, `expectedQty(Double)`, `actualQty(Double)`, `variance(Double)`, `varianceReason(String)`, `costImpact(Long)`, sync fields

**Dependencies:** P2-003

**Complexity:** Medium

**Definition of Done:**
- [ ] All 5 entities compile with Room processor
- [ ] `MenuItemIngredientEntity` uses `@Entity(primaryKeys = ["menuItemId", "stockItemId"])`
- [ ] All FK constraints declared
- [ ] These entities are listed in `@Database(entities = [...])` in `WarungDatabase`
- [ ] **These entities produce no UI, use cases, or repositories in Phase 2** — that is correct and expected

---

### P2-009 · WarungDatabase `[MVP]`

**Description:** Create `data/local/db/WarungDatabase.kt` with `@Database` annotation listing ALL entities from P2-003 through P2-008 (including stock entities). Set `version = 1`. Apply `@TypeConverters(Converters::class)`. Build with `Room.databaseBuilder` using `"warung_pos_db"` as the name. Provide as Hilt `@Singleton` in `AppModule`. **Do not set `fallbackToDestructiveMigration()`** — add explicit migration stubs instead, even for version 1 to 2 (future-proofing).

**Dependencies:** P2-001 through P2-008

**Complexity:** Medium — must list ALL entities correctly or Room processor fails

**Definition of Done:**
- [ ] Database compiles with all entities listed
- [ ] Room generates `WarungDatabase_Impl` (visible in build outputs)
- [ ] App launches with database created at correct path (verify with `adb shell ls /data/data/com.warungpos/databases/`)
- [ ] All stock entities included even with no use cases pointing at them
- [ ] Version = 1, no destructive migration fallback

---

### P2-010 · Room DAOs — Menu `[MVP]`

**Description:** Create DAOs in `data/local/dao/`:

`MenuCategoryDao.kt`:
- `fun observeAll(): Flow<List<MenuCategoryEntity>>`
- `suspend fun upsert(entity: MenuCategoryEntity)`
- `suspend fun delete(id: String)`
- `suspend fun getById(id: String): MenuCategoryEntity?`

`MenuItemDao.kt`:
- `fun observeAvailable(): Flow<List<MenuItemEntity>>` — `WHERE isAvailable = 1`
- `fun observeAll(): Flow<List<MenuItemEntity>>` — including unavailable (for management screen)
- `suspend fun upsert(entity: MenuItemEntity)`
- `suspend fun softDelete(id: String)` — `UPDATE SET isAvailable = 0`
- `suspend fun setSoldOut(id: String, isSoldOut: Boolean)`
- `suspend fun resetAllSoldOut()` — `UPDATE SET isSoldOut = 0` (all items)
- `suspend fun getPendingSync(): List<MenuItemEntity>` — `WHERE syncStatus = 'PENDING'`

`VariantDao.kt`:
- `fun observeGroupsForItem(menuItemId: String): Flow<List<VariantGroupEntity>>`
- `fun observeOptionsForGroup(groupId: String): Flow<List<VariantOptionEntity>>`
- `suspend fun upsertGroup(entity: VariantGroupEntity)`
- `suspend fun upsertOption(entity: VariantOptionEntity)`
- `suspend fun deleteGroup(id: String)` — cascade deletes options via FK
- `suspend fun deleteOption(id: String)`

**Dependencies:** P2-003, P2-009

**Complexity:** Medium

**Definition of Done:**
- [ ] All DAO methods compile with Room processor
- [ ] `observeAvailable()` correctly filters `isSoldOut` — available vs sold-out are different flags. `isAvailable = 0` means hidden from menu entirely; `isSoldOut = 1` means visible but greyed out. Both cases should be reflected in what the order screen shows.
- [ ] `resetAllSoldOut()` updates all rows regardless of category

---

### P2-011 · Room DAOs — Tables and Bills `[MVP]`

**Description:** Create:

`TableDao.kt`:
- `fun observeActive(): Flow<List<TableEntity>>`
- `fun observeAll(): Flow<List<TableEntity>>`
- `suspend fun upsert(entity: TableEntity)`
- `suspend fun deactivate(id: String)`

`BillDao.kt`:
- `fun observeOpenBills(): Flow<List<BillEntity>>` — `WHERE status = 'OPEN'`
- `fun observeOpenBillsForTable(tableId: String): Flow<List<BillEntity>>`
- `fun observeBillsForShift(shiftId: String): Flow<List<BillEntity>>`
- `suspend fun getById(id: String): BillEntity?`
- `suspend fun insert(entity: BillEntity)`
- `suspend fun updateStatus(id: String, status: String, paidAt: Long?, shiftId: String?)`
- `suspend fun voidBill(id: String, reason: String, voidedBy: String, updatedAt: Long)`
- `suspend fun countOpenBills(): Int` — used by shift close pre-check
- `suspend fun getPendingSync(): List<BillEntity>`

`OrderItemDao.kt`:
- `fun observeForBill(billId: String): Flow<List<OrderItemEntity>>`
- `suspend fun insertAll(items: List<OrderItemEntity>)`
- `suspend fun voidItem(id: String, reason: String, voidedBy: String, updatedAt: Long)`
- `suspend fun getPendingSync(): List<OrderItemEntity>`

**Dependencies:** P2-004, P2-009

**Complexity:** Medium

**Definition of Done:**
- [ ] `countOpenBills()` is used in shift close pre-condition (verify in use case later)
- [ ] `updateStatus` is used within a `@Transaction` by the repository — not called directly with inconsistent state
- [ ] All `observe*` methods return `Flow` (not suspend)
- [ ] All `insert/update/delete` methods are `suspend`

---

### P2-012 · Room DAOs — Payment and PaymentMethod `[MVP]`

**Description:** Create:

`PaymentMethodDao.kt`:
- `fun observeEnabled(): Flow<List<PaymentMethodEntity>>`
- `fun observeAll(): Flow<List<PaymentMethodEntity>>`
- `suspend fun upsert(entity: PaymentMethodEntity)`
- `suspend fun setEnabled(id: String, enabled: Boolean)`
- `suspend fun updateSortOrder(id: String, sortOrder: Int)`
- `suspend fun insertDefaults(methods: List<PaymentMethodEntity>)` — used for first-run seed

`PaymentDao.kt`:
- `fun observeForBill(billId: String): Flow<List<PaymentEntity>>`
- `suspend fun insertAll(payments: List<PaymentEntity>)`
- `suspend fun sumForShiftByMethod(shiftId: String): List<PaymentSumByMethod>` — data class for report
- `suspend fun getPendingSync(): List<PaymentEntity>`

`PaymentSumByMethod` is a Room POJO (not an entity): `data class PaymentSumByMethod(val paymentMethodId: String, val total: Long)`

**Dependencies:** P2-005, P2-009

**Complexity:** Medium

**Definition of Done:**
- [ ] `sumForShiftByMethod` uses `@Query` with `GROUP BY paymentMethodId` and `JOIN` to payments table
- [ ] `insertDefaults` used during first-run setup (called from AppModule or a startup routine)
- [ ] Default payment methods seeded: Tunai (Cash), QRIS, GoPay, OVO, Transfer Bank — all enabled, sort orders 1–5

---

### P2-013 · Room DAOs — Shift and Expense `[MVP]`

**Description:** Create:

`ShiftDao.kt`:
- `fun observeOpenShift(): Flow<ShiftEntity?>` — `WHERE status = 'OPEN' LIMIT 1`
- `suspend fun getOpenShift(): ShiftEntity?`
- `suspend fun insert(entity: ShiftEntity)`
- `suspend fun closeShift(id: String, closedAt: Long, countedCash: Long, expectedCash: Long, variance: Long, updatedAt: Long)`
- `fun observeHistory(): Flow<List<ShiftEntity>>` — closed shifts, DESC order

`ZReportDao.kt`:
- `suspend fun insert(entity: ZReportEntity)`
- `suspend fun getByShiftId(shiftId: String): ZReportEntity?`
- `fun observeAll(): Flow<List<ZReportEntity>>`

`ExpenseDao.kt`:
- `fun observeForDate(startEpoch: Long, endEpoch: Long): Flow<List<ExpenseEntity>>`
- `suspend fun insert(entity: ExpenseEntity)`
- `suspend fun sumForShift(shiftId: String): Long` — aggregate for Z-report
- `suspend fun sumForDateRange(startEpoch: Long, endEpoch: Long): Long`
- `suspend fun getPendingSync(): List<ExpenseEntity>`

**Dependencies:** P2-006, P2-007, P2-009

**Complexity:** Medium

**Definition of Done:**
- [ ] `observeOpenShift()` emits `null` when no shift is open — this is the primary "shift guard" for order taking
- [ ] `closeShift()` is wrapped in a `@Transaction` in the repository, not called in isolation
- [ ] `ZReportEntity` insert is idempotent (use `OnConflictStrategy.REPLACE` or `IGNORE`)

---

### P2-014 · Room DAOs — Stock (Stub) `[P2]`

**Description:** Create `StockDao.kt` and `StockOpnameDao.kt` with minimal stub implementations. These will be fully populated in PRD Phase 2 but the DAOs must exist for the Hilt module to compile.

`StockDao.kt` stubs:
- `fun observeAll(): Flow<List<StockItemEntity>>`
- `suspend fun upsertItem(entity: StockItemEntity)`
- `suspend fun upsertBatch(entity: StockBatchEntity)`
- `suspend fun decrementQuantity(stockItemId: String, amount: Double)`
- `fun observeLowStock(): Flow<List<StockItemEntity>>`

`StockOpnameDao.kt` stubs:
- `suspend fun insertSession(entity: StockOpnameEntity)`
- `fun observeActiveSession(): Flow<StockOpnameEntity?>`
- `suspend fun insertLine(entity: StockOpnameLineEntity)`

**Dependencies:** P2-008, P2-009

**Complexity:** Low

**Definition of Done:**
- [ ] Both DAOs compile with Room processor
- [ ] Methods are `@Query`-backed even if the queries are simple (no TODO stubs — write real SQL)
- [ ] `decrementQuantity` uses `UPDATE SET currentQuantity = currentQuantity - :amount` — not a read-modify-write

---

### P2-015 · Room DAOs — Report Queries `[MVP]`

**Description:** Create `ReportQueryDao.kt` with aggregate query methods used by reports:

- `suspend fun totalSalesForDateRange(startEpoch: Long, endEpoch: Long): Long` — `SUM(grandTotal)` from PAID bills
- `suspend fun transactionCountForDateRange(startEpoch: Long, endEpoch: Long): Int`
- `fun observeBestSellers(startEpoch: Long, endEpoch: Long, limit: Int): Flow<List<BestSellerRow>>` — JOIN OrderItem, GROUP BY menuItemId, ORDER BY SUM(quantity) DESC
- `suspend fun salesByPaymentMethod(startEpoch: Long, endEpoch: Long): List<PaymentSumByMethod>`
- `suspend fun totalVoidsForShift(shiftId: String): VoidSummary` — count + total value

`BestSellerRow` POJO: `data class BestSellerRow(val menuItemId: String, val nameSnapshot: String, val totalQty: Int, val totalRevenue: Long)`
`VoidSummary` POJO: `data class VoidSummary(val count: Int, val totalValue: Long)`

**Dependencies:** P2-011, P2-012, P2-009

**Complexity:** High — complex SQL joins, must be correct for report accuracy

**Definition of Done:**
- [ ] All queries verified against Room's schema (no runtime SQL errors — run on instrumented test or emulator)
- [ ] `totalSalesForDateRange` filters `WHERE status = 'PAID' AND paidAt BETWEEN :start AND :end`
- [ ] `bestSellers` excludes VOID OrderItems from the quantity sum
- [ ] All POJOs are in a `data/local/dao/pojo/` subpackage
- [ ] At least one SQL query tested with in-memory Room DB (see Phase 5)

---

### P2-016 · Firebase Auth Data Source `[MVP]`

**Description:** Create `data/remote/firebase/FirebaseAuthDataSource.kt`. Wraps `FirebaseAuth` with coroutine-friendly suspend functions:
- `suspend fun signIn(email: String, password: String): Result<FirebaseUser>`
- `suspend fun signOut()`
- `fun observeAuthState(): Flow<FirebaseUser?>` — uses `callbackFlow` wrapping `AuthStateListener`
- `suspend fun getUserRole(): UserRole` — force-refreshes the ID token and reads the `role` claim

**Dependencies:** P1-004, P1-006

**Complexity:** Medium

**Definition of Done:**
- [ ] `signIn` returns `Result.failure` with a typed exception on wrong credentials (not a crash)
- [ ] `observeAuthState` emits on login, logout, and token refresh
- [ ] `getUserRole` reads from `idTokenResult.claims["role"]` — returns `UserRole.NONE` if claim is absent
- [ ] `callbackFlow` in `observeAuthState` properly calls `awaitClose { removeAuthStateListener(...) }`

---

### P2-017 · Firebase RTDB Data Source `[MVP]`

**Description:** Create `data/remote/firebase/FirebaseRtdbDataSource.kt`. Provides low-level RTDB operations:
- `suspend fun write(path: String, value: Any): Result<Unit>` — field-level `setValue`
- `suspend fun writeMulti(updates: Map<String, Any?>): Result<Unit>` — `updateChildren` for atomic multi-path
- `suspend fun runTransaction(path: String, block: (MutableData) -> Transaction.Result): Result<Unit>`
- `fun observe(path: String): Flow<DataSnapshot>` — `callbackFlow` with `ValueEventListener`
- `fun observeChildren(path: String): Flow<DataSnapshot>` — `callbackFlow` with `ChildEventListener`
- `suspend fun increment(path: String, delta: Double): Result<Unit>` — `ServerValue.increment(delta)`
- `suspend fun get(path: String): Result<DataSnapshot>`
- `suspend fun delete(path: String): Result<Unit>`

**Dependencies:** P1-004

**Complexity:** High — async Firebase callbacks → coroutines requires careful `callbackFlow` usage

**Definition of Done:**
- [ ] `observe()` properly unregisters `ValueEventListener` in `awaitClose {}`
- [ ] `write()` uses `await()` from `kotlinx-coroutines-play-services` (not manual callbacks)
- [ ] `runTransaction()` properly handles `doTransaction` returning `Transaction.abort()` on error
- [ ] `increment()` uses `ServerValue.increment()` (atomic server-side, not read-modify-write)
- [ ] All methods return `Result<T>` — never throw unchecked exceptions to callers

---

### P2-018 · ConflictResolver `[MVP]`

**Description:** Create `data/remote/sync/ConflictResolver.kt`. Implements last-write-wins (LWW) by `updatedAt` timestamp. The logic: given an incoming snapshot from RTDB and the existing record in Room, if `incoming.updatedAt > room.updatedAt` → the incoming version wins and should be written to Room. If `incoming.updatedAt <= room.updatedAt` → discard the incoming update (local is at least as fresh).

Also handles the bill-status forward-only rule: if incoming `status` would move a bill backwards (e.g., PAID → OPEN), always reject it regardless of timestamp.

**Dependencies:** P2-002

**Complexity:** Medium

**Definition of Done:**
- [ ] `resolve(incoming: Map<String, Any?>, existingUpdatedAt: Long?, existingStatus: String?): ConflictResolution` returns `ACCEPT` or `REJECT`
- [ ] Bill status regression (PAID → OPEN, VOID → anything) always returns `REJECT`
- [ ] `existingUpdatedAt == null` (record doesn't exist locally) → always `ACCEPT`
- [ ] Unit tested with all conflict scenarios (see Phase 5)

---

### P2-019 · RtdbListener `[MVP]`

**Description:** Create `data/remote/sync/RtdbListener.kt`. Sets up `ChildEventListener`s on all top-level RTDB paths. When a remote change arrives, calls `ConflictResolver` and, if `ACCEPT`, writes the change to Room via the appropriate DAO. This class is started by `SyncCoordinator` and runs for the lifetime of the app.

Paths to listen on: `/bills`, `/orderItems`, `/payments`, `/menuItems`, `/menuCategories`, `/variantGroups`, `/variantOptions`, `/tables`, `/paymentMethods`, `/shifts`, `/expenses`, `/stockItems` (P2), `/opnames` (P2).

**Dependencies:** P2-017, P2-018, P2-011, P2-012, P2-013

**Complexity:** High — must handle all entity types and map RTDB JSON → Room entity correctly

**Definition of Done:**
- [ ] Listener correctly maps RTDB `DataSnapshot` → Room entity for all MVP entity types
- [ ] `ConflictResolver` is called for every inbound change before writing to Room
- [ ] Listener is started once and handles reconnects (Firebase SDK handles this automatically)
- [ ] `onCancelled` logs the error but does not crash the app
- [ ] Stock listeners are registered but no-op in MVP (data written to Room but no UI shows it)

---

### P2-020 · SyncWorker `[MVP]`

**Description:** Create `data/remote/sync/SyncWorker.kt` extending `CoroutineWorker`. Injected with Hilt (`@HiltWorker`). On `doWork()`: query all DAOs for entities with `syncStatus = PENDING`, push each to RTDB via `FirebaseRtdbDataSource.write()`, on success update `syncStatus = SYNCED` in Room. Uses `Result.retry()` on transient failures. Uses `Result.success()` when all pending records are synced or there are none.

WorkManager constraints: `NetworkType.CONNECTED`. Retry policy: `BackoffPolicy.EXPONENTIAL` with 10-second initial delay.

**Dependencies:** P2-017, P2-010 through P2-015

**Complexity:** High

**Definition of Done:**
- [ ] `@HiltWorker` annotation and Hilt injection work correctly (common failure point)
- [ ] `WorkerModule.kt` registers `HiltWorkerFactory` with WorkManager
- [ ] Worker processes all entity types with `PENDING` status in `updatedAt` ascending order
- [ ] Worker sets `syncStatus = SYNCED` after successful RTDB write for each record
- [ ] Worker sets `syncStatus = CONFLICTED` if RTDB returns a non-retryable error
- [ ] Verified: starting worker offline queues it; connecting to network triggers execution

---

### P2-021 · SyncCoordinator `[MVP]`

**Description:** Create `data/remote/sync/SyncCoordinator.kt` as a Hilt `@Singleton`. Responsibilities:
1. Start `RtdbListener` when the user is authenticated
2. Expose `fun notifyPendingSync()` — enqueues `SyncWorker` as a `OneTimeWorkRequest` with network constraint
3. Expose `val pendingCount: Flow<Int>` — Room query counting `syncStatus = PENDING` across all entities
4. Expose `val syncState: StateFlow<SyncState>` where `SyncState = IDLE | SYNCING | OFFLINE | ERROR`
5. Start a periodic `PeriodicWorkRequest` for backup sync every 15 minutes

**Dependencies:** P2-019, P2-020, P1-007

**Complexity:** High — orchestrates multiple systems

**Definition of Done:**
- [ ] `notifyPendingSync()` enqueues work correctly — does not create duplicate workers (use `ExistingWorkPolicy.REPLACE` or `APPEND_OR_REPLACE`)
- [ ] `pendingCount` emits 0 when all records are SYNCED, N when any are PENDING
- [ ] `syncState` correctly transitions: OFFLINE when `NetworkMonitor.isOnline = false`, SYNCING when worker is running, IDLE when done
- [ ] `RtdbListener` is only started after successful auth (not before login)
- [ ] `SyncCoordinator` is exposed in `AppModule` as `@Singleton`

---

### P2-022 · Hilt DI Modules `[MVP]`

**Description:** Create the three Hilt modules:

`AppModule.kt`:
- `@Provides @Singleton WarungDatabase` — `Room.databaseBuilder(...)`
- `@Provides @Singleton FirebaseDatabase` — `FirebaseDatabase.getInstance()`
- `@Provides @Singleton FirebaseAuth` — `FirebaseAuth.getInstance()`
- `@Provides` for all DAOs (from the database)
- `@Provides @Singleton FirebaseRtdbDataSource`
- `@Provides @Singleton FirebaseAuthDataSource`
- `@Provides @Singleton SyncCoordinator`

`RepositoryModule.kt` (all `@Binds`):
- All repository interface → implementation bindings (will be filled in Phase 3 after implementations exist — create this file as empty `@Module @InstallIn(SingletonComponent::class)` now)

`WorkerModule.kt`:
- `@Provides WorkManagerConfiguration` using `HiltWorkerFactory`
- Must disable default WorkManager initialization in `AndroidManifest.xml` (`<provider>` removal) and initialize manually

**Dependencies:** P2-009, P2-016, P2-017, P2-021

**Complexity:** Medium — but easy to get wrong (circular dependencies, wrong scope)

**Definition of Done:**
- [ ] App builds and launches without Hilt graph errors
- [ ] All DAOs injectable in test classes
- [ ] `WorkManagerConfiguration` uses `HiltWorkerFactory` (verify by checking that `SyncWorker` injects its dependencies correctly)
- [ ] `RepositoryModule.kt` exists (even if empty) — prevents "missing binding" errors when repository implementations are added in Phase 3

---

### P2-023 · RTDB Security Rules `[MVP]`

**Description:** Write Firebase Realtime Database security rules in `firebase/database.rules.json`. Rules must:
- Require authentication for all reads and writes
- Allow reads/writes only where `auth != null`
- Owner-only paths (reports aggregates are computed locally, so no special rules needed — the data is in Room)
- Allow write to `/bills/{id}/status` only if the new value is "PAID" or "VOID" (forward-only status progression)
- Allow any authenticated user to write to `/bills`, `/orderItems`, `/payments`, `/expenses`
- Allow any authenticated user to read all paths

**Dependencies:** P1-004

**Complexity:** Medium

**Definition of Done:**
- [ ] Rules deployed to Firebase project (via Firebase CLI or console)
- [ ] Unauthenticated read returns `Permission denied`
- [ ] Authenticated read returns data correctly
- [ ] Bill status regression rejected by rules (test: try to set `status = "OPEN"` on a PAID bill via Firebase REST API or console — should fail)
- [ ] Rules file committed to the repository as `firebase/database.rules.json`

---

### P2-024 · AppConfig in RTDB `[MVP]`

**Description:** Manually set `/appConfig/minVersionCode: 1` and `/appConfig/openShiftId: null` in the Firebase RTDB console. Document the RTDB path structure matching Appendix C of the architecture document. This is a one-time manual setup task, not code.

**Dependencies:** P1-004

**Complexity:** Low

**Definition of Done:**
- [ ] `/appConfig/minVersionCode` exists and equals `1` in RTDB console
- [ ] `/appConfig/openShiftId` exists and equals `null` (or the string "null") in RTDB
- [ ] Path structure matches architecture Appendix C exactly
- [ ] P1-013 version gate reads this value correctly (test: set to 999, verify gate triggers)

---

### P2-025 · First-Run Data Seeding `[MVP]`

**Description:** Create a `FirstRunManager` that runs once on first app launch (tracked via a boolean in `AppPreferences`). Seeds Room with:
- 5 default `PaymentMethodEntity` rows: Tunai (Cash), QRIS, GoPay, OVO, Transfer Bank — all enabled, sortOrder 1–5
- 5 default `ExpenseCategory` values (stored as a setting, not a Room entity — just enum defaults)
These are not synced to RTDB — each device initializes from defaults. If a user later modifies payment methods, those changes sync.

**Dependencies:** P2-012, P2-022

**Complexity:** Low

**Definition of Done:**
- [ ] On first install, 5 payment methods exist in Room
- [ ] On second launch, seeding does not run again (idempotent guard)
- [ ] Payment methods appear on the payment screen without any setup by the owner

---

### ✅ Phase 2 Gate

Before proceeding to Phase 3, verify:
- [ ] `./gradlew assembleDebug` succeeds with all entities and DAOs
- [ ] Room database creates on device with all tables visible via Database Inspector
- [ ] Stock tables exist in the database even with no use cases
- [ ] SyncWorker can be manually triggered and pushes PENDING records to RTDB
- [ ] RTDB listener receives changes from the console and writes them to Room
- [ ] Firebase Auth login works on device

---

## Phase 3: Domain Layer

> Goal: Pure Kotlin business logic — domain models, repository interfaces, all use cases, mappers, and repository implementations. No Compose, no Android imports (except for the repository implementations which may reference Context for WorkManager). After this phase, any business rule can be unit-tested without an emulator.

---

### P3-001 · Rupiah Value Class and Domain Enums `[MVP]`

**Description:** Create `domain/model/Rupiah.kt` — `@JvmInline value class Rupiah(val value: Long)`. Add operators: `operator fun plus(other: Rupiah): Rupiah`, `operator fun minus(other: Rupiah): Rupiah`. Companion: `val ZERO = Rupiah(0)`. Create all domain enums if not already in core: `BillType`, `BillStatus`, `OrderItemStatus`, `ShiftStatus`, `UserRole`, `VoidReason`, `ExpenseCategory`, `VariantSelectionType`, `VarianceReason`, `SyncStatus`.

**Dependencies:** P1-001

**Complexity:** Low

**Definition of Done:**
- [ ] `Rupiah(15000L) + Rupiah(5000L) == Rupiah(20000L)`
- [ ] `Rupiah.ZERO` is `Rupiah(0L)`
- [ ] `Rupiah` is NOT used in Room entities (stored as plain `Long`) — verify no entity imports `Rupiah`
- [ ] All enums created in `domain/model/`

---

### P3-002 · Domain Models `[MVP]`

**Description:** Create pure Kotlin data classes for all domain models in `domain/model/`. Each is a clean representation with no Room or Firebase annotations. Models include:
- `MenuItem` (with `VariantGroup` and `VariantOption` lists nested)
- `MenuCategory`
- `CartItem` (in-memory only: `menuItem`, `selectedVariants: List<SelectedVariant>`, `quantity: Int`, `lineTotal: Rupiah`)
- `SelectedVariant` (variantGroupId, variantGroupName, selectedOptionId, selectedOptionName, priceDelta: Rupiah)
- `Bill` (with `orderItems: List<OrderItem>` and `payments: List<Payment>`)
- `OrderItem`
- `Payment`
- `PaymentMethod`
- `Table` (with `openBillCount: Int` and `totalOwed: Rupiah` as computed fields)
- `Shift`
- `ZReport` (includes all Z-report fields as typed fields, NOT just a JSON string — the JSON string is only in the Room entity)
- `Expense`
- `StockItem`
- `StockBatch`
- `StockOpname`
- `StockOpnameLine`
- `DailyDashboard` (report aggregate model)
- `DateRangeReport`
- `BestSellerItem`
- Custom exception classes: `ShiftNotOpenException`, `EmptyCartException`, `BillAlreadyPaidException`, `InsufficientPermissionsException`, `OpenBillsBlockShiftCloseException(val openBills: List<Bill>)`, `MissingRequiredVariantException`, `InsufficientTenderedAmountException`, `BillNotVoidableException`

**Dependencies:** P3-001

**Complexity:** Medium — many models, must match entity fields exactly for mappers

**Definition of Done:**
- [ ] All domain models are pure Kotlin data classes (zero Android/Room/Firebase imports)
- [ ] `ZReport` has typed fields (not just `snapshotJson`) — the mapper will serialize/deserialize from the entity
- [ ] `CartItem` and `SelectedVariant` have no persistence — they exist only in `domain/model/`
- [ ] All exception classes extend `Exception` and have meaningful messages
- [ ] All domain models use `Rupiah` for monetary fields, not raw `Long`

---

### P3-003 · Repository Interfaces `[MVP]`

**Description:** Create all repository interfaces in `domain/repository/`. Each interface defines only what the domain layer needs — no Room, no Firebase types. All return types use domain models and Flow/suspend:

Key interface signatures:
- `MenuRepository`: `fun observeMenuWithVariants(): Flow<List<MenuItem>>`, `suspend fun upsert(item: MenuItem)`, `suspend fun softDelete(id: String)`, `suspend fun toggleSoldOut(id: String, isSoldOut: Boolean)`, `suspend fun resetAllSoldOut()`
- `BillRepository`: `fun observeOpenBills(): Flow<List<Bill>>`, `suspend fun createBill(bill: Bill, items: List<OrderItem>)`, `fun observeBillDetail(id: String): Flow<Bill>`
- `OrderRepository`: `suspend fun confirmOrder(bill: Bill, items: List<OrderItem>)`, `suspend fun addItemsToExistingBill(billId: String, items: List<OrderItem>)`
- `PaymentRepository`: `suspend fun processPayment(billId: String, payments: List<Payment>)`, `fun observeForBill(billId: String): Flow<List<Payment>>`
- `ShiftRepository`: `fun observeOpenShift(): Flow<Shift?>`, `suspend fun getOpenShift(): Shift?`, `suspend fun openShift(shift: Shift)`, `suspend fun closeShift(shiftId: String, countedCash: Rupiah)`, `suspend fun countOpenBills(): Int`
- `ExpenseRepository`: `suspend fun logExpense(expense: Expense)`, `fun observeForToday(): Flow<List<Expense>>`
- `ReportRepository`: `suspend fun getDailyDashboard(dateEpoch: Long): DailyDashboard`, `suspend fun getDateRangeReport(start: Long, end: Long): DateRangeReport`, `fun observeBestSellers(start: Long, end: Long): Flow<List<BestSellerItem>>`
- `StockRepository`: `fun observeAll(): Flow<List<StockItem>>`, `suspend fun recordBatch(batch: StockBatch)`, `suspend fun deductForPayment(billId: String)` (P2)

**Dependencies:** P3-002

**Complexity:** Medium

**Definition of Done:**
- [ ] All interfaces use only domain model types (no Room entities, no Firebase types)
- [ ] All interfaces are in `domain/repository/` — no implementations here
- [ ] All methods that observe data return `Flow<*>`
- [ ] All methods that write data are `suspend`
- [ ] `RepositoryModule.kt` (created in P2-022) can now be populated with `@Binds` for each interface

---

### P3-004 · Mappers `[MVP]`

**Description:** Create mappers in `data/mapper/` that convert Room entities ↔ domain models. Each mapper is a Kotlin object or class with extension functions:

- `MenuMapper.kt` — `MenuItemEntity → MenuItem`, `MenuItem → MenuItemEntity`. Also assembles `MenuItem` with its `VariantGroup` and `VariantOption` lists from multiple entities.
- `BillMapper.kt` — `BillEntity → Bill`. The `Bill` domain model includes `orderItems` and `payments` — the mapper must accept these as parameters (not fetch them).
- `OrderItemMapper.kt` — `OrderItemEntity ↔ OrderItem`. Must deserialize `selectedVariantsJson` using `kotlinx.serialization`.
- `PaymentMapper.kt` — `PaymentEntity ↔ Payment`. Maps `Rupiah` ↔ `Long`.
- `ShiftMapper.kt` — `ShiftEntity ↔ Shift`.
- `ExpenseMapper.kt` — `ExpenseEntity ↔ Expense`.
- `ZReportMapper.kt` — `ZReportEntity ↔ ZReport`. Must serialize/deserialize the full `ZReport` domain object to/from `snapshotJson` using `kotlinx.serialization`. `ZReport` must be `@Serializable`.

All `Rupiah` fields: entity `Long` ↔ domain `Rupiah(value)`.

**Dependencies:** P3-002, P2-003 through P2-008

**Complexity:** High — mappers touch every field; mistakes cause silent data corruption

**Definition of Done:**
- [ ] Every entity field is mapped — no fields silently dropped
- [ ] `Rupiah` fields correctly unwrap to `Long` for entities and wrap to `Rupiah` for domain models
- [ ] `OrderItemMapper` correctly serializes/deserializes `selectedVariantsJson` (test with a variant-heavy item)
- [ ] `ZReportMapper` round-trips correctly: `ZReport → json → ZReport` preserves all fields
- [ ] Mappers are unit tested for every entity type (see Phase 5)

---

### P3-005 · Repository Implementations `[MVP]`

**Description:** Create all repository implementations in `data/repository/`. Each implementation:
1. Injects the relevant DAOs and `SyncCoordinator`
2. Maps entities to domain models using the mappers
3. Writes to Room first (optimistic, sets `syncStatus = PENDING`)
4. Calls `SyncCoordinator.notifyPendingSync()` after any write

Key implementations to create: `BillRepositoryImpl`, `MenuRepositoryImpl`, `OrderRepositoryImpl`, `PaymentRepositoryImpl`, `ShiftRepositoryImpl`, `ExpenseRepositoryImpl`, `ReportRepositoryImpl`, `StockRepositoryImpl` (stub — just compiles).

Important implementation detail for `PaymentRepositoryImpl.processPayment()`: must use `@Transaction` to atomically insert payments and update bill status in a single Room transaction.

**Dependencies:** P3-003, P3-004, P2-021

**Complexity:** High — most complex implementation phase, touches every DAO

**Definition of Done:**
- [ ] `RepositoryModule.kt` fully populated with all `@Binds` bindings
- [ ] `PaymentRepositoryImpl.processPayment()` uses `@Transaction` — verified by testing that a failure mid-payment does not leave the bill in a partially-paid state
- [ ] All `observeX()` methods correctly map `Flow<List<Entity>>` → `Flow<List<DomainModel>>` using `.map { list -> list.map { mapper.toDomain(it) } }`
- [ ] `SyncCoordinator.notifyPendingSync()` is called after every write operation
- [ ] No repository method touches RTDB directly — all RTDB interaction goes through `SyncCoordinator`

---

### P3-006 · Auth Use Cases `[MVP]`

**Description:**
- `LoginUseCase` — `suspend fun invoke(email: String, password: String): Result<UserRole>`. Calls `FirebaseAuthDataSource.signIn()`, then reads role from claim, updates `SessionManager`.
- `GetCurrentUserUseCase` — `fun invoke(): Flow<FirebaseUser?>`. Delegates to `FirebaseAuthDataSource.observeAuthState()`.

**Dependencies:** P3-003, P2-016

**Complexity:** Low

**Definition of Done:**
- [ ] `LoginUseCase` returns `Result.failure(AuthException)` on wrong credentials without crashing
- [ ] `GetCurrentUserUseCase` emits null on logout
- [ ] Use cases are pure — no Android context required

---

### P3-007 · Order Use Cases `[MVP]`

**Description:**
- `GetMenuItemsUseCase` — `fun invoke(): Flow<List<MenuItem>>`. Delegates to `MenuRepository.observeMenuWithVariants()`. Groups items by category.
- `ConfirmOrderUseCase` — `suspend fun invoke(cartItems: List<CartItem>, destination: OrderDestination, operatorId: String): Result<Unit>`. Validates: shift is open, cart non-empty, required variants fulfilled for each cart item, destination bill is OPEN (if adding to existing). Builds `Bill` and `OrderItem` domain objects with UUIDs, price/name snapshots, `sessionLabel`. Calls `OrderRepository.confirmOrder()`.
- `AddItemsToExistingBillUseCase` — `suspend fun invoke(billId: String, cartItems: List<CartItem>, operatorId: String): Result<Unit>`.

`OrderDestination` is a sealed class: `GrabAndGo`, `NewTableBill(tableId: String, billType: BillType)`, `ExistingBill(billId: String)`.

**Dependencies:** P3-003, P3-002, P3-001

**Complexity:** High — `ConfirmOrderUseCase` is the most business-logic-dense use case

**Definition of Done:**
- [ ] `ConfirmOrderUseCase` throws `ShiftNotOpenException` when no shift is open
- [ ] `ConfirmOrderUseCase` throws `EmptyCartException` on empty cart
- [ ] `ConfirmOrderUseCase` throws `MissingRequiredVariantException` when a required `VariantGroup` has no selection
- [ ] `ConfirmOrderUseCase` assigns `sessionLabel = "Bill #N"` correctly (counter tracked in the use case or passed in)
- [ ] Price snapshot correctly captures `basePrice + sum(selectedOption.priceDelta)` at confirm time
- [ ] All 3 use cases are unit tested (see Phase 5)

---

### P3-008 · Bill Use Cases `[MVP]`

**Description:**
- `GetOpenBillsUseCase` — `fun invoke(): Flow<List<Bill>>`. Includes each bill's order items and running total.
- `GetBillDetailUseCase` — `fun invoke(billId: String): Flow<Bill>`. Full bill with items and payments.
- `VoidBillUseCase` — `suspend fun invoke(billId: String, reason: VoidReason, operatorId: String, userRole: UserRole): Result<Unit>`. Enforces owner-only: throws `InsufficientPermissionsException` if `userRole != OWNER`. Validates bill is not already VOID.
- `VoidOrderItemUseCase` — `suspend fun invoke(orderItemId: String, reason: VoidReason, note: String?, operatorId: String): Result<Unit>`. If `reason == OTHER` and `note` is blank → throws `IllegalArgumentException`. Recalculates bill subtotal after void.

**Dependencies:** P3-003, P3-002

**Complexity:** Medium

**Definition of Done:**
- [ ] `VoidBillUseCase` rejects STAFF role with `InsufficientPermissionsException`
- [ ] `VoidOrderItemUseCase` requires non-blank `note` when `reason == OTHER`
- [ ] Bill total correctly updates after item void (voided items excluded from sum)
- [ ] Unit tested for role guard, reason guard, double-void prevention

---

### P3-009 · Payment Use Cases `[MVP]`

**Description:**
- `CalculateChangeUseCase` — `fun invoke(tendered: Rupiah, total: Rupiah): Rupiah`. Pure function, no repo. `change = tendered - total`. Returns `Rupiah.ZERO` if tendered == total. Negative result means insufficient (caller handles this).
- `ProcessPaymentUseCase` — `suspend fun invoke(billId: String, payments: List<Payment>): Result<Unit>`. Validates: bill is OPEN, sum of payments == bill.grandTotal, cash tendered >= cash amount for any cash payment rows, active shift exists. On success: atomically inserts payments and sets bill to PAID in Room.

**Dependencies:** P3-003, P3-002, P3-001

**Complexity:** High — payment is the most critical financial write operation

**Definition of Done:**
- [ ] `ProcessPaymentUseCase` uses a Room `@Transaction` (verified via `PaymentRepositoryImpl`)
- [ ] Partial payment (sum < grandTotal) returns `Result.failure(InsufficientPaymentException)`
- [ ] Insufficient cash tendered returns `Result.failure(InsufficientTenderedAmountException)`
- [ ] Bill PAID only when ALL payment rows sum to grandTotal
- [ ] Unit tested with: exact cash, overpaid cash, split payment, insufficient payment, already-paid bill

---

### P3-010 · Shift Use Cases `[MVP]`

**Description:**
- `OpenShiftUseCase` — `suspend fun invoke(operatorName: String, openingFloat: Rupiah): Result<Unit>`. Checks no shift is currently open (via `ShiftRepository.getOpenShift()`). Creates shift record. If any `isSoldOut` items exist, returns `Result.success(SoldOutItemsExist)` so UI can prompt the reset question (use a sealed Result type).
- `CheckSoldOutItemsUseCase` — `suspend fun invoke(): Boolean`. Returns true if any menu item has `isSoldOut = true`.
- `ResetSoldOutItemsUseCase` — `suspend fun invoke(): Result<Unit>`. Calls `MenuRepository.resetAllSoldOut()`.
- `CloseShiftUseCase` — `suspend fun invoke(shiftId: String, countedCash: Rupiah, operatorId: String, userRole: UserRole): Result<Unit>`. Guards: `userRole == OWNER`, `countOpenBills() == 0` (throws `OpenBillsBlockShiftCloseException`). Calculates `expectedCash`, `variance`. Calls `GenerateZReportUseCase`, then closes the shift.
- `GenerateZReportUseCase` — `suspend fun invoke(shiftId: String): ZReport`. Aggregates all shift data into a `ZReport` domain object.

**Dependencies:** P3-003, P3-002

**Complexity:** High — `CloseShiftUseCase` and `GenerateZReportUseCase` are complex aggregations

**Definition of Done:**
- [ ] `OpenShiftUseCase` fails if a shift is already open
- [ ] `CloseShiftUseCase` fails for STAFF role
- [ ] `CloseShiftUseCase` fails with `OpenBillsBlockShiftCloseException` when open bills exist
- [ ] `GenerateZReportUseCase` correctly aggregates: gross sales, payment breakdown, void summary, expenses, cash reconciliation
- [ ] Cash variance formula: `expectedCash = openingFloat + Σ(cash payments) - Σ(cash expenses)`, `variance = countedCash - expectedCash`
- [ ] Unit tested for all failure paths

---

### P3-011 · Menu and Expense Use Cases `[MVP]`

**Description:**
- `UpsertMenuItemUseCase` — `suspend fun invoke(item: MenuItem, userRole: UserRole): Result<Unit>`. Owner-only guard.
- `DeleteMenuItemUseCase` — `suspend fun invoke(id: String, userRole: UserRole): Result<Unit>`. Owner-only. Checks if item appears in any OPEN bill before soft-deleting.
- `ToggleSoldOutUseCase` — `suspend fun invoke(id: String, isSoldOut: Boolean)`. Any role allowed.
- `LogExpenseUseCase` — `suspend fun invoke(expense: Expense): Result<Unit>`. Any role. Validates: `amount > 0`, `date` is today (warn but don't block if backdated).

**Dependencies:** P3-003, P3-002

**Complexity:** Medium

**Definition of Done:**
- [ ] `UpsertMenuItemUseCase` rejects STAFF role
- [ ] `DeleteMenuItemUseCase` does NOT hard-delete — sets `isAvailable = false`
- [ ] `DeleteMenuItemUseCase` warns (but does not block) if item appears in OPEN bills
- [ ] `LogExpenseUseCase` rejects zero or negative amounts
- [ ] All unit tested

---

### P3-012 · Report Use Cases `[MVP + P2]`

**Description:**
- `GetDailyDashboardUseCase` — `suspend fun invoke(dateEpoch: Long): DailyDashboard`. Owner-only usage (enforced in ViewModel, not here). Aggregates today's sales, transaction count, top 5 items, payment method breakdown, cash variance. `[MVP]`
- `GetDateRangeReportUseCase` — `suspend fun invoke(start: Long, end: Long): DateRangeReport`. `[P2]`
- `GetBestSellersUseCase` — `fun invoke(start: Long, end: Long): Flow<List<BestSellerItem>>`. `[P2]`
- `ExportReportUseCase` — `suspend fun invoke(report: DateRangeReport, format: ExportFormat): ByteArray`. Formats data as CSV or PDF bytes. `[P2]`

**Dependencies:** P3-003, P3-002

**Complexity:** Medium (dashboard) · High (export)

**Definition of Done:**
- [ ] `GetDailyDashboardUseCase` returns correct data for a known set of bills (testable with in-memory Room)
- [ ] `ExportReportUseCase` CSV output is valid (parseable by Excel/Sheets)
- [ ] `ExportReportUseCase` PDF output opens without error (can defer full PDF layout to P2)
- [ ] `[MVP]` Use cases tagged MVP must work before first production use

---

### ✅ Phase 3 Gate

Before proceeding to Phase 4, verify:
- [ ] All domain use cases compile with zero Android imports (except `CoroutineDispatcher` if needed)
- [ ] `RepositoryModule.kt` has a `@Binds` for every repository interface
- [ ] All mappers round-trip correctly (entity → domain → entity produces equal entity)
- [ ] `ConfirmOrderUseCase`, `ProcessPaymentUseCase`, and `CloseShiftUseCase` are unit tested with all failure paths covered

---

## Phase 4: UI Layer

> Goal: All screens built and wired to ViewModels and use cases. The app is fully operable by a real user on a real device.

**Build order within this phase** (respect dependencies):
1. Auth → Shift Open (nothing works without these)
2. Order screen (launch destination, most used)
3. Tables/Bills + Bill Detail
4. Payment screen
5. Void flows (dialogs, part of Bill Detail and Order screens)
6. Shift Close + Z-Report
7. Menu Management
8. Expense
9. Daily Dashboard
10. Settings
11. Version Gate screen
12. Sync Status Bar (can be done any time after P2-021)

---

### P4-001 · Login Screen `[MVP]`

**Description:** `feature/auth/LoginScreen.kt`. Email + password fields, login button, loading indicator. `LoginViewModel` calls `LoginUseCase`, on success navigates to `OrderScreen`. On failure shows Snackbar with localized error. Must work offline (auth token cached) — app should not force re-login on every launch if already authenticated.

**Dependencies:** P3-006, P1-011

**Complexity:** Low

**Definition of Done:**
- [ ] Correct credentials → navigates to Order screen
- [ ] Wrong credentials → Snackbar with "Login gagal. Periksa email dan kata sandi." / "Login failed."
- [ ] Loading state shows `CircularProgressIndicator`, disables button
- [ ] If already authenticated, auto-navigate past login screen on app start
- [ ] Both EN and ID strings present

---

### P4-002 · Shift Open Screen `[MVP]`

**Description:** `feature/shift/ShiftOpenScreen.kt`. Shown when no shift is OPEN and user tries to access the Order screen. Fields: operator name (pre-filled from SessionManager), opening cash float (numeric input). On submit: calls `OpenShiftUseCase`. If `CheckSoldOutItemsUseCase` returns true, show a dialog: "Ada [N] menu yang sold out. Reset semua?" / "Reset all sold-out items?" with Yes/No. If Yes, calls `ResetSoldOutItemsUseCase` then opens shift. On success, navigates to Order screen.

**Dependencies:** P3-010, P4-001

**Complexity:** Medium

**Definition of Done:**
- [ ] Operator name pre-filled
- [ ] Opening float field: numeric only, Rp formatted hint
- [ ] Sold-out prompt appears when applicable; reset works
- [ ] Cannot submit with empty opening float
- [ ] After successful open, Order screen is accessible
- [ ] Both languages present

---

### P4-003 · Order Screen — Core `[MVP]`

**Description:** `feature/order/OrderScreen.kt` — the most-used screen in the app, launch destination. Layout: Category chip strip (top), MenuItem grid (scrollable), Cart panel (bottom, persistent). `OrderViewModel` holds `_cart: MutableStateFlow<List<CartItem>>` in memory.

Grid item card: name, price (Rp formatted), sold-out overlay if `isSoldOut = true`. Tap on available item: if no variants → add 1 to cart. If has variants → open `VariantSelectionSheet`.

Cart panel: shows cart items with +/- buttons, line totals, subtotal. "Confirm Order" button enabled only when cart is non-empty AND open shift exists.

**Dependencies:** P3-007, P4-002, P1-009

**Complexity:** Critical — most complex screen, most used

**Definition of Done:**
- [ ] Item grid renders all available items grouped by category
- [ ] Category chip filters items correctly
- [ ] Sold-out items are greyed, non-tappable (but visible with "Habis" / "Sold Out" badge)
- [ ] Tapping available item adds 1 to cart immediately (optimistic, no loading)
- [ ] +/- buttons in cart work; `-` at qty 1 removes item
- [ ] Running subtotal is always correct (including variant price deltas)
- [ ] "Confirm Order" is disabled when cart is empty or shift is not open
- [ ] No network required for this screen to function

---

### P4-004 · Variant Selection Bottom Sheet `[MVP]`

**Description:** `feature/order/component/VariantSelectionSheet.kt`. Bottom sheet shown when tapping an item with VariantGroups. Shows each group with its options (Radio buttons for SINGLE, Checkboxes for MULTIPLE). Required groups are starred. "Add to Cart" button disabled until all required groups have a selection. Shows running price delta as selections change.

**Dependencies:** P4-003

**Complexity:** Medium

**Definition of Done:**
- [ ] SINGLE selection groups use Radio buttons — only one option selectable
- [ ] MULTIPLE selection groups use Checkboxes
- [ ] Required groups validated before "Add" is enabled
- [ ] Price shown updates in real time: "Nasi Goreng — Rp 15.000 (+Rp 3.000 Pedas)" 
- [ ] Bottom sheet dismissable via back gesture or drag down
- [ ] Cart receives the correctly priced item with variant snapshot

---

### P4-005 · Order Destination Bottom Sheet `[MVP]`

**Description:** `feature/order/component/OrderDestinationSheet.kt`. Bottom sheet shown after "Confirm Order" is tapped. Three options:
1. **Bawa Pulang / Grab & Go** — goes straight to `PaymentScreen`
2. **Meja Baru / New Table** — shows table selector + UPFRONT/OPEN_BILL toggle → confirms order
3. **Tambah ke Tagihan / Add to Existing Bill** — shows list of currently open bills grouped by table → confirms order

**Dependencies:** P4-003, P3-007

**Complexity:** Medium

**Definition of Done:**
- [ ] Grab-and-go path navigates to payment immediately
- [ ] New table bill: table must be selected, bill type must be chosen — confirm disabled until both are set
- [ ] Add to existing: shows only OPEN bills, with table label and total
- [ ] After non-grab-and-go confirm: cart clears, Snackbar "Pesanan dikirim" / "Order sent", stays on Order screen

---

### P4-006 · Tables/Bills Overview Screen `[MVP]`

**Description:** `feature/tables/TablesScreen.kt`. Shows all active tables with open bill count and total owed. Tables with no open bills shown with a muted card. Grab-and-go open bills (tableId = null) shown in a dedicated "Bawa Pulang" section. Tapping a table card expands to show its open bills. Tapping a bill navigates to `BillDetailScreen`.

`TablesViewModel` observes `GetOpenBillsUseCase` and groups bills by table.

**Dependencies:** P3-008, P1-011

**Complexity:** Medium

**Definition of Done:**
- [ ] Bills grouped by table correctly
- [ ] Grab-and-go bills shown separately
- [ ] Total owed per table is sum of `grandTotal` of all OPEN bills on that table
- [ ] Bills open > 12 hours show an amber warning indicator
- [ ] Screen updates in real time as new bills are created on Device 2

---

### P4-007 · Bill Detail Screen `[MVP]`

**Description:** `feature/tables/BillDetailScreen.kt`. Shows full bill: table label, bill type, session label, order items list (including voided items struck through), payments received (for partial payments), running total, "Bayar / Pay" button.

For OPEN bills: shows "Tambah Item / Add Items" button (navigates back to Order screen with this bill pre-selected as destination). Shows void item button (long press or swipe-to-reveal on each item).

**Dependencies:** P4-006, P3-008

**Complexity:** Medium

**Definition of Done:**
- [ ] All order items shown, voided items struck through and excluded from total
- [ ] "Bayar" button navigates to PaymentScreen with `billId`
- [ ] "Tambah Item" navigates to OrderScreen with the bill pre-selected as destination
- [ ] Void item dialog accessible per item
- [ ] Bill total recalculates correctly after item void

---

### P4-008 · Void Order Item Dialog `[MVP]`

**Description:** Dialog triggered from `BillDetailScreen` when voiding an order item. Shows: item name, reason picker (RadioGroup: Customer Change, Kitchen Error, Item Unavailable, Test, Other), text field shown only when "Other" is selected. "Batalkan Item / Void Item" button disabled until reason selected (and note filled if Other). Calls `VoidOrderItemUseCase`.

**Dependencies:** P4-007, P3-008

**Complexity:** Low

**Definition of Done:**
- [ ] All 5 void reasons available in both languages
- [ ] Note field shown only when "Other" selected
- [ ] "Void" button disabled until valid state
- [ ] After void: dialog closes, item shows struck-through in bill, total updates

---

### P4-009 · Void Entire Bill Dialog `[MVP]`

**Description:** Owner-only dialog triggered from `BillDetailScreen`. Similar to item void but for the whole bill. Uses `VoidBillUseCase`. If current user is STAFF, the option to void the entire bill is not shown in the UI (role guard in ViewModel).

**Dependencies:** P4-007, P3-008

**Complexity:** Low

**Definition of Done:**
- [ ] "Void Bill" option hidden for STAFF role
- [ ] Reason picker required (same reasons as item void)
- [ ] After void: bill disappears from open bills list, appears in shift history with VOID status

---

### P4-010 · Payment Screen `[MVP]`

**Description:** `feature/payment/PaymentScreen.kt`. Shows bill summary (items, total). Payment method selector (horizontal scrollable chip row — only enabled methods shown). For cash: tendered amount input, change display (auto-calculated). For non-cash: mark as received button. Split payment: can add multiple payment rows until `remainingBalance == 0`. "Bayar / Pay" button enabled only when `remainingBalance == 0`.

`PaymentViewModel` holds in-memory payment rows, remaining balance calculation, and calls `ProcessPaymentUseCase`.

**Dependencies:** P3-009, P4-007

**Complexity:** High — split payment state management is complex

**Definition of Done:**
- [ ] Only enabled payment methods shown
- [ ] Cash: change = tendered - grandTotal, shown instantly as typing
- [ ] Cash: cannot complete if tendered < remaining balance for that row
- [ ] Split payment: adding a row reduces remaining balance; remaining balance shown prominently
- [ ] "Pay" disabled until remaining balance == 0
- [ ] After payment: navigate to success/confirmation screen → then to Tables (or Order for grab-and-go)
- [ ] Paid bill disappears from open bills list

---

### P4-011 · Shift Close Screen `[MVP]`

**Description:** `feature/shift/ShiftCloseScreen.kt`. Owner-only (role-gated at nav level). Shows: current shift summary (opened at, opened by, hours open). Pre-condition check: if any bills are OPEN, shows a blocking list "Tutup tagihan berikut sebelum menutup shift / Close these bills before closing the shift" with each open bill listed (table + session label + total). When no open bills: shows counted cash input, expected cash (computed), variance display (green if 0, amber if small, red if large). "Tutup Shift / Close Shift" button calls `CloseShiftUseCase`, then navigates to Z-Report screen.

**Dependencies:** P3-010, P1-011

**Complexity:** High — complex pre-condition flow

**Definition of Done:**
- [ ] Blocked state shows exactly which bills are open with enough detail to find them
- [ ] Cash variance displayed with color coding: zero = green, ≤ Rp 5.000 = amber, > Rp 5.000 = red
- [ ] "Close Shift" disabled when open bills exist
- [ ] After close: navigates to Z-Report, shift is no longer accessible for adding orders

---

### P4-012 · Z-Report Screen `[MVP]`

**Description:** `feature/shift/ZReportScreen.kt`. Read-only screen. Shows: date, shift duration, gross sales, payment method breakdown, void summary (count + total + by reason), expenses by category, opening float, expected cash, counted cash, cash variance. Also accessible from Shift History for past shifts. Immutable — no edit capability.

**Dependencies:** P3-010, P4-011

**Complexity:** Medium

**Definition of Done:**
- [ ] All Z-report fields displayed correctly
- [ ] Cash variance shown with same color coding as shift close screen
- [ ] Historical Z-reports accessible from Shift History
- [ ] Tapping "Share" (Phase 2) is stubbed for now

---

### P4-013 · Expense Log Screen `[MVP]`

**Description:** `feature/expense/ExpenseLogScreen.kt`. Accessible to all roles. FAB to add new expense. Form: category picker (spinner/dropdown), amount field (numeric), date picker (defaults to today), optional note. List of today's expenses below. `ExpenseViewModel` calls `LogExpenseUseCase`.

**Dependencies:** P3-011, P1-011

**Complexity:** Low

**Definition of Done:**
- [ ] All default expense categories available in both languages
- [ ] Amount field: numeric only, Rp prefix
- [ ] Cannot submit with zero amount
- [ ] Expense appears in list immediately after submission
- [ ] Expenses appear in today's dashboard total

---

### P4-014 · Menu Management Screen `[MVP]`

**Description:** `feature/menu/MenuManagementScreen.kt`. Owner-only. Shows all categories with their items. FAB to add new category. Each item shows name, price, availability/sold-out status. Tap item → `MenuItemEditScreen`. Sold-out toggle accessible directly from this list. "Hide" (soft delete) option per item.

**Dependencies:** P3-011, P1-011

**Complexity:** Medium

**Definition of Done:**
- [ ] Owner can create, rename, reorder categories
- [ ] Owner can create new items with name, price, category
- [ ] Sold-out toggle works inline (no navigation required)
- [ ] "Hide" soft-deletes item (sets isAvailable = false), item disappears from order screen immediately
- [ ] Items blocked from hide if in OPEN bills (show warning dialog)

---

### P4-015 · Menu Item Edit Screen + Variant Management `[MVP]`

**Description:** `feature/menu/MenuItemEditScreen.kt`. Edit name, price, category. Below: section for VariantGroups. Each group shows its options. Buttons: add group, add option to group, delete option, delete group. Group settings: name, type (SINGLE/MULTIPLE), required toggle. Option settings: name, price delta (can be +/0/-).

**Dependencies:** P4-014, P3-011

**Complexity:** High — nested editable lists (groups → options) with their own state

**Definition of Done:**
- [ ] Can add/edit/delete VariantGroups
- [ ] Can add/edit/delete VariantOptions per group
- [ ] Required toggle works — affects order screen variant selection validation
- [ ] Price delta shown as "+Rp 2.000" or "-Rp 1.000" or "Gratis / Free"
- [ ] Save correctly upserts all entities (menu item + groups + options in correct order)

---

### P4-016 · Daily Dashboard Screen `[MVP]`

**Description:** `feature/reports/DashboardScreen.kt`. Owner-only. Shows today's data: gross sales (large number), transaction count, payment breakdown (bar or list), top 5 items by quantity (simple ranked list), cash variance. Auto-refreshes via `Flow` observation. No date picker — always today.

**Dependencies:** P3-012, P1-011

**Complexity:** Medium

**Definition of Done:**
- [ ] Data is always for the current calendar day in WIB timezone
- [ ] Updates in real time as new sales happen (Flow-backed)
- [ ] All monetary values formatted as Rp
- [ ] Shows "Belum ada penjualan hari ini / No sales today" when empty
- [ ] Inaccessible to STAFF role (route guard)

---

### P4-017 · Settings Screens `[MVP]`

**Description:** A group of screens under the "More" nav item:

`SettingsScreen.kt` — root settings hub with list of options.
`TableSettingsScreen.kt` — CRUD for tables (add, rename, deactivate).
`PaymentMethodSettingsScreen.kt` — list of payment methods with toggle, rename, drag-to-reorder.
`ExpenseCategorySettingsScreen.kt` — edit default expense categories.
`LanguageSettingsScreen.kt` — toggle between EN and ID; applies immediately via `AppPreferences` + `LocalConfiguration` override in the app composable.
`AboutScreen.kt` — app version, build number.

**Dependencies:** P1-008, P1-010, P1-011

**Complexity:** Medium — drag-to-reorder for payment methods is the hard part

**Definition of Done:**
- [ ] Table CRUD works: add, rename, deactivate. Deactivated tables don't appear in order flow.
- [ ] Payment method toggle works; enabled methods appear on payment screen within the same session
- [ ] Language switch applies without app restart (test by switching mid-session)
- [ ] Drag-to-reorder updates `sortOrder` field in Room and RTDB

---

### P4-018 · Sync Status Bar `[MVP]`

**Description:** A persistent, non-blocking status bar composable injected into the app's top-level `Scaffold` (above content). Observes `SyncCoordinator.syncState` and `SyncCoordinator.pendingCount`. Renders:
- Hidden when `syncState = IDLE` and online
- Amber `"Menyinkronkan N data... / Syncing N items..."` when `syncState = SYNCING`
- Red `"Offline — data tersimpan lokal / Offline — data saved locally"` when offline
- Gray `"N data tertunda / N items pending"` when online but pending

**Dependencies:** P2-021, P1-011

**Complexity:** Low

**Definition of Done:**
- [ ] Status bar is visible on all screens (lives in top-level Scaffold)
- [ ] Transitions correctly between states
- [ ] Does NOT overlap with top app bar or content
- [ ] Correct strings in both languages

---

### P4-019 · Version Gate Screen `[MVP]`

**Description:** `feature/auth/UpdateRequiredScreen.kt`. Full-screen, non-dismissable. Shows: app icon, "Versi baru tersedia / Update available", current version, message "Silakan minta update APK ke pemilik toko / Please ask the store owner for the latest APK". No action button (manual update only). Shown before any other screen if version gate fires.

**Dependencies:** P1-013

**Complexity:** Low

**Definition of Done:**
- [ ] Screen is non-dismissable (back button does nothing)
- [ ] Correct message in both languages
- [ ] Triggered correctly when version code < minVersionCode (test manually)

---

### P4-020 · Stock Screens — Stub `[P2]`

**Description:** Create stub screens for `StockScreen.kt`, `StockBatchScreen.kt`, `OpnameScreen.kt`. Each shows a centered "Coming Soon" placeholder. They must compile and be reachable from the navigation graph. The `StockViewModel` stub can have an empty `init` block.

**Dependencies:** P1-011

**Complexity:** Low

**Definition of Done:**
- [ ] Screens compile and are reachable from the More nav menu
- [ ] No crash when tapping into these screens
- [ ] Clear visual indication these screens are not yet functional

---

### P4-021 · Full Reports Screen `[P2]`

**Description:** `feature/reports/ReportScreen.kt`. Date range picker (day/week/month/custom). Shows: revenue, expenses, gross profit, net, payment method pie/bar chart, void summary, expense breakdown. Share/export button calls `ExportReportUseCase`. `BestSellerScreen.kt` is a sub-screen with ranked item list.

**Dependencies:** P3-012, P4-016

**Complexity:** High

**Definition of Done:**
- [ ] Date range filter works correctly
- [ ] Export produces valid CSV shared via Android share sheet
- [ ] Best seller ranking matches expected data
- [ ] All monetary values Rp-formatted

---

### ✅ Phase 4 Gate

Before proceeding to Phase 5, run a **full service day simulation** on a real device:
- [ ] Log in as owner
- [ ] Open shift with Rp 100.000 float
- [ ] Take a grab-and-go order, pay cash, verify change
- [ ] Open a dine-in upfront bill on Table 1
- [ ] Add items to the same bill from a second device (if available)
- [ ] Open a dine-in open bill on Table 2, add items twice
- [ ] Void one item with reason
- [ ] Process payment for all bills
- [ ] Add an expense (Gas, Rp 30.000)
- [ ] Close shift, verify Z-report totals match expectations
- [ ] Confirm sync status bar shows correctly throughout

---

## Phase 5: Testing

> Goal: Confidence that the business-critical paths are correct and won't regress. Focus test effort on the domain layer (use cases) and data layer (DAOs). Do not write UI tests for every screen — only the 3 critical flows.

---

### P5-001 · Unit Tests — ConfirmOrderUseCase `[MVP]`

**Description:** Test all paths of the most business-critical use case.

**Dependencies:** P3-007

**Complexity:** Medium

**Definition of Done:**
- [ ] Test: valid cart + active shift + new table bill → `Result.success`
- [ ] Test: empty cart → `Result.failure(EmptyCartException)`
- [ ] Test: no active shift → `Result.failure(ShiftNotOpenException)`
- [ ] Test: required variant missing → `Result.failure(MissingRequiredVariantException)`
- [ ] Test: grab-and-go → `tableId = null`, `type = UPFRONT`
- [ ] Test: add to existing OPEN bill → items appended
- [ ] Test: add to PAID bill → `Result.failure(BillAlreadyPaidException)` or equivalent
- [ ] Tests run on JVM (no emulator required)

---

### P5-002 · Unit Tests — ProcessPaymentUseCase `[MVP]`

**Description:** Test all payment validation paths.

**Dependencies:** P3-009

**Complexity:** Medium

**Definition of Done:**
- [ ] Test: exact cash payment → `Result.success`, bill PAID
- [ ] Test: overpaid cash → correct change, `Result.success`
- [ ] Test: underpaid cash → `Result.failure`
- [ ] Test: split cash + QRIS summing to total → `Result.success`
- [ ] Test: split total less than grandTotal → `Result.failure`
- [ ] Test: bill already PAID → `Result.failure`
- [ ] Test: no active shift → `Result.failure`

---

### P5-003 · Unit Tests — CloseShiftUseCase `[MVP]`

**Description:** Test shift close business rules.

**Dependencies:** P3-010

**Complexity:** Medium

**Definition of Done:**
- [ ] Test: open bills exist → `Result.failure(OpenBillsBlockShiftCloseException)` with correct bill list
- [ ] Test: STAFF role → `Result.failure(InsufficientPermissionsException)`
- [ ] Test: no open bills + OWNER → `Result.success`, Z-report generated
- [ ] Test: cash variance calculation with known inputs → correct variance
- [ ] Test: expectedCash formula: `openingFloat + Σ(cash payments) - Σ(cash expenses) = expectedCash`

---

### P5-004 · Unit Tests — VoidBillUseCase and VoidOrderItemUseCase `[MVP]`

**Dependencies:** P3-008

**Complexity:** Low

**Definition of Done:**
- [ ] Test: STAFF role voids bill → `InsufficientPermissionsException`
- [ ] Test: OWNER voids OPEN bill → `Result.success`
- [ ] Test: void item with `OTHER` reason and empty note → `IllegalArgumentException`
- [ ] Test: void item with `OTHER` reason and valid note → `Result.success`
- [ ] Test: bill total recalculates after item void (voided item excluded)

---

### P5-005 · Unit Tests — CalculateChangeUseCase `[MVP]`

**Dependencies:** P3-009

**Complexity:** Low

**Definition of Done:**
- [ ] Test: tendered > total → correct positive change
- [ ] Test: tendered == total → `Rupiah.ZERO`
- [ ] Test: tendered < total → negative Rupiah (caller is responsible for UI treatment)
- [ ] Test: zero tendered → negative equal to total
- [ ] Pure function — no mocking required

---

### P5-006 · Unit Tests — ConflictResolver `[MVP]`

**Dependencies:** P2-018

**Complexity:** Low

**Definition of Done:**
- [ ] Test: incoming.updatedAt > existing → `ACCEPT`
- [ ] Test: incoming.updatedAt < existing → `REJECT`
- [ ] Test: incoming.updatedAt == existing → `REJECT` (local wins on tie)
- [ ] Test: incoming status = OPEN, existing status = PAID → `REJECT` (status regression)
- [ ] Test: no existing record (new entity) → `ACCEPT`

---

### P5-007 · Unit Tests — Mappers `[MVP]`

**Dependencies:** P3-004

**Complexity:** Low

**Definition of Done:**
- [ ] Each mapper has a round-trip test: `entity → domain → entity` produces equal entity (all fields preserved)
- [ ] `OrderItemMapper`: test that `selectedVariantsJson` serialization/deserialization is lossless
- [ ] `ZReportMapper`: test that a complex Z-report round-trips correctly through JSON
- [ ] `Rupiah` fields: verify `Long(15000) → Rupiah(15000) → Long(15000)` round-trip

---

### P5-008 · Integration Tests — BillDao `[MVP]`

**Description:** In-memory Room database tests for the most critical DAO.

**Dependencies:** P2-011, P2-009

**Complexity:** Medium

**Definition of Done:**
- [ ] Test: insert OPEN bill → `observeOpenBills()` emits it
- [ ] Test: update bill to PAID → `observeOpenBills()` no longer includes it
- [ ] Test: two OPEN bills on same tableId → `observeOpenBillsForTable()` returns both
- [ ] Test: `countOpenBills()` returns 0 when no open bills
- [ ] Test: Flow emits new value after each insert (tests Room invalidation)

---

### P5-009 · Integration Tests — ReportQueryDao `[MVP]`

**Description:** Verify aggregate SQL queries produce correct results with known test data.

**Dependencies:** P2-015

**Complexity:** Medium — requires seeding test data carefully

**Definition of Done:**
- [ ] Test: 3 PAID bills with known totals → `totalSalesForDateRange` returns correct sum
- [ ] Test: bills outside date range → excluded from sum
- [ ] Test: best seller query returns correct ranking with tied quantities handled
- [ ] Test: `salesByPaymentMethod` groups correctly across multiple payments
- [ ] Test: void order items excluded from best seller quantity count

---

### P5-010 · Integration Tests — PaymentDao Transaction `[MVP]`

**Description:** Verify that `processPayment` repository method is atomic.

**Dependencies:** P2-012, P3-005

**Complexity:** Medium

**Definition of Done:**
- [ ] Test: simulate failure mid-transaction → bill remains OPEN, no payment rows inserted
- [ ] Test: successful payment → bill is PAID and payments inserted atomically
- [ ] Test: double payment attempt → second attempt fails (bill already PAID)

---

### P5-011 · UI Tests — Grab-and-Go Order Flow `[MVP]`

**Description:** End-to-end Compose UI test: full grab-and-go order + cash payment.

**Dependencies:** P4-003, P4-005, P4-010

**Complexity:** High — requires Hilt test injection and in-memory Room

**Definition of Done:**
- [ ] Test navigates from Order screen to payment and back to Order
- [ ] Cart total on payment screen matches what was added in order screen
- [ ] Change calculation correct for overpaid cash
- [ ] Bill is marked PAID in Room after test completes
- [ ] Test is repeatable and not flaky

---

### P5-012 · UI Tests — Shift Close Blocked by Open Bill `[MVP]`

**Description:** End-to-end test: create an open bill → try to close shift → blocked → close bill → shift close succeeds.

**Dependencies:** P4-011, P4-007

**Complexity:** High

**Definition of Done:**
- [ ] Shift close screen shows the blocking bill
- [ ] After bill is paid, shift close is accessible
- [ ] Z-report is generated and displayed
- [ ] Test is repeatable

---

### ✅ Phase 5 Gate

Before proceeding to Phase 6:
- [ ] All use case unit tests pass (`./gradlew test`)
- [ ] All DAO integration tests pass (`./gradlew connectedAndroidTest`)
- [ ] Two critical UI tests pass on emulator
- [ ] Zero compilation warnings in test source sets

---

## Phase 6: Release Preparation

> Goal: APK is signed, tested on real device, and ready for private distribution.

---

### P6-001 · Signing Configuration `[MVP]`

**Description:** Configure release signing in `app/build.gradle.kts` using a keystore. Store keystore credentials in `local.properties` (not in version control). Set up `release` buildType with `signingConfig`. The keystore is generated once and stored securely by the owner.

**Dependencies:** P1-001

**Complexity:** Low

**Definition of Done:**
- [ ] `local.properties` contains keystore path, alias, passwords (not committed to git)
- [ ] `.gitignore` excludes `local.properties` and the keystore file
- [ ] `./gradlew assembleRelease` produces a signed APK

---

### P6-002 · Release Build Verification `[MVP]`

**Description:** Build a release APK with minification and R8 enabled. Install on a real Android device (not emulator). Run through the full service day simulation from the Phase 4 gate.

**Dependencies:** P1-012, P6-001

**Complexity:** Medium — R8 often reveals issues not visible in debug builds

**Definition of Done:**
- [ ] Release APK installs without error
- [ ] Firebase connects in release build
- [ ] Room queries return correct data in release build
- [ ] `kotlinx.serialization` works in release build (check `selectedVariantsJson` round-trip)
- [ ] No runtime crashes in any MVP flow
- [ ] APK size is reasonable (< 30 MB recommended)

---

### P6-003 · Firebase Auth Account Setup `[MVP]`

**Description:** Create two Firebase Auth accounts in the console: one owner account (owner's email) and one staff account. Set custom claims via Firebase console or a one-time Cloud Function / Admin SDK script: `{ "role": "owner" }` for the owner account, `{ "role": "staff" }` for the staff account.

**Dependencies:** P1-004

**Complexity:** Low — one-time manual task

**Definition of Done:**
- [ ] Both accounts exist in Firebase Auth console
- [ ] Owner account: custom claim `role = "owner"` verified (sign in, check `getIdTokenResult().claims`)
- [ ] Staff account: custom claim `role = "staff"` verified
- [ ] Staff account cannot access owner-only routes when logged in on device
- [ ] Custom claim setup procedure documented in `README.md`

---

### P6-004 · RTDB Security Rules Final Deployment `[MVP]`

**Description:** Deploy the final RTDB security rules from P2-023 to production. Verify rules are active. Test with both authenticated and unauthenticated requests.

**Dependencies:** P2-023, P6-003

**Complexity:** Low

**Definition of Done:**
- [ ] Rules deployed to Firebase project (not just saved as draft)
- [ ] Unauthenticated write returns 403 (test via Firebase REST API)
- [ ] Owner-authenticated read/write succeeds
- [ ] Bill status regression rule verified (PAID → OPEN rejected)

---

### P6-005 · Device Installation and Sync Test `[MVP]`

**Description:** Install the release APK on both devices (owner's phone + staff's phone). Log in as owner on Device 1, staff on Device 2. Create a bill on Device 1 and verify it appears on Device 2. Test offline mode: turn off internet on Device 2, create an order — verify it syncs when internet is restored.

**Dependencies:** P6-002, P6-003

**Complexity:** Medium

**Definition of Done:**
- [ ] Bill created on Device 1 appears on Device 2 within 5 seconds (online)
- [ ] Order created offline on Device 2 syncs correctly when internet is restored
- [ ] Sync status bar shows correct states on both devices
- [ ] No data loss during sync test

---

### P6-006 · Smoke Test Checklist — Full Service Day `[MVP]`

**Description:** A written checklist to run before declaring the app ready for production use. Run on the actual devices that will be used in the store.

**Dependencies:** P6-005

**Complexity:** Low (checklist execution) · High (consequences of failure)

**Definition of Done:**
- [ ] Open shift with Rp 100.000 float
- [ ] 3 grab-and-go orders (cash, QRIS, split)
- [ ] 2 dine-in upfront orders on different tables with variants
- [ ] 1 open bill with two rounds of ordering
- [ ] 1 void item with Kitchen Error reason
- [ ] 1 bill payment with exact cash
- [ ] 1 expense logged
- [ ] Close shift — verify Z-report totals match mental arithmetic
- [ ] Check RTDB console — all data present and correctly structured
- [ ] Both EN and ID language verified for all touched screens
- [ ] No crashes recorded in Firebase Crashlytics during the test

---

### P6-007 · README and Setup Documentation `[MVP]`

**Description:** Write `README.md` covering: project setup, Firebase project config, custom claim setup procedure, how to build the release APK, how to update the `minVersionCode` in RTDB when distributing a new version, and the RTDB path structure reference.

**Dependencies:** All prior phases

**Complexity:** Low

**Definition of Done:**
- [ ] New device setup can be completed by following README alone
- [ ] Custom claim setup procedure is step-by-step and verified accurate
- [ ] RTDB path structure section matches architecture Appendix C exactly
- [ ] "How to release a new version" section explains updating `minVersionCode`

---

### P6-008 · Crashlytics Integration `[MVP]`

**Description:** Add Firebase Crashlytics to catch any production crashes. Add `com.google.firebase:firebase-crashlytics-ktx` (via Firebase BOM). Add `id("com.google.firebase.crashlytics")` plugin. Ensure Crashlytics is initialized. Add `FIREBASE_CRASHLYTICS_ENABLED = false` for debug builds to avoid polluting crash reports with dev crashes.

**Dependencies:** P1-002, P6-002

**Complexity:** Low

**Definition of Done:**
- [ ] Crashlytics enabled in release builds only
- [ ] Test crash appears in Firebase Crashlytics dashboard within 5 minutes
- [ ] No PII (user emails, order items) sent to Crashlytics — use generic crash context only

---

### ✅ Phase 6 Gate — Ship It

- [ ] Release APK signed and tested on both real devices
- [ ] Both Firebase accounts set up with correct custom claims
- [ ] RTDB security rules deployed and verified
- [ ] Smoke test checklist completed without blockers
- [ ] README complete
- [ ] Crashlytics active in release build
- [ ] APK shared to both devices via WhatsApp/Drive

---

## Summary Table

| Phase | Tasks | MVP Tasks | P2 Tasks | Estimated Effort |
|-------|-------|-----------|----------|-----------------|
| 1: Setup | 13 | 13 | 0 | ~2-3 days |
| 2: Data Layer | 25 | 24 | 1 (P2-014) | ~4-5 days |
| 3: Domain Layer | 12 | 11 | 1 (P3-012 partial) | ~3-4 days |
| 4: UI Layer | 21 | 19 | 2 (P4-020, P4-021) | ~5-7 days |
| 5: Testing | 12 | 12 | 0 | ~2-3 days |
| 6: Release | 8 | 8 | 0 | ~1-2 days |
| **Total** | **91** | **87** | **4** | **~17-24 days** |

> Effort estimates assume Claude Code assistance for all code generation, with the owner reviewing, testing, and course-correcting. Pure manual development would be 2-3x longer.

## P2 Feature Backlog (After MVP Ships)

Once the MVP is running in production, implement these in order:
1. P4-020 → P3-012 (full reports + export) — data already exists from MVP
2. Stock management screens + use cases (entities already in DB from P2-008)
3. Stock opname session flow
4. Auto stock deduction on payment (phase into `ProcessPaymentUseCase`)
5. Discounts/promos
6. Best seller screen
7. Kitchen order queue display
