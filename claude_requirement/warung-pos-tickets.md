# Warung POS — Implementation Tickets

> **How to use:** Attach `claude-instruction.md` + `warung-pos-architecture.md` to every Claude Code session. Reference the ticket ID in your commit message. One ticket = one session.
>
> **Scope:** `[MVP]` = opening day · `[P2]` = after MVP ships · `[P3]` = future
> **Prereqs listed as TICKET-NNN** — complete those first.

---

# TICKET-001: Gradle Plugins and Build Toolchain
**Roadmap:** P1-001 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** none

## Affected Files
- `build.gradle.kts` (project-level) — modify
- `app/build.gradle.kts` — modify
- `gradle/libs.versions.toml` — create

## Acceptance Criteria
- [ ] KSP plugin active (`id("com.google.devtools.ksp")`); zero `kapt` blocks anywhere
- [ ] Plugins declared: `google-services`, `hilt-android`, `kotlin-serialization`, `ksp`
- [ ] `compileSdk = 35`, `targetSdk = 35`
- [ ] `google-services.json` in `/app` root — no Gradle warning
- [ ] `./gradlew assembleDebug` succeeds on empty project

## Testing
`./gradlew assembleDebug` → BUILD SUCCESSFUL. Grep build files for "kapt" → zero results.

---

# TICKET-002: Gradle Dependencies
**Roadmap:** P1-002 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-001

## Affected Files
- `app/build.gradle.kts` — modify
- `gradle/libs.versions.toml` — modify

## Acceptance Criteria
- [ ] Compose BOM + Firebase BOM declared; all child deps use `platform()` reference
- [ ] Room runtime + ktx + KSP compiler declared
- [ ] Hilt Android + KSP compiler declared
- [ ] WorkManager ktx + `hilt-work` + `hilt-compiler` declared
- [ ] `kotlinx-coroutines-android` + `kotlinx-coroutines-play-services` declared
- [ ] `kotlinx-serialization-json` + serialization plugin declared
- [ ] `security-crypto` declared (EncryptedSharedPreferences)
- [ ] Navigation Compose + `hilt-navigation-compose` declared
- [ ] Firebase RTDB + Auth ktx declared (via BOM)
- [ ] Test deps: JUnit4, Turbine, coroutines-test, MockK, room-testing, hilt-android-testing, compose-ui-test-junit4
- [ ] Zero occurrences of Retrofit, OkHttp, Gson, Moshi, Glide, Coil, Paging3
- [ ] `./gradlew assembleDebug` still compiles clean

## Testing
Grep `app/build.gradle.kts` for "retrofit", "gson", "glide" → zero results.

---

# TICKET-003: Application Class and Hilt Entry Point
**Roadmap:** P1-003 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-002

## Affected Files
- `app/src/main/kotlin/com/warungpos/WarungPosApplication.kt` — create
- `app/src/main/kotlin/com/warungpos/MainActivity.kt` — create
- `app/src/main/kotlin/com/warungpos/WarungPosApp.kt` — create (stub composable)
- `AndroidManifest.xml` — modify (`android:name=".WarungPosApplication"`)

## Acceptance Criteria
- [ ] `WarungPosApplication` annotated `@HiltAndroidApp`, extends `Application`
- [ ] `MainActivity` annotated `@AndroidEntryPoint`, calls `setContent { WarungPosApp() }`
- [ ] `WarungPosApp()` renders an empty `Surface` (no crash, no content yet)
- [ ] App launches on emulator with blank screen and zero Hilt errors in Logcat

## Testing
Launch on emulator → no crash. Logcat filter "Hilt" → no errors.

---

# TICKET-004: Firebase Initialization
**Roadmap:** P1-004 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-003

## Affected Files
- `WarungPosApplication.kt` — modify
- `AndroidManifest.xml` — modify (add `INTERNET` permission)

## Acceptance Criteria
- [ ] `FirebaseDatabase.getInstance().setPersistenceEnabled(true)` called before any RTDB reference is created
- [ ] `setPersistenceCacheSizeBytes(5 * 1024 * 1024L)` called immediately after
- [ ] `<uses-permission android:name="android.permission.INTERNET"/>` in manifest
- [ ] App launches with no Firebase errors in Logcat
- [ ] Temp verification: write `getReference("_test").setValue("ok")`, confirm it appears in RTDB console, then delete the test code

## Testing
Add temp write → verify in Firebase console → remove.

---

# TICKET-005: Core Utilities — UuidGenerator, DateUtil, CurrencyFormatter
**Roadmap:** P1-005 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-001

## Affected Files
- `core/util/UuidGenerator.kt` — create
- `core/util/DateUtil.kt` — create
- `core/util/CurrencyFormatter.kt` — create
- `test/.../util/UuidGeneratorTest.kt` — create
- `test/.../util/DateUtilTest.kt` — create
- `test/.../util/CurrencyFormatterTest.kt` — create

## Acceptance Criteria
- [ ] `UuidGenerator.generate()` returns non-null, non-empty, unique UUID string each call
- [ ] `CurrencyFormatter.format(15000L)` returns `"Rp 15.000"` (dot thousands, no decimal places)
- [ ] `CurrencyFormatter.format(0L)` returns `"Rp 0"`
- [ ] `DateUtil.startOfDay(epochMs)` returns midnight WIB (UTC+7) for that epoch
- [ ] `DateUtil.endOfDay(epochMs)` returns 23:59:59.999 WIB
- [ ] All six unit tests pass on JVM

## Testing
`./gradlew test` → all test files pass.

---

# TICKET-006: SessionManager
**Roadmap:** P1-006 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-003, TICKET-004

## Affected Files
- `core/common/SessionManager.kt` — create
- `core/di/AppModule.kt` — create (partial, SessionManager only for now)

## Acceptance Criteria
- [ ] `SessionManager` is `@Singleton`, injectable via Hilt
- [ ] `currentUser: StateFlow<FirebaseUser?>` emits `null` when not logged in
- [ ] `userRole: StateFlow<UserRole>` emits `UserRole.NONE` when not logged in
- [ ] `UserRole` enum: `OWNER`, `STAFF`, `NONE`
- [ ] Role read from Firebase ID token custom claim key `"role"`; missing claim → `NONE`
- [ ] `deviceId: String` — stable UUID persisted in `EncryptedSharedPreferences`, same value across restarts
- [ ] `suspend fun refreshRole()` forces token refresh and updates `userRole`

## Testing
Manual: fresh install → log `deviceId`; force-stop → relaunch → same `deviceId`.

---

# TICKET-007: NetworkMonitor
**Roadmap:** P1-007 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-003

## Affected Files
- `core/common/NetworkMonitor.kt` — create
- `AndroidManifest.xml` — modify (add `ACCESS_NETWORK_STATE`)
- `core/di/AppModule.kt` — modify (add `NetworkMonitor` provider)

## Acceptance Criteria
- [ ] `isOnline: StateFlow<Boolean>` initialized to correct value on cold start (not always `false`)
- [ ] Emits `true` when WiFi or mobile data connected
- [ ] Emits `false` when airplane mode enabled
- [ ] `ACCESS_NETWORK_STATE` permission in manifest
- [ ] Registered as `@Singleton` — one `NetworkCallback` total, not per observer

## Testing
Manual: toggle airplane mode while app runs → observe Logcat emissions from `isOnline`.

---

# TICKET-008: AppPreferences — Language Setting
**Roadmap:** P1-008 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-003

## Affected Files
- `core/common/AppPreferences.kt` — create
- `core/di/AppModule.kt` — modify (add `AppPreferences` `@Singleton` provider)

## Acceptance Criteria
- [ ] `getLanguage()` returns `"id"` on fresh install (no prior preference)
- [ ] `setLanguage("en")` persists across force-stop + relaunch
- [ ] Implementation uses `EncryptedSharedPreferences.create(...)` — NOT `getSharedPreferences`
- [ ] Injectable as `@Singleton` via Hilt

## Testing
Manual: `setLanguage("en")` → force-stop → relaunch → `getLanguage()` returns `"en"`.

---

# TICKET-009: App Theme — Material 3
**Roadmap:** P1-009 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-003

## Affected Files
- `core/theme/Color.kt` — create
- `core/theme/Type.kt` — create
- `core/theme/Theme.kt` — create (exports `WarungPosTheme`)
- `MainActivity.kt` — modify (wrap `setContent` with `WarungPosTheme`)

## Acceptance Criteria
- [ ] `WarungPosTheme` composable wraps all content in `MainActivity`
- [ ] Custom `ColorScheme` defined (warm, high-contrast palette — not default Material 3 teal)
- [ ] Custom `Typography` scale defined
- [ ] Light mode works; dark mode not required for MVP
- [ ] Zero hardcoded `Color(0xFF...)` literals in any `.kt` file

## Testing
Visual check on emulator — non-default theme colors visible.

---

# TICKET-010: Bilingual String Resources Setup
**Roadmap:** P1-010 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-001

## Affected Files
- `res/values/strings.xml` — create (English, default)
- `res/values-id/strings.xml` — create (Bahasa Indonesia)

## Acceptance Criteria
- [ ] Both files created; `app_name` in both
- [ ] Bottom nav labels in both: `nav_order`, `nav_tables`, `nav_reports`, `nav_more`
- [ ] Common actions in both: `action_save`, `action_cancel`, `action_confirm`, `action_delete`, `action_close`, `action_back`
- [ ] Common statuses in both: `status_open`, `status_paid`, `status_void`, `status_sold_out`
- [ ] No display string literal exists hardcoded in any `.kt` file

## Testing
Switch emulator locale to `id-ID` → relaunch → verify Indonesian strings render.

---

# TICKET-011: Navigation Skeleton — Routes, NavHost, BottomNavBar
**Roadmap:** P1-011 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-003, TICKET-006, TICKET-009

## Affected Files
- `core/navigation/Routes.kt` — create (all route constants)
- `core/navigation/AppNavGraph.kt` — create (NavHost with stub composables for every route)
- `core/navigation/BottomNavBar.kt` — create
- `WarungPosApp.kt` — modify (add `NavController`, `NavHost`, `BottomNavBar`)

## Acceptance Criteria
- [ ] `Routes` object defines string constants for every screen (auth, order, tables, payment, shift, menu, expense, reports, settings, stock)
- [ ] Every route has a `composable()` block rendering an empty `Box` stub
- [ ] Bottom nav shows 4 items: Order (default), Tables, Reports, More
- [ ] Reports tab is hidden when `SessionManager.userRole == STAFF`
- [ ] Staff navigating directly to a Reports route is redirected to Order
- [ ] Back stack: Back from a nested screen returns to parent, not exits app
- [ ] Default start destination: Order screen

## Testing
Tap all 4 nav items → no crash. Set role to STAFF → Reports tab disappears.

---

# TICKET-012: ProGuard / R8 Rules
**Roadmap:** P1-012 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-002

## Affected Files
- `app/proguard-rules.pro` — modify

## Acceptance Criteria
- [ ] Keep rules for `kotlinx.serialization` (serializers survive R8)
- [ ] Keep rules for `com.google.firebase.**`
- [ ] Keep rules for Room `@Entity`, `@Dao`, `@Database` annotated classes
- [ ] Hilt keep rules verified (plugin usually handles; confirm)
- [ ] `./gradlew assembleRelease` produces a signed-debug APK that launches without crash
- [ ] Firebase RTDB connection works in release build

## Testing
`./gradlew assembleRelease` → install on device → app launches → Firebase connects.

---

# TICKET-013: Version Gate — Startup Check Against RTDB
**Roadmap:** P1-013 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-004, TICKET-011

## Affected Files
- `core/navigation/AppNavGraph.kt` — modify (add startup version check logic)
- `feature/auth/UpdateRequiredScreen.kt` — create (non-dismissable stub; content added in TICKET-080)

## Acceptance Criteria
- [ ] On launch while online: read `/appConfig/minVersionCode` from RTDB
- [ ] If `BuildConfig.VERSION_CODE < minVersionCode` → navigate to `UpdateRequiredScreen` (non-dismissable)
- [ ] If offline → skip check, proceed normally (never block offline use)
- [ ] If online + OK version → proceed normally
- [ ] `UpdateRequiredScreen`: back button intercepted (no-op)

## Testing
Set RTDB `minVersionCode: 999` → gate triggers. Enable airplane mode → gate skipped.

---

# TICKET-014: Room Type Converters
**Roadmap:** P2-001 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-001

## Affected Files
- `core/common/SyncStatus.kt` — create (enum: `PENDING`, `SYNCED`, `CONFLICTED`)
- `data/local/db/Converters.kt` — create (`@TypeConverters` class)

## Acceptance Criteria
- [ ] `@TypeConverter` pairs (to/from String) for: `SyncStatus`, `BillType`, `BillStatus`, `OrderItemStatus`, `ShiftStatus`, `OpnameStatus`, `VariantSelectionType`, `VoidReason`, `ExpenseCategory`, `VarianceReason`
- [ ] All converters use `.name` for serialization and `enumValueOf<>()` for deserialization (never `.ordinal`)
- [ ] Compile with no Room processor errors

## Testing
Unit tests: `Converters().fromBillStatus(BillStatus.PAID)` → `"PAID"`. `toBillStatus("OPEN")` → `BillStatus.OPEN`.

---

# TICKET-015: Room Entities — Menu (Category, Item, VariantGroup, VariantOption)
**Roadmap:** P2-003 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-014

## Affected Files
- `data/local/entity/MenuCategoryEntity.kt` — create
- `data/local/entity/MenuItemEntity.kt` — create
- `data/local/entity/VariantGroupEntity.kt` — create
- `data/local/entity/VariantOptionEntity.kt` — create

## Acceptance Criteria
- [ ] `MenuCategoryEntity`: `id(String PK)`, `name`, `sortOrder(Int)`, sync fields
- [ ] `MenuItemEntity`: `id(String PK)`, `categoryId(String? FK → MenuCategory onDelete=SET_NULL)`, `name`, `basePrice(Long)`, `isAvailable(Boolean)`, `isSoldOut(Boolean)`, sync fields; `@Index("categoryId")`
- [ ] `VariantGroupEntity`: `id(String PK)`, `menuItemId(String FK → MenuItem onDelete=CASCADE)`, `name`, `selectionType(String)`, `isRequired(Boolean)`, sync fields; `@Index("menuItemId")`
- [ ] `VariantOptionEntity`: `id(String PK)`, `variantGroupId(String FK → VariantGroup onDelete=CASCADE)`, `name`, `priceDelta(Long)`, sync fields; `@Index("variantGroupId")`
- [ ] Zero `Float`/`Double` fields — `basePrice` and `priceDelta` are `Long`
- [ ] All PKs are `String` (UUID) — no `autoGenerate`

## Testing
`./gradlew kspDebugKotlin` → no Room processor errors.

---

# TICKET-016: Room Entities — Table, Bill, OrderItem
**Roadmap:** P2-004 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-015

## Affected Files
- `data/local/entity/TableEntity.kt` — create
- `data/local/entity/BillEntity.kt` — create
- `data/local/entity/OrderItemEntity.kt` — create

## Acceptance Criteria
- [ ] `TableEntity`: `id(String PK)`, `label(String?)`, `isActive(Boolean)`, sync fields
- [ ] `BillEntity`: `id(String PK)`, `tableId(String? FK nullable → Table)`, `type(String)`, `status(String)`, `sessionLabel(String)`, `createdAt(Long)`, `paidAt(Long?)`, `subtotal(Long)`, `discountTotal(Long)`, `grandTotal(Long)`, `note(String?)`, `shiftId(String? FK nullable)`, `voidReason(String?)`, `voidedBy(String?)`, sync fields; `@Index("tableId")`, `@Index("shiftId")`, `@Index("status")`
- [ ] `OrderItemEntity`: `id(String PK)`, `billId(String FK → Bill onDelete=CASCADE)`, `menuItemId(String FK)`, `nameSnapshot(String)`, `priceSnapshot(Long)`, `quantity(Int)`, `selectedVariantsJson(String)`, `lineTotal(Long)`, `status(String)`, `voidReason(String?)`, `voidedBy(String?)`, `createdAt(Long)`, sync fields; `@Index("billId")`
- [ ] `BillEntity.tableId` FK declared as nullable — grab-and-go bills legal
- [ ] Zero `Float`/`Double` monetary fields

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-017: Room Entities — Payment (PaymentMethod, Payment)
**Roadmap:** P2-005 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-016

## Affected Files
- `data/local/entity/PaymentMethodEntity.kt` — create
- `data/local/entity/PaymentEntity.kt` — create

## Acceptance Criteria
- [ ] `PaymentMethodEntity`: `id(String PK)`, `name(String)`, `isEnabled(Boolean)`, `sortOrder(Int)`, sync fields
- [ ] `PaymentEntity`: `id(String PK)`, `billId(String FK → Bill onDelete=CASCADE)`, `paymentMethodId(String FK)`, `amount(Long)`, `amountTendered(Long?)`, `changeGiven(Long?)`, `paidAt(Long)`, sync fields; `@Index("billId")`
- [ ] `amountTendered` and `changeGiven` are nullable — only populated for cash
- [ ] All monetary fields are `Long`

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-018: Room Entities — Shift, ZReport
**Roadmap:** P2-006 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-016

## Affected Files
- `data/local/entity/ShiftEntity.kt` — create
- `data/local/entity/ZReportEntity.kt` — create

## Acceptance Criteria
- [ ] `ShiftEntity`: `id(String PK)`, `openedAt(Long)`, `closedAt(Long?)`, `openedBy(String)`, `openingFloat(Long)`, `countedCash(Long?)`, `expectedCash(Long)`, `variance(Long?)`, `status(String)`, sync fields
- [ ] `ZReportEntity`: `id(String PK)`, `shiftId(String FK unique)`, `snapshotJson(String)`, `createdAt(Long)` — **NO sync fields** (immutable, never synced)
- [ ] `ZReportEntity.shiftId` has `@Index(unique = true)` — one Z-report per shift
- [ ] Code review confirms `ZReportEntity` has zero `updatedAt`/`syncStatus`/`deviceId` fields

## Testing
`./gradlew kspDebugKotlin` → no errors. Manual code review: `ZReportEntity` must have exactly 4 fields.

---

# TICKET-019: Room Entity — Expense
**Roadmap:** P2-007 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-014

## Affected Files
- `data/local/entity/ExpenseEntity.kt` — create

## Acceptance Criteria
- [ ] Fields: `id(String PK)`, `category(String)`, `amount(Long)`, `date(Long)`, `note(String?)`, `recordedBy(String)`, sync fields
- [ ] `@Index("date")` declared — reports filter by date range
- [ ] `amount` is `Long`, not Float/Double

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-020: Room Entities — Stock (Schema-Only, No Use Cases Yet)
**Roadmap:** P2-008 · **Scope:** P2 · **Est.:** ~1.5h · **Prereqs:** TICKET-015

## Affected Files
- `data/local/entity/StockItemEntity.kt` — create
- `data/local/entity/StockBatchEntity.kt` — create
- `data/local/entity/MenuItemIngredientEntity.kt` — create
- `data/local/entity/StockOpnameEntity.kt` — create
- `data/local/entity/StockOpnameLineEntity.kt` — create

## Acceptance Criteria
- [ ] `StockItemEntity`: `id(String PK)`, `name`, `unit(String)`, `currentQuantity(Double)`, `lowStockThreshold(Double)`, `lastCostPrice(Long)`, sync fields
- [ ] `StockBatchEntity`: `id(String PK)`, `stockItemId(String FK)`, `quantity(Double)`, `purchasePrice(Long)`, `purchaseDate(Long)`, `supplier(String?)`, sync fields
- [ ] `MenuItemIngredientEntity`: `@Entity(primaryKeys = ["menuItemId","stockItemId"])`, `quantityUsed(Double)`, sync fields
- [ ] `StockOpnameEntity`: `id(String PK)`, `startedAt(Long)`, `completedAt(Long?)`, `status(String)`, `conductedBy(String)`, `note(String?)`, sync fields
- [ ] `StockOpnameLineEntity`: `id(String PK)`, `opnameId(String FK)`, `stockItemId(String FK)`, `expectedQty(Double)`, `actualQty(Double)`, `variance(Double)`, `varianceReason(String)`, `costImpact(Long)`, sync fields
- [ ] All FK constraints and indexes declared
- [ ] **No DAO, use case, or screen references these entities** — schema-only

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-021: WarungDatabase
**Roadmap:** P2-009 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-014 through TICKET-020

## Affected Files
- `data/local/db/WarungDatabase.kt` — create
- `core/di/AppModule.kt` — modify (add database + all DAO providers)

## Acceptance Criteria
- [ ] `@Database(entities = [...all 19 entities...], version = 1)` — no entity missing
- [ ] `@TypeConverters(Converters::class)` applied
- [ ] `Room.databaseBuilder(context, WarungDatabase::class.java, "warung_pos_db")` — **no** `.fallbackToDestructiveMigration()`
- [ ] Abstract DAO getter declared for every DAO (added in subsequent tickets — use empty stubs now)
- [ ] Provided as `@Singleton` in `AppModule`
- [ ] App launches — `warung_pos_db` visible in Android Studio Database Inspector with all tables

## Testing
Launch app → Database Inspector → `warung_pos_db` present with all 19 tables (including stock at 0 rows).

---

# TICKET-022: Room DAOs — MenuCategoryDao, MenuItemDao, VariantDao
**Roadmap:** P2-010 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-021

## Affected Files
- `data/local/dao/MenuCategoryDao.kt` — create
- `data/local/dao/MenuItemDao.kt` — create
- `data/local/dao/VariantDao.kt` — create

## Acceptance Criteria
- [ ] `MenuCategoryDao`: `observeAll(): Flow<List<MenuCategoryEntity>>`, `upsert(entity)`, `delete(id)`, `getById(id): MenuCategoryEntity?`
- [ ] `MenuItemDao`: `observeAvailable(): Flow<List<MenuItemEntity>>` (WHERE `isAvailable=1`), `observeAll()`, `upsert()`, `softDelete(id)` (SET `isAvailable=0`), `setSoldOut(id, bool)`, `resetAllSoldOut()` (UPDATE ALL SET `isSoldOut=0`), `getPendingSync(): List<MenuItemEntity>`
- [ ] `VariantDao`: `observeGroupsForItem(menuItemId): Flow<List<VariantGroupEntity>>`, `observeOptionsForGroup(groupId): Flow<List<VariantOptionEntity>>`, `upsertGroup()`, `upsertOption()`, `deleteGroup(id)`, `deleteOption(id)`
- [ ] All `observe*()` return `Flow` (not `suspend`)
- [ ] All writes are `suspend`

## Testing
`./gradlew kspDebugKotlin` → no Room processor errors.

---

# TICKET-023: Room DAOs — TableDao, BillDao, OrderItemDao
**Roadmap:** P2-011 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-021

## Affected Files
- `data/local/dao/TableDao.kt` — create
- `data/local/dao/BillDao.kt` — create
- `data/local/dao/OrderItemDao.kt` — create

## Acceptance Criteria
- [ ] `TableDao`: `observeActive(): Flow<List<TableEntity>>`, `observeAll()`, `upsert()`, `deactivate(id)` (SET `isActive=0`)
- [ ] `BillDao`: `observeOpenBills(): Flow<List<BillEntity>>` (WHERE `status='OPEN'`), `observeOpenBillsForTable(tableId)`, `observeBillsForShift(shiftId)`, `getById(id): BillEntity?`, `insert(entity)`, `updateStatus(id, status, paidAt, shiftId)`, `voidBill(id, reason, voidedBy, updatedAt)`, `countOpenBills(): Int`, `getPendingSync()`
- [ ] `OrderItemDao`: `observeForBill(billId): Flow<List<OrderItemEntity>>`, `insertAll(items: List<OrderItemEntity>)`, `voidItem(id, reason, voidedBy, updatedAt)`, `getPendingSync()`
- [ ] `countOpenBills()` returns 0 correctly when table is empty

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-024: Room DAOs — PaymentMethodDao, PaymentDao
**Roadmap:** P2-012 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-021

## Affected Files
- `data/local/dao/PaymentMethodDao.kt` — create
- `data/local/dao/PaymentDao.kt` — create
- `data/local/dao/pojo/PaymentSumByMethod.kt` — create (data class, not `@Entity`)

## Acceptance Criteria
- [ ] `PaymentMethodDao`: `observeEnabled(): Flow<List<PaymentMethodEntity>>`, `observeAll()`, `upsert()`, `setEnabled(id, bool)`, `updateSortOrder(id, order)`, `insertAll(methods: List<PaymentMethodEntity>)`
- [ ] `PaymentDao`: `observeForBill(billId): Flow<List<PaymentEntity>>`, `insertAll(payments)`, `sumForShiftByMethod(shiftId): List<PaymentSumByMethod>`, `getPendingSync()`
- [ ] `sumForShiftByMethod` uses `GROUP BY paymentMethodId` JOIN query
- [ ] `PaymentSumByMethod(paymentMethodId: String, total: Long)` — plain data class

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-025: Room DAOs — ShiftDao, ZReportDao, ExpenseDao
**Roadmap:** P2-013 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-021

## Affected Files
- `data/local/dao/ShiftDao.kt` — create
- `data/local/dao/ZReportDao.kt` — create
- `data/local/dao/ExpenseDao.kt` — create

## Acceptance Criteria
- [ ] `ShiftDao`: `observeOpenShift(): Flow<ShiftEntity?>` (WHERE `status='OPEN'` LIMIT 1), `getOpenShift(): ShiftEntity?`, `insert()`, `closeShift(id, closedAt, countedCash, expectedCash, variance, updatedAt)`, `observeHistory(): Flow<List<ShiftEntity>>`
- [ ] `ZReportDao`: `insert()`, `getByShiftId(id): ZReportEntity?`, `observeAll(): Flow<List<ZReportEntity>>`
- [ ] `ExpenseDao`: `observeForDateRange(start, end): Flow<List<ExpenseEntity>>`, `insert()`, `sumForShift(shiftId): Long`, `sumForDateRange(start, end): Long`, `getPendingSync()`
- [ ] `observeOpenShift()` emits `null` when zero OPEN shifts exist

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-026: Room DAOs — Stock Stubs
**Roadmap:** P2-014 · **Scope:** P2 · **Est.:** ~45m · **Prereqs:** TICKET-021

## Affected Files
- `data/local/dao/StockDao.kt` — create
- `data/local/dao/StockOpnameDao.kt` — create

## Acceptance Criteria
- [ ] `StockDao`: `observeAll(): Flow<List<StockItemEntity>>`, `upsertItem()`, `upsertBatch()`, `decrementQuantity(stockItemId, amount)` (single UPDATE: `currentQuantity = currentQuantity - :amount`), `observeLowStock(): Flow<List<StockItemEntity>>`
- [ ] `StockOpnameDao`: `insertSession()`, `observeActiveSession(): Flow<StockOpnameEntity?>`, `insertLine()`
- [ ] `decrementQuantity` is a single SQL UPDATE — not a read-modify-write pattern

## Testing
`./gradlew kspDebugKotlin` → no errors.

---

# TICKET-027: Room DAOs — ReportQueryDao and POJOs
**Roadmap:** P2-015 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-021, TICKET-023, TICKET-024

## Affected Files
- `data/local/dao/ReportQueryDao.kt` — create
- `data/local/dao/pojo/BestSellerRow.kt` — create
- `data/local/dao/pojo/VoidSummary.kt` — create

## Acceptance Criteria
- [ ] `totalSalesForDateRange(start, end): Long` — `SUM(grandTotal) WHERE status='PAID' AND paidAt BETWEEN start AND end`
- [ ] `transactionCountForDateRange(start, end): Int`
- [ ] `observeBestSellers(start, end, limit): Flow<List<BestSellerRow>>` — JOIN order_items, GROUP BY menuItemId, SUM(quantity) excluding voided items, ORDER BY totalQty DESC
- [ ] `salesByPaymentMethod(start, end): List<PaymentSumByMethod>` — JOIN payments + bills on date range
- [ ] `totalVoidsForShift(shiftId): VoidSummary` — COUNT + SUM of voided bills in shift
- [ ] `BestSellerRow(menuItemId: String, nameSnapshot: String, totalQty: Int, totalRevenue: Long)`
- [ ] `VoidSummary(count: Int, totalValue: Long)`

## Testing
`./gradlew kspDebugKotlin` → Room validates SQL at compile time; all queries must compile.

---

# TICKET-028: FirebaseAuthDataSource
**Roadmap:** P2-016 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-006

## Affected Files
- `data/remote/firebase/FirebaseAuthDataSource.kt` — create

## Acceptance Criteria
- [ ] `suspend fun signIn(email, password): Result<FirebaseUser>` — `signInWithEmailAndPassword().await()` wrapped in `runCatching`
- [ ] `suspend fun signOut()`
- [ ] `fun observeAuthState(): Flow<FirebaseUser?>` — `callbackFlow` with `AuthStateListener`; `awaitClose { auth.removeAuthStateListener(listener) }`
- [ ] `suspend fun getUserRole(): UserRole` — force-refreshes ID token, reads `claims["role"]`; missing → `NONE`
- [ ] Wrong credentials return `Result.failure(Exception)` — never crash

## Testing
Manual: `signIn` with wrong credentials → `Result.failure`, no crash or ANR.

---

# TICKET-029: FirebaseRtdbDataSource
**Roadmap:** P2-017 · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-004

## Affected Files
- `data/remote/firebase/FirebaseRtdbDataSource.kt` — create

## Acceptance Criteria
- [ ] `suspend fun write(path: String, value: Any?): Result<Unit>` — field-level `setValue().await()`
- [ ] `suspend fun writeMulti(updates: Map<String, Any?>): Result<Unit>` — `updateChildren().await()`
- [ ] `suspend fun runTransaction(path: String, block: (Any?) -> Any?): Result<Unit>`
- [ ] `fun observe(path: String): Flow<DataSnapshot>` — `callbackFlow` + `ValueEventListener`; `awaitClose { ref.removeEventListener(listener) }`
- [ ] `fun observeChildren(path: String): Flow<DataSnapshot>` — `callbackFlow` + `ChildEventListener`; `awaitClose`
- [ ] `suspend fun increment(path: String, delta: Double): Result<Unit>` — uses `ServerValue.increment(delta)` (NOT read-modify-write)
- [ ] `suspend fun get(path: String): Result<DataSnapshot>`
- [ ] `suspend fun delete(path: String): Result<Unit>`
- [ ] Every method wraps exceptions in `Result.failure` — none throw to callers

## Testing
Manual: `write("test/key", "hello")` → verify in RTDB console. `increment("test/n", 1.0)` twice → value = 2.

---

# TICKET-030: ConflictResolver
**Roadmap:** P2-018 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-014

## Affected Files
- `data/remote/sync/ConflictResolution.kt` — create (enum: `ACCEPT`, `REJECT`)
- `data/remote/sync/ConflictResolver.kt` — create

## Acceptance Criteria
- [ ] `fun resolve(incomingUpdatedAt: Long, existingUpdatedAt: Long?, incomingStatus: String?, existingStatus: String?): ConflictResolution`
- [ ] `incomingUpdatedAt > existingUpdatedAt` → `ACCEPT`
- [ ] `incomingUpdatedAt <= existingUpdatedAt` → `REJECT` (local wins on tie)
- [ ] `existingUpdatedAt == null` (new entity) → `ACCEPT`
- [ ] `existingStatus == "PAID"` and `incomingStatus == "OPEN"` → always `REJECT`
- [ ] `existingStatus == "VOID"` and `incomingStatus != "VOID"` → always `REJECT`
- [ ] `existingStatus == "OPEN"` + newer incoming → `ACCEPT` (normal LWW)

## Testing
Unit tests written in TICKET-093. Compile only for now.

---

# TICKET-031: RtdbListener
**Roadmap:** P2-019 · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-029, TICKET-030, TICKET-023, TICKET-024, TICKET-025

## Affected Files
- `data/remote/sync/RtdbListener.kt` — create

## Acceptance Criteria
- [ ] `ChildEventListener` registered on all MVP paths: `/bills`, `/orderItems`, `/payments`, `/menuItems`, `/menuCategories`, `/variantGroups`, `/variantOptions`, `/tables`, `/paymentMethods`, `/shifts`, `/expenses`
- [ ] For each child event: calls `ConflictResolver.resolve(...)`, writes to Room if `ACCEPT`
- [ ] Maps `DataSnapshot` → correct entity type for each path (via `getValue(EntityClass::class.java)`)
- [ ] `onCancelled` logs the error but does not crash
- [ ] Stock paths registered as stubs that no-op in MVP (listeners exist, no DAO writes)

## Testing
Manual: write a bill to RTDB console → verify it appears in Room via Database Inspector.

---

# TICKET-032: SyncWorker
**Roadmap:** P2-020 · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-029, TICKET-022 through TICKET-027

## Affected Files
- `data/remote/sync/SyncWorker.kt` — create
- `core/di/WorkerModule.kt` — create
- `AndroidManifest.xml` — modify (disable default WorkManager `InitializationProvider`)

## Acceptance Criteria
- [ ] `SyncWorker` extends `CoroutineWorker`, annotated `@HiltWorker`
- [ ] Injects all DAOs + `FirebaseRtdbDataSource` via `@AssistedInject`
- [ ] `doWork()`: query all `getPendingSync()` DAOs ordered by `updatedAt ASC`; for each, call `rtdbDataSource.write(path, data)`; on success set `syncStatus = SYNCED` in Room
- [ ] `Result.retry()` on transient Firebase failure; `Result.success()` when done
- [ ] `WorkerModule` provides `WorkManagerConfiguration` using `HiltWorkerFactory`
- [ ] Default WorkManager `<provider>` removed from manifest

## Testing
Manual: create PENDING record → airplane mode → trigger worker → stays PENDING → reconnect → syncs to RTDB.

---

# TICKET-033: SyncCoordinator
**Roadmap:** P2-021 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-030, TICKET-031, TICKET-032, TICKET-007

## Affected Files
- `data/remote/sync/SyncState.kt` — create (enum: `IDLE`, `SYNCING`, `OFFLINE`, `ERROR`)
- `data/remote/sync/SyncCoordinator.kt` — create

## Acceptance Criteria
- [ ] `@Singleton` injectable via Hilt
- [ ] `fun notifyPendingSync()` — enqueues `SyncWorker` as `OneTimeWorkRequest` with `NetworkType.CONNECTED`; `ExistingWorkPolicy.APPEND_OR_REPLACE`
- [ ] `val pendingCount: Flow<Int>` — counts PENDING records across all syncable entities in Room
- [ ] `val syncState: StateFlow<SyncState>` — `OFFLINE` when `NetworkMonitor.isOnline = false`, `SYNCING` when worker active, `IDLE` otherwise
- [ ] `fun startListening()` — starts `RtdbListener`; **only called after confirmed auth**
- [ ] Periodic `PeriodicWorkRequest` for backup sync every 15 minutes

## Testing
Manual: toggle airplane mode → `syncState` transitions `IDLE → OFFLINE → SYNCING → IDLE` on reconnect.

---

# TICKET-034: Hilt DI Modules
**Roadmap:** P2-022 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-021, TICKET-028, TICKET-029, TICKET-033

## Affected Files
- `core/di/AppModule.kt` — complete (consolidate all partial `@Provides` from prior tickets)
- `core/di/RepositoryModule.kt` — create (empty `@Module @InstallIn(SingletonComponent::class)`)
- `core/di/WorkerModule.kt` — already exists from TICKET-032; verify complete

## Acceptance Criteria
- [ ] `AppModule` provides as `@Singleton`: `WarungDatabase`, all DAOs, `FirebaseDatabase`, `FirebaseAuth`, `FirebaseRtdbDataSource`, `FirebaseAuthDataSource`, `SyncCoordinator`, `NetworkMonitor`, `SessionManager`, `AppPreferences`, `FirstRunManager`
- [ ] `RepositoryModule` exists as empty module — prevents missing binding errors when impls added in Phase 3
- [ ] `WorkerModule` provides `WorkManagerConfiguration` via `HiltWorkerFactory`
- [ ] `./gradlew assembleDebug` — zero Hilt graph errors

## Testing
`./gradlew assembleDebug` → BUILD SUCCESSFUL, no Hilt dependency errors.

---

# TICKET-035: RTDB Security Rules
**Roadmap:** P2-023 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-004

## Affected Files
- `firebase/database.rules.json` — create

## Acceptance Criteria
- [ ] Root: `".read": "auth != null"`, `".write": "auth != null"`
- [ ] `/bills/{id}/status`: write validation — new data must be `"PAID"` or `"VOID"` (prevents OPEN regression)
- [ ] Rules deployed to Firebase project (not just draft/saved locally)
- [ ] Unauthenticated REST GET returns `403 Permission Denied`
- [ ] Authenticated read/write succeeds
- [ ] File committed to repo at `firebase/database.rules.json`

## Testing
Firebase console Simulator: unauthenticated read → denied. Set PAID bill status to "OPEN" → rejected.

---

# TICKET-036: RTDB AppConfig Manual Setup
**Roadmap:** P2-024 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-004

## Affected Files
- `docs/firebase-setup.md` — create (documents steps taken)

## Acceptance Criteria
- [ ] `/appConfig/minVersionCode: 1` in RTDB console
- [ ] `/appConfig/openShiftId: null` in RTDB console
- [ ] RTDB tree matches architecture Appendix C structure
- [ ] Test: set `minVersionCode: 999` → version gate fires; reset to `1` → normal launch
- [ ] Steps documented in `docs/firebase-setup.md`

## Testing
Set `minVersionCode: 999` → gate triggers; set back to `1` → normal.

---

# TICKET-037: First-Run Data Seeding
**Roadmap:** P2-025 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-024, TICKET-034

## Affected Files
- `core/common/FirstRunManager.kt` — create
- `WarungPosApplication.kt` — modify (call `firstRunManager.runIfNeeded()` in `onCreate`)

## Acceptance Criteria
- [ ] On first launch: inserts 5 `PaymentMethodEntity` rows — Tunai (sortOrder=1), QRIS (2), GoPay (3), OVO (4), Transfer Bank (5); all `isEnabled=true`; all with UUID PKs; `syncStatus=PENDING`
- [ ] On second launch: seeding does NOT run again (guarded by a boolean key in `AppPreferences`)
- [ ] 5 payment method rows visible in Database Inspector after first launch
- [ ] Guard key persists across force-stop

## Testing
Fresh install → Database Inspector → exactly 5 payment rows. Force-stop → relaunch → still 5 (not 10).

---

# TICKET-038: Phase 2 Gate Verification
**Roadmap:** P2 gate · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-021 through TICKET-037

> This is a **verification ticket** — no new code. Run all checks listed below before starting Phase 3.

## Affected Files
None — verification only.

## Acceptance Criteria
- [ ] `./gradlew assembleDebug` succeeds with all entities and DAOs present
- [ ] Database Inspector: all tables visible including stock tables with 0 rows
- [ ] Manually trigger `SyncWorker` → it pushes a seeded payment method to RTDB
- [ ] RTDB Listener receives a change made in Firebase console → appears in Room
- [ ] Firebase Auth login works on a physical or emulator device
- [ ] `./gradlew test` passes (utility unit tests from TICKET-005)

## Testing
Run every checklist item manually and mark each done.
---

# TICKET-039: Rupiah Value Class and Domain Enums
**Roadmap:** P3-001 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-001

## Affected Files
- `domain/model/Rupiah.kt` — create
- `domain/model/enums/BillType.kt` — create (`UPFRONT`, `OPEN_BILL`)
- `domain/model/enums/BillStatus.kt` — create (`OPEN`, `PAID`, `VOID`)
- `domain/model/enums/OrderItemStatus.kt` — create (`ORDERED`, `DONE`, `VOID`)
- `domain/model/enums/ShiftStatus.kt` — create (`OPEN`, `CLOSED`)
- `domain/model/enums/VoidReason.kt` — create (`CUSTOMER_CHANGE`, `KITCHEN_ERROR`, `ITEM_UNAVAILABLE`, `TEST`, `OTHER`)
- `domain/model/enums/ExpenseCategory.kt` — create (Gas, Packaging, Cleaning, Salary, Other)
- `domain/model/enums/VariantSelectionType.kt` — create (`SINGLE`, `MULTIPLE`)
- `domain/model/enums/UserRole.kt` — create (`OWNER`, `STAFF`, `NONE`)
- `test/.../model/RupiahTest.kt` — create

## Acceptance Criteria
- [ ] `@JvmInline value class Rupiah(val value: Long)` with `+`, `-` operators and `companion object { val ZERO = Rupiah(0L) }`
- [ ] `Rupiah(15000L) + Rupiah(5000L) == Rupiah(20000L)`
- [ ] `Rupiah.ZERO == Rupiah(0L)`
- [ ] Zero Android imports in any file in this ticket
- [ ] Enum values match TypeConverter strings exactly (case-sensitive)

## Testing
`./gradlew test` → `RupiahTest` passes.

---

# TICKET-040: Domain Models — Core
**Roadmap:** P3-002 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-039

## Affected Files
- `domain/model/MenuCategory.kt` — create
- `domain/model/MenuItem.kt` — create (includes `variantGroups: List<VariantGroup>`)
- `domain/model/VariantGroup.kt` — create (includes `options: List<VariantOption>`)
- `domain/model/VariantOption.kt` — create
- `domain/model/Table.kt` — create
- `domain/model/Bill.kt` — create (includes `orderItems: List<OrderItem>`, `payments: List<Payment>`)
- `domain/model/OrderItem.kt` — create (includes `selectedVariants: List<SelectedVariant>`)
- `domain/model/SelectedVariant.kt` — create
- `domain/model/CartItem.kt` — create (in-memory only, no PK)
- `domain/model/Payment.kt` — create
- `domain/model/PaymentMethod.kt` — create
- `domain/model/Shift.kt` — create
- `domain/model/Expense.kt` — create

## Acceptance Criteria
- [ ] All monetary fields use `Rupiah` (not `Long`)
- [ ] Zero Android/Room/Firebase imports in any file
- [ ] `CartItem` has no `id` field — never persisted
- [ ] `MenuItem.variantGroups` is a `List<VariantGroup>` assembled at query time (not a DB column)
- [ ] `OrderItem.selectedVariants: List<SelectedVariant>` (deserialized from JSON by mapper)

## Testing
`./gradlew compileDebugKotlin` → compiles clean.

---

# TICKET-041: Domain Models — Reporting and Exceptions
**Roadmap:** P3-002 (cont.) · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-039

## Affected Files
- `domain/model/ZReport.kt` — create (`@Serializable`, all fields typed)
- `domain/model/report/DailyDashboard.kt` — create
- `domain/model/report/DateRangeReport.kt` — create [P2]
- `domain/model/report/BestSellerItem.kt` — create [P2]
- `domain/exception/PosExceptions.kt` — create (all exception classes)

## Acceptance Criteria
- [ ] `ZReport` is annotated `@Serializable` — required for `ZReportMapper` JSON round-trip
- [ ] `DailyDashboard`: `grossSales: Rupiah`, `transactionCount: Int`, `paymentBreakdown: Map<String, Rupiah>`, `topItems: List<BestSellerItem>`, `cashVariance: Rupiah`
- [ ] Exception classes: `ShiftNotOpenException`, `EmptyCartException`, `BillAlreadyPaidException`, `InsufficientPermissionsException`, `OpenBillsBlockShiftCloseException(openBills: List<Bill>)`, `MissingRequiredVariantException`, `InsufficientTenderedAmountException`, `InsufficientPaymentException`, `BillNotVoidableException`

## Testing
`./gradlew compileDebugKotlin` → clean.

---

# TICKET-042: Repository Interfaces — Menu, Bill, Order, Payment
**Roadmap:** P3-003 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-040, TICKET-041

## Affected Files
- `domain/repository/MenuRepository.kt` — create
- `domain/repository/BillRepository.kt` — create
- `domain/repository/OrderRepository.kt` — create
- `domain/repository/PaymentRepository.kt` — create

## Acceptance Criteria
- [ ] `MenuRepository`: `observeMenuWithVariants(): Flow<List<MenuItem>>`, `observeCategories(): Flow<List<MenuCategory>>`, `upsert(item)`, `upsertCategory(cat)`, `softDelete(id)`, `resetAllSoldOut()`, `toggleSoldOut(id, bool)`, `getAll(): List<MenuItem>`
- [ ] `BillRepository`: `observeOpenBills(): Flow<List<Bill>>`, `observeBillDetail(id): Flow<Bill>`, `createBill(bill, items)`, `voidBill(id, reason, voidedBy)`, `countOpenBills(): Int`
- [ ] `OrderRepository`: `confirmOrder(bill, items)`, `addItemsToExistingBill(billId, items)`, `voidOrderItem(itemId, reason, note, voidedBy)`
- [ ] `PaymentRepository`: `processPayment(billId, payments)`, `observeForBill(billId): Flow<List<Payment>>`, `getEnabledMethods(): List<PaymentMethod>`, `observeEnabledMethods(): Flow<List<PaymentMethod>>`
- [ ] Zero data-layer imports in any interface

## Testing
`./gradlew compileDebugKotlin` → clean.

---

# TICKET-043: Repository Interfaces — Shift, Expense, Report, Stock, Auth
**Roadmap:** P3-003 (cont.) · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-040, TICKET-041

## Affected Files
- `domain/repository/ShiftRepository.kt` — create
- `domain/repository/ExpenseRepository.kt` — create
- `domain/repository/ReportRepository.kt` — create
- `domain/repository/StockRepository.kt` — create (stub signatures)
- `domain/repository/AuthRepository.kt` — create

## Acceptance Criteria
- [ ] `ShiftRepository`: `observeOpenShift(): Flow<Shift?>`, `getOpenShift(): Shift?`, `openShift(shift)`, `closeShift(shiftId, countedCash, expectedCash, variance)`, `saveZReport(report)`, `countOpenBills(): Int`, `observeHistory(): Flow<List<Shift>>`
- [ ] `ExpenseRepository`: `logExpense(expense)`, `observeForDateRange(start, end): Flow<List<Expense>>`
- [ ] `ReportRepository`: `getDailyDashboard(dateEpoch): DailyDashboard`, `getDateRangeReport(start, end): DateRangeReport`, `observeBestSellers(start, end): Flow<List<BestSellerItem>>`
- [ ] `StockRepository`: minimal stubs that compile — `observeAll(): Flow<List<Any>>` etc.
- [ ] `AuthRepository`: `signIn(email, password): Result<UserRole>`, `signOut()`, `observeAuthState(): Flow<Boolean>`

## Testing
`./gradlew compileDebugKotlin` → clean.

---

# TICKET-044: Mappers — Menu and OrderItem
**Roadmap:** P3-004 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-040, TICKET-015, TICKET-016

## Affected Files
- `data/mapper/MenuCategoryMapper.kt` — create
- `data/mapper/MenuItemMapper.kt` — create
- `data/mapper/VariantMapper.kt` — create
- `data/mapper/OrderItemMapper.kt` — create

## Acceptance Criteria
- [ ] `MenuCategoryEntity.toDomain(): MenuCategory` and `MenuCategory.toEntity(): MenuCategoryEntity`
- [ ] `MenuItemEntity.toDomain(variantGroups: List<VariantGroup>): MenuItem` and `MenuItem.toEntity(): MenuItemEntity`
- [ ] `VariantGroupEntity.toDomain(options): VariantGroup`, `VariantOptionEntity.toDomain(): VariantOption`
- [ ] `OrderItemEntity.toDomain(): OrderItem` — deserializes `selectedVariantsJson` via `Json.decodeFromString<List<SelectedVariant>>(...)`
- [ ] `OrderItem.toEntity(billId, syncStatus, deviceId): OrderItemEntity` — serializes selectedVariants to JSON via `Json.encodeToString(...)`
- [ ] `basePrice: Long` in entity ↔ `Rupiah(basePrice)` in domain — verified for all monetary fields

## Testing
Unit tests written in TICKET-094. Compile only for now.

---

# TICKET-045: Mappers — Bill, Payment, Shift, ZReport, Expense
**Roadmap:** P3-004 (cont.) · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-040, TICKET-016, TICKET-017, TICKET-018, TICKET-019

## Affected Files
- `data/mapper/BillMapper.kt` — create
- `data/mapper/PaymentMapper.kt` — create
- `data/mapper/ShiftMapper.kt` — create
- `data/mapper/ZReportMapper.kt` — create
- `data/mapper/ExpenseMapper.kt` — create

## Acceptance Criteria
- [ ] `BillMapper`: `BillEntity.toDomain(items, payments): Bill` and `Bill.toEntity(): BillEntity`
- [ ] `PaymentMapper`: bidirectional; `amount: Long ↔ Rupiah`
- [ ] `ShiftMapper`: bidirectional; all `Long` monetary fields map to `Rupiah`
- [ ] `ZReportMapper`: `ZReportEntity.toDomain(): ZReport` — `Json.decodeFromString<ZReport>(snapshotJson)`; `ZReport.toSnapshotJson(): String` — `Json.encodeToString(...)`
- [ ] `ExpenseMapper`: bidirectional; `amount: Long ↔ Rupiah`
- [ ] No fields silently dropped in any mapper

## Testing
Unit tests written in TICKET-094. Compile only for now.

---

# TICKET-046: Repository Implementations — Menu and Order
**Roadmap:** P3-005 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-042, TICKET-044, TICKET-033

## Affected Files
- `data/repository/MenuRepositoryImpl.kt` — create
- `data/repository/OrderRepositoryImpl.kt` — create
- `core/di/RepositoryModule.kt` — modify (add `@Binds` for both)

## Acceptance Criteria
- [ ] Write sequence in every write method: (1) map domain→entity with `syncStatus=PENDING`, (2) write to Room, (3) call `syncCoordinator.notifyPendingSync()`
- [ ] `MenuRepositoryImpl.observeMenuWithVariants()`: combines `MenuItemDao.observeAvailable()` + `VariantDao.observeGroupsForItem()` into `Flow<List<MenuItem>>`
- [ ] `OrderRepositoryImpl.confirmOrder(bill, items)`: `@Transaction` insert of bill + all items atomically
- [ ] `OrderRepositoryImpl.addItemsToExistingBill(billId, items)`: inserts new items only — existing items untouched (append-only)
- [ ] Zero direct RTDB calls in either class

## Testing
`./gradlew assembleDebug` → Hilt graph resolves for both bindings.

---

# TICKET-047: Repository Implementations — Bill and Payment
**Roadmap:** P3-005 (cont.) · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-042, TICKET-045, TICKET-033

## Affected Files
- `data/repository/BillRepositoryImpl.kt` — create
- `data/repository/PaymentRepositoryImpl.kt` — create
- `core/di/RepositoryModule.kt` — modify (add bindings)

## Acceptance Criteria
- [ ] `BillRepositoryImpl.observeOpenBills()` maps `BillDao.observeOpenBills()` + assembles `OrderItem` and `Payment` lists per bill
- [ ] `BillRepositoryImpl.voidBill()` calls `BillDao.voidBill()` then `notifyPendingSync()`
- [ ] `PaymentRepositoryImpl.processPayment(billId, payments)`: uses Room `@Transaction` to atomically insert all payment rows + call `BillDao.updateStatus(id, "PAID", paidAt, shiftId)` — if any step fails, nothing is committed
- [ ] `notifyPendingSync()` called after every write

## Testing
Manual: process payment → verify bill is PAID and payment rows exist atomically (no half-paid state possible).

---

# TICKET-048: Repository Implementations — Shift, Expense, Report, Auth
**Roadmap:** P3-005 (cont.) · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-043, TICKET-045, TICKET-033

## Affected Files
- `data/repository/ShiftRepositoryImpl.kt` — create
- `data/repository/ExpenseRepositoryImpl.kt` — create
- `data/repository/ReportRepositoryImpl.kt` — create
- `data/repository/StockRepositoryImpl.kt` — create (stub)
- `data/repository/AuthRepositoryImpl.kt` — create
- `core/di/RepositoryModule.kt` — modify (complete — all bindings now present)

## Acceptance Criteria
- [ ] `ShiftRepositoryImpl.openShift()`: inserts `ShiftEntity` with `syncStatus=PENDING`, calls `notifyPendingSync()`
- [ ] `ShiftRepositoryImpl.closeShift()`: updates shift status; inserts `ZReportEntity` via `ZReportDao.insert()`; **does not** set `syncStatus` on `ZReportEntity` (it has none)
- [ ] `ReportRepositoryImpl.getDailyDashboard()`: calls `ReportQueryDao` aggregate queries + assembles `DailyDashboard`
- [ ] `StockRepositoryImpl`: all methods throw `NotImplementedError("Phase 2")` — compiles cleanly
- [ ] `RepositoryModule` fully populated — all 8+ interface bindings present
- [ ] `./gradlew assembleDebug` → zero missing binding errors

## Testing
`./gradlew assembleDebug` → BUILD SUCCESSFUL, full Hilt graph resolved.

---

# TICKET-049: Auth Use Cases
**Roadmap:** P3-006 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-043, TICKET-028

## Affected Files
- `domain/usecase/auth/LoginUseCase.kt` — create
- `domain/usecase/auth/GetCurrentUserUseCase.kt` — create

## Acceptance Criteria
- [ ] `LoginUseCase`: `suspend operator fun invoke(email: String, password: String): Result<UserRole>` — calls `AuthRepository.signIn()`, on success calls `sessionManager.refreshRole()`, returns role
- [ ] Wrong credentials → `Result.failure(Exception)` with readable message; no crash
- [ ] `GetCurrentUserUseCase`: `operator fun invoke(): Flow<Boolean>` — delegates to `AuthRepository.observeAuthState()`
- [ ] Zero Android imports (only `@Inject constructor` is acceptable)

## Testing
Manual: `LoginUseCase` with wrong credentials → `Result.failure`. With correct → `Result.success(OWNER)`.

---

# TICKET-050: Order Use Cases — GetMenuItems and AddItemsToExistingBill
**Roadmap:** P3-007 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-042

## Affected Files
- `domain/usecase/order/GetMenuItemsUseCase.kt` — create
- `domain/usecase/order/AddItemsToExistingBillUseCase.kt` — create
- `domain/usecase/order/OrderDestination.kt` — create (sealed class)

## Acceptance Criteria
- [ ] `OrderDestination` sealed class: `object GrabAndGo`, `data class NewTableBill(tableId: String, billType: BillType)`, `data class ExistingBill(billId: String)`
- [ ] `GetMenuItemsUseCase`: `operator fun invoke(): Flow<List<MenuItem>>` — delegates to `MenuRepository.observeMenuWithVariants()`
- [ ] `AddItemsToExistingBillUseCase`: `suspend operator fun invoke(billId, cartItems, operatorId): Result<Unit>` — validates bill is OPEN and active shift exists; calls `OrderRepository.addItemsToExistingBill()`
- [ ] Adding to a PAID bill → `Result.failure(BillAlreadyPaidException)`

## Testing
Unit test: `AddItemsToExistingBillUseCase` with PAID bill → `Result.failure`.

---

# TICKET-051: Order Use Cases — ConfirmOrderUseCase
**Roadmap:** P3-007 (cont.) · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-042, TICKET-050

> Split from P3-007 due to complexity. This is the most business-logic-dense use case.

## Affected Files
- `domain/usecase/order/ConfirmOrderUseCase.kt` — create

## Acceptance Criteria
- [ ] `suspend operator fun invoke(cartItems: List<CartItem>, destination: OrderDestination, operatorId: String): Result<Unit>`
- [ ] Validates (in order): cart non-empty → `EmptyCartException`, active shift exists → `ShiftNotOpenException`, required variants fulfilled for each cart item → `MissingRequiredVariantException`
- [ ] For `ExistingBill` destination: validates bill is OPEN → `BillAlreadyPaidException`
- [ ] Price snapshot: `basePrice + sum(selectedOption.priceDelta)` captured at confirm time — immutable after
- [ ] Name snapshot: `menuItem.name` at confirm time — not affected by later menu edits
- [ ] `GrabAndGo` → bill with `tableId=null`, `type=UPFRONT`
- [ ] New bill gets `sessionLabel = "Bill #N"` (counter per shift — passed from ViewModel or tracked in repo)
- [ ] Calls `OrderRepository.confirmOrder(bill, items)` on success

## Testing
Unit tests in TICKET-090. All 7 failure/success paths covered.

---

# TICKET-052: Bill Use Cases
**Roadmap:** P3-008 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-042, TICKET-041

## Affected Files
- `domain/usecase/bill/GetOpenBillsUseCase.kt` — create
- `domain/usecase/bill/GetBillDetailUseCase.kt` — create
- `domain/usecase/bill/VoidBillUseCase.kt` — create
- `domain/usecase/bill/VoidOrderItemUseCase.kt` — create

## Acceptance Criteria
- [ ] `GetOpenBillsUseCase`: `operator fun invoke(): Flow<List<Bill>>` — delegates to `BillRepository.observeOpenBills()`
- [ ] `GetBillDetailUseCase`: `operator fun invoke(billId: String): Flow<Bill>` — delegates to `observeBillDetail(billId)`
- [ ] `VoidBillUseCase`: `suspend operator fun invoke(billId, reason, operatorId, userRole): Result<Unit>` — rejects `STAFF` with `InsufficientPermissionsException`; rejects already-VOID bill with `BillNotVoidableException`
- [ ] `VoidOrderItemUseCase`: `suspend operator fun invoke(itemId, reason, note, operatorId): Result<Unit>` — if `reason == OTHER` and `note.isNullOrBlank()` → `Result.failure(IllegalArgumentException)`

## Testing
Unit test: STAFF calls `VoidBillUseCase` → `InsufficientPermissionsException`.

---

# TICKET-053: Payment Use Cases
**Roadmap:** P3-009 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-042, TICKET-039

## Affected Files
- `domain/usecase/payment/CalculateChangeUseCase.kt` — create
- `domain/usecase/payment/ProcessPaymentUseCase.kt` — create

## Acceptance Criteria
- [ ] `CalculateChangeUseCase`: `operator fun invoke(tendered: Rupiah, total: Rupiah): Rupiah` — pure function, `return tendered - total`; no repo calls
- [ ] `ProcessPaymentUseCase`: `suspend operator fun invoke(billId: String, payments: List<Payment>): Result<Unit>` validates: bill OPEN → else `BillAlreadyPaidException`, active shift exists → else `ShiftNotOpenException`, sum of `payments.amount == bill.grandTotal` → else `InsufficientPaymentException`, for cash rows `amountTendered >= amount` → else `InsufficientTenderedAmountException`
- [ ] On pass: calls `PaymentRepository.processPayment()` (which uses Room `@Transaction`)

## Testing
Unit tests in TICKET-091.

---

# TICKET-054: Shift Use Cases — Open, CheckSoldOut, ResetSoldOut
**Roadmap:** P3-010 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-043, TICKET-042

## Affected Files
- `domain/usecase/shift/OpenShiftUseCase.kt` — create
- `domain/usecase/shift/CheckSoldOutItemsUseCase.kt` — create
- `domain/usecase/shift/ResetSoldOutItemsUseCase.kt` — create

## Acceptance Criteria
- [ ] `OpenShiftUseCase`: `suspend operator fun invoke(operatorName, openingFloat): Result<Unit>` — if `ShiftRepository.getOpenShift() != null` → `Result.failure(IllegalStateException("Shift already open"))`; else inserts new shift
- [ ] `CheckSoldOutItemsUseCase`: `suspend operator fun invoke(): Boolean` — returns `true` if any `MenuItem.isSoldOut == true`
- [ ] `ResetSoldOutItemsUseCase`: `suspend operator fun invoke(): Result<Unit>` — calls `MenuRepository.resetAllSoldOut()`
- [ ] PRD rule: after successful shift open, ViewModel calls `CheckSoldOutItemsUseCase` and shows reset prompt if `true`

## Testing
Unit test: `OpenShiftUseCase` when shift already open → `Result.failure`.

---

# TICKET-055: Shift Use Cases — CloseShift and GenerateZReport
**Roadmap:** P3-010 (cont.) · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-043, TICKET-041

> Split due to complexity. These are the most financially critical use cases.

## Affected Files
- `domain/usecase/shift/CloseShiftUseCase.kt` — create
- `domain/usecase/shift/GenerateZReportUseCase.kt` — create

## Acceptance Criteria
- [ ] `CloseShiftUseCase`: `suspend operator fun invoke(shiftId, countedCash, operatorId, userRole): Result<Unit>`:
  - `userRole != OWNER` → `InsufficientPermissionsException`
  - `ShiftRepository.countOpenBills() > 0` → `OpenBillsBlockShiftCloseException(openBills)`
  - Calculates: `expectedCash = openingFloat + Σ(cash payments in shift) - Σ(cash expenses in shift)`, `variance = countedCash - expectedCash`
  - Calls `GenerateZReportUseCase` then `ShiftRepository.closeShift()`
- [ ] `GenerateZReportUseCase`: `suspend operator fun invoke(shiftId): ZReport` — aggregates gross sales, payment breakdown by method, void summary, expenses by category, cash reconciliation fields

## Testing
Unit tests in TICKET-092.

---

# TICKET-056: Menu and Expense Use Cases
**Roadmap:** P3-011 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-042, TICKET-043

## Affected Files
- `domain/usecase/menu/UpsertMenuItemUseCase.kt` — create
- `domain/usecase/menu/UpsertMenuCategoryUseCase.kt` — create
- `domain/usecase/menu/DeleteMenuItemUseCase.kt` — create
- `domain/usecase/menu/ToggleSoldOutUseCase.kt` — create
- `domain/usecase/expense/LogExpenseUseCase.kt` — create

## Acceptance Criteria
- [ ] `UpsertMenuItemUseCase` + `UpsertMenuCategoryUseCase`: owner-only guard → `InsufficientPermissionsException` for STAFF
- [ ] `DeleteMenuItemUseCase`: owner-only; performs soft-delete only (`isAvailable=false`); warns if item in OPEN bills but does not block
- [ ] `ToggleSoldOutUseCase`: any role allowed; no validation
- [ ] `LogExpenseUseCase`: any role; validates `expense.amount > Rupiah.ZERO` → `IllegalArgumentException` on zero/negative

## Testing
Unit test: STAFF calls `UpsertMenuItemUseCase` → `InsufficientPermissionsException`. Zero amount expense → `Result.failure`.

---

# TICKET-057: Report Use Cases
**Roadmap:** P3-012 · **Scope:** MVP+P2 · **Est.:** ~1h · **Prereqs:** TICKET-043

## Affected Files
- `domain/usecase/report/GetDailyDashboardUseCase.kt` — create `[MVP]`
- `domain/usecase/report/GetDateRangeReportUseCase.kt` — create `[P2]`
- `domain/usecase/report/GetBestSellersUseCase.kt` — create `[P2]`
- `domain/usecase/report/ExportReportUseCase.kt` — create (stub `[P2]`)

## Acceptance Criteria
- [ ] `GetDailyDashboardUseCase`: `suspend operator fun invoke(dateEpoch: Long): DailyDashboard` — uses `DateUtil.startOfDay()` and `endOfDay()` for WIB boundaries; delegates to `ReportRepository.getDailyDashboard()`
- [ ] `GetDateRangeReportUseCase`: `suspend operator fun invoke(start, end): DateRangeReport`
- [ ] `GetBestSellersUseCase`: `operator fun invoke(start, end): Flow<List<BestSellerItem>>`
- [ ] `ExportReportUseCase`: stub that throws `NotImplementedError("P2")` — compiles but uncallable

## Testing
Unit test: `GetDailyDashboardUseCase` with known seeded Room data → correct `DailyDashboard` totals.

---

# TICKET-058: Login Screen
**Roadmap:** P4-001 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-049, TICKET-011

## Affected Files
- `feature/auth/LoginViewModel.kt` — create
- `feature/auth/LoginUiState.kt` — create
- `feature/auth/LoginUiEffect.kt` — create
- `feature/auth/LoginScreen.kt` — create
- `core/navigation/AppNavGraph.kt` — modify (wire login route + auto-navigate if already authenticated)
- `res/values/strings.xml` + `res/values-id/strings.xml` — add login strings

## Acceptance Criteria
- [ ] Email + password fields (keyboard type set correctly for each)
- [ ] Login button shows `CircularProgressIndicator` and is disabled while `isLoading = true`
- [ ] Wrong credentials → `Snackbar` in current language; app does not crash
- [ ] Correct credentials → navigates to Order (or Shift Open if no open shift)
- [ ] If already authenticated on launch → skip login screen entirely
- [ ] All strings in both languages

## Testing
Manual: wrong credentials → Snackbar; correct credentials → proceeds past login.

---

# TICKET-059: Shift Open Screen
**Roadmap:** P4-002 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-054, TICKET-058

## Affected Files
- `feature/shift/ShiftViewModel.kt` — create (handles open + close; extended in later tickets)
- `feature/shift/ShiftUiState.kt` — create
- `feature/shift/ShiftUiEffect.kt` — create
- `feature/shift/ShiftOpenScreen.kt` — create
- `core/navigation/AppNavGraph.kt` — modify (redirect to ShiftOpen when no active shift)
- `res/values/strings.xml` + `res/values-id/strings.xml` — add shift strings

## Acceptance Criteria
- [ ] Screen shown when `observeOpenShift()` emits `null`
- [ ] Operator name pre-filled from `SessionManager.currentUser.displayName`
- [ ] Opening float: numeric-only input; Rp prefix hint
- [ ] Cannot submit with zero or empty float
- [ ] After open: if sold-out items exist → `AlertDialog` "Reset semua item sold out? / Reset all sold-out items?" → Yes calls `ResetSoldOutItemsUseCase` then proceeds; No proceeds without reset
- [ ] On success: navigates to Order screen

## Testing
Manual: open app without active shift → shift open screen appears. Sold-out item present → dialog shown.

---

# TICKET-060: Order ViewModel and State
**Roadmap:** P4-003 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-051, TICKET-050, TICKET-039

> Split from P4-003 for manageability. ViewModel and state types only.

## Affected Files
- `feature/order/model/OrderUiState.kt` — create
- `feature/order/model/OrderUiEffect.kt` — create
- `feature/order/model/OrderUiEvent.kt` — create
- `feature/order/OrderViewModel.kt` — create

## Acceptance Criteria
- [ ] `OrderUiState`: `menuItems: List<MenuItemUi>`, `categories: List<MenuCategoryUi>`, `cart: List<CartItemUi>`, `cartTotal: String`, `activeShift: Boolean`, `selectedCategory: String?`, `isLoading: Boolean`
- [ ] `OrderUiEffect`: `ShowVariantSheet(item: MenuItem)`, `ShowDestinationSheet`, `NavigateToPayment(billId: String)`
- [ ] Private `_cart = MutableStateFlow<List<CartItem>>(emptyList())` — in-memory, never persisted
- [ ] `onAddItem(item)`: no variants → add directly; has variants → emit `ShowVariantSheet`
- [ ] `onAddToCart(item, selectedVariants)`: adds to `_cart` with price snapshot
- [ ] `onIncrement(cartItemId)`, `onDecrement(cartItemId)` (removes if qty=1)
- [ ] `onConfirmOrder(destination)`: calls `ConfirmOrderUseCase`; on success clears cart + emits appropriate effect
- [ ] Active shift observed from `ShiftRepository.observeOpenShift()`; `null` → `activeShift = false` in state

## Testing
Unit test: add same item twice → quantity = 2. Decrement to 0 → item removed from cart.

---

# TICKET-061: Order Screen Layout and Components
**Roadmap:** P4-003 (cont.) · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-060

## Affected Files
- `feature/order/OrderRoute.kt` — create (wires ViewModel → Screen + handles effects)
- `feature/order/OrderScreen.kt` — create (stateless composable)
- `feature/order/component/MenuItemGrid.kt` — create
- `feature/order/component/CategoryChipRow.kt` — create
- `feature/order/component/CartPanel.kt` — create
- `res/values/strings.xml` + `res/values-id/strings.xml` — add order screen strings

## Acceptance Criteria
- [ ] `OrderScreen` is stateless — all state/callbacks passed in as params
- [ ] `CategoryChipRow`: horizontal `LazyRow`, filtering grid by selected category
- [ ] `MenuItemGrid`: `LazyVerticalGrid`, 2 columns; item cards show name + `CurrencyFormatter.format(price)`
- [ ] Sold-out items: greyed overlay, `"Habis"/"Sold Out"` badge, `enabled = false`
- [ ] Cart quantity badge on each item card (hidden if 0)
- [ ] `CartPanel`: shows cart rows, qty +/- buttons, running subtotal, "Konfirmasi Pesanan / Confirm Order" button
- [ ] Confirm button disabled when `uiState.activeShift == false` or cart empty
- [ ] `collectAsStateWithLifecycle()` used — not `collectAsState()`

## Testing
Manual: add items → cart updates; sold-out item is non-tappable; category filter works.

---

# TICKET-062: Variant Selection Bottom Sheet
**Roadmap:** P4-004 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-060

## Affected Files
- `feature/order/component/VariantSelectionSheet.kt` — create
- `feature/order/OrderRoute.kt` — modify (handle `ShowVariantSheet` effect)

## Acceptance Criteria
- [ ] `ModalBottomSheet` driven by UiState boolean flag (not imperative `show()`)
- [ ] `SINGLE` groups use `RadioButton` — only one selectable
- [ ] `MULTIPLE` groups use `Checkbox` — multiple selectable
- [ ] Required groups marked with `*`
- [ ] "Tambah / Add" button disabled until all required groups have a selection
- [ ] Price updates in real time: `"Nasi Goreng — Rp 15.000 (+Rp 3.000)"`
- [ ] On confirm: calls `onAddToCart(item, selectedVariants)` in ViewModel; sheet closes

## Testing
Manual: item with required variant → sheet opens → cannot add until variant selected.

---

# TICKET-063: Order Destination Bottom Sheet
**Roadmap:** P4-005 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-060, TICKET-050

## Affected Files
- `feature/order/component/OrderDestinationSheet.kt` — create
- `feature/order/OrderRoute.kt` — modify (handle `ShowDestinationSheet` effect)

## Acceptance Criteria
- [ ] `ModalBottomSheet` with 3 sections: Grab & Go, New Table, Add to Existing Bill
- [ ] **Grab & Go**: tap → `onConfirmOrder(GrabAndGo)` → navigates to PaymentScreen
- [ ] **New Table**: table selector (active tables only) + UPFRONT/OPEN_BILL radio; Confirm disabled until table selected; calls `onConfirmOrder(NewTableBill(...))`
- [ ] **Existing Bill**: shows list of open bills with table label + running total; tap → `onConfirmOrder(ExistingBill(...))`
- [ ] Non-grab-and-go success: cart clears, `Snackbar("Pesanan dikirim / Order sent")`, stays on Order screen

## Testing
Manual: all 3 destination paths work end-to-end; cart clears after confirm.

---

# TICKET-064: Tables and Bills Overview Screen
**Roadmap:** P4-006 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-052, TICKET-011

## Affected Files
- `feature/tables/TablesViewModel.kt` — create
- `feature/tables/TablesUiState.kt` — create
- `feature/tables/TablesScreen.kt` — create
- `feature/tables/component/TableCard.kt` — create

## Acceptance Criteria
- [ ] Bills grouped by `tableId`; bills with `tableId=null` in `"Bawa Pulang / Grab & Go"` section
- [ ] Each `TableCard`: table label, count of open bills, total owed (sum of `grandTotal`)
- [ ] Bills open > 12 hours show amber clock indicator
- [ ] Tapping a bill → navigates to `BillDetailScreen`
- [ ] Screen updates in real time via Flow — new bills from Device 2 appear automatically
- [ ] Empty state: `"Belum ada tagihan aktif / No active bills"`

## Testing
Manual: create bill → appears in tables screen. Pay bill → disappears.

---

# TICKET-065: Bill Detail Screen
**Roadmap:** P4-007 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-052, TICKET-064

## Affected Files
- `feature/tables/BillDetailViewModel.kt` — create
- `feature/tables/BillDetailUiState.kt` — create
- `feature/tables/BillDetailScreen.kt` — create

## Acceptance Criteria
- [ ] Shows bill header: table label, session label, type, status, created at
- [ ] Order items list: void items shown struck-through and excluded from total
- [ ] Payments received list (for split payment tracking)
- [ ] Running total (excludes voided items)
- [ ] "Tambah Item / Add Items" button (OPEN only) → navigates to Order screen with bill pre-selected
- [ ] "Bayar / Pay" button (OPEN only) → navigates to PaymentScreen with `billId`
- [ ] "Void Bill" option visible only for `OWNER` role

## Testing
Manual: void an item → struck-through in UI, total recalculates.

---

# TICKET-066: Void Order Item Dialog
**Roadmap:** P4-008 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-052, TICKET-065

## Affected Files
- `feature/tables/component/VoidOrderItemDialog.kt` — create
- `feature/tables/BillDetailScreen.kt` — modify (trigger via long-press or swipe-to-reveal)

## Acceptance Criteria
- [ ] `AlertDialog` showing: item name, reason `RadioGroup` with 5 options (Customer Change, Kitchen Error, Item Unavailable, Test, Other) in both languages
- [ ] Text field for note visible only when "Other / Lainnya" is selected
- [ ] "Void Item / Batalkan Item" button disabled until reason selected; if "Other" then note must be non-blank
- [ ] On confirm: calls `VoidOrderItemUseCase`; dialog closes; item shows struck-through in bill

## Testing
Manual: select "Other" with blank note → button disabled; fill note → enabled; confirm → item struck-through.

---

# TICKET-067: Void Entire Bill Dialog
**Roadmap:** P4-009 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-052, TICKET-065

## Affected Files
- `feature/tables/component/VoidBillDialog.kt` — create
- `feature/tables/BillDetailScreen.kt` — modify (show "Void Bill" option only to OWNER)

## Acceptance Criteria
- [ ] "Void Bill" option hidden entirely for `STAFF` role (not just disabled — not rendered)
- [ ] `AlertDialog` with warning message + reason picker (same 5 reasons)
- [ ] On confirm: calls `VoidBillUseCase`; dialog closes; bill disappears from open bills list

## Testing
Manual (as staff): "Void Bill" option must not appear. Manual (as owner): void succeeds, bill removed from list.

---

# TICKET-068: Payment ViewModel and State
**Roadmap:** P4-010 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-053, TICKET-039

> Split from P4-010 for manageability.

## Affected Files
- `feature/payment/model/PaymentUiState.kt` — create
- `feature/payment/model/PaymentUiEffect.kt` — create
- `feature/payment/PaymentViewModel.kt` — create

## Acceptance Criteria
- [ ] `PaymentUiState`: `bill: BillDetailUi?`, `enabledMethods: List<PaymentMethodUi>`, `paymentRows: List<PaymentRowUi>`, `remainingBalance: String` (formatted), `canComplete: Boolean`, `isSubmitting: Boolean`
- [ ] `PaymentRowUi`: `method: PaymentMethodUi`, `amount: Rupiah`, `amountTendered: Rupiah?`, `change: Rupiah?`
- [ ] `onAddPaymentRow(method)`: appends row; `remainingBalance` decrements
- [ ] `onSetCashTendered(rowId, amount)`: updates row; `change = tendered - rowAmount`; row invalid if tendered < amount
- [ ] `canComplete = remainingBalance == Rupiah.ZERO && all rows valid`
- [ ] `onCompletePayment()`: calls `ProcessPaymentUseCase`; on success emits navigation effect

## Testing
Unit test: add payment row for full amount → `canComplete = true`. Under-tender cash → `canComplete = false`.

---

# TICKET-069: Payment Screen
**Roadmap:** P4-010 (cont.) · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-068

## Affected Files
- `feature/payment/PaymentRoute.kt` — create
- `feature/payment/PaymentScreen.kt` — create
- `feature/payment/component/PaymentMethodSelector.kt` — create
- `feature/payment/component/PaymentRowCard.kt` — create
- `res/values/strings.xml` + `res/values-id/strings.xml` — add payment strings

## Acceptance Criteria
- [ ] Bill summary shown at top: items list, `grandTotal` in large text
- [ ] `PaymentMethodSelector`: horizontal scrollable chips; only enabled methods shown
- [ ] Each `PaymentRowCard` for cash: amount input + tendered input + change display (auto-calculated)
- [ ] Each `PaymentRowCard` for non-cash: amount shown, "Diterima / Received" confirmation toggle
- [ ] Remaining balance shown prominently; turns green when = `Rp 0`
- [ ] "Bayar / Pay" button enabled only when `canComplete = true`
- [ ] On success: Snackbar + navigate to Tables screen (or Order for grab-and-go)

## Testing
Manual: full grab-and-go order → payment → success → cart cleared.

---

# TICKET-070: Shift Close Screen
**Roadmap:** P4-011 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-055, TICKET-059

## Affected Files
- `feature/shift/ShiftCloseScreen.kt` — create
- `feature/shift/ShiftViewModel.kt` — modify (add close shift state)
- `feature/shift/ShiftCloseUiState.kt` — create

## Acceptance Criteria
- [ ] Owner-only route (role guard at NavGraph level — staff cannot reach this route)
- [ ] If open bills exist: shows blocking list with table label + session label + total per bill; "Tutup Shift" disabled
- [ ] If no open bills: shows counted cash input + expected cash (auto-calculated) + variance with color coding:
  - Variance `== 0` → green
  - `|variance| ≤ Rp 5.000` → amber
  - `|variance| > Rp 5.000` → red
- [ ] "Tutup Shift / Close Shift" calls `CloseShiftUseCase`; on success navigates to Z-Report screen
- [ ] Screen live-updates (if an open bill is paid on Device 2, blocking list shrinks)

## Testing
Manual: create open bill → shift close blocked. Pay bill → close now succeeds.

---

# TICKET-071: Z-Report Screen and Shift History Screen
**Roadmap:** P4-012 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-055, TICKET-070

## Affected Files
- `feature/shift/ZReportScreen.kt` — create
- `feature/shift/ShiftHistoryScreen.kt` — create

## Acceptance Criteria
- [ ] `ZReportScreen`: read-only, shows all `ZReport` fields with clear section labels (sales, payment breakdown, void summary, expenses, cash reconciliation)
- [ ] Cash variance shown with same color coding as shift close screen
- [ ] `ShiftHistoryScreen`: list of closed shifts sorted newest-first; each row shows date, opened by, gross sales; tap → `ZReportScreen` for that shift
- [ ] Both accessible from More navigation

## Testing
Manual: close a shift → navigate to Z-Report → all sections present with correct totals.

---

# TICKET-072: Expense Log Screen
**Roadmap:** P4-013 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-056, TICKET-011

## Affected Files
- `feature/expense/ExpenseViewModel.kt` — create
- `feature/expense/ExpenseUiState.kt` — create
- `feature/expense/ExpenseLogScreen.kt` — create

## Acceptance Criteria
- [ ] Accessible to all roles (no owner guard)
- [ ] FAB opens inline form or dialog: category picker, amount field (numeric, Rp prefix), optional note
- [ ] Category picker shows all `ExpenseCategory` values in current language
- [ ] Cannot submit with zero or negative amount
- [ ] Expense list for current shift shown below; updates reactively
- [ ] Expense totals feed into daily dashboard

## Testing
Manual: log expense → appears in list. Verify total in daily dashboard increases.

---

# TICKET-073: Menu Management Screen
**Roadmap:** P4-014 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-056, TICKET-011

## Affected Files
- `feature/menu/MenuViewModel.kt` — create
- `feature/menu/MenuUiState.kt` — create
- `feature/menu/MenuManagementScreen.kt` — create

## Acceptance Criteria
- [ ] Owner-only route
- [ ] Items grouped by category with category header row (category name + "Add Item" + "Edit Category" buttons)
- [ ] Each item row: name, price, availability chip, sold-out toggle (inline, no navigation)
- [ ] "Hide" option per item → soft-delete; item disappears from order screen immediately
- [ ] If item appears in OPEN bills: show `AlertDialog` warning before hiding (warn, don't block)
- [ ] FAB → `MenuItemEditScreen` (new item)
- [ ] Tapping item → `MenuItemEditScreen` (edit)

## Testing
Manual: toggle sold-out on item → greyed on order screen. Hide item → disappears from order screen.

---

# TICKET-074: Menu Item Edit Screen + Variant Management
**Roadmap:** P4-015 · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-073, TICKET-056

## Affected Files
- `feature/menu/MenuItemEditScreen.kt` — create
- `feature/menu/component/VariantGroupEditor.kt` — create

## Acceptance Criteria
- [ ] Fields: name (text), category (dropdown from active categories), base price (numeric, formatted as Rp)
- [ ] Validates: name non-blank, price > 0, category selected
- [ ] Variant section: shows existing groups; "Add Group" button
- [ ] Per group: name, SINGLE/MULTIPLE toggle, required toggle, "Delete Group" button
- [ ] Per option: name, price delta (`+Rp X`, `-Rp X`, or `"Gratis/Free"`), "Delete Option" button
- [ ] Deleting a group removes all its options (cascaded in Room by FK)
- [ ] Save calls `UpsertMenuItemUseCase` + upserts all groups/options; navigates back on success

## Testing
Manual: create item with 2 variant groups → appears in order screen variant sheet.

---

# TICKET-075: Daily Dashboard Screen
**Roadmap:** P4-016 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-057, TICKET-011

## Affected Files
- `feature/reports/DashboardViewModel.kt` — create
- `feature/reports/DashboardUiState.kt` — create
- `feature/reports/DashboardScreen.kt` — create

## Acceptance Criteria
- [ ] Owner-only route (role guard)
- [ ] Always shows today's WIB data — no date picker
- [ ] Displays: gross sales (large Rp text), transaction count, payment method breakdown (list), top 5 items by quantity (ranked list), cash variance
- [ ] Updates in real time via Flow
- [ ] Empty state: `"Belum ada penjualan hari ini / No sales today"`

## Testing
Manual: complete a transaction → dashboard totals update in real time.

---

# TICKET-076: Settings Root and Language Settings
**Roadmap:** P4-017 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-008, TICKET-011

## Affected Files
- `feature/settings/SettingsScreen.kt` — create (navigation hub list)
- `feature/settings/LanguageSettingsScreen.kt` — create
- `feature/settings/SettingsViewModel.kt` — create
- `core/navigation/AppNavGraph.kt` — modify (add settings sub-routes)

## Acceptance Criteria
- [ ] `SettingsScreen`: list of destinations — Tables, Payment Methods, Expense Categories, Language, About
- [ ] `LanguageSettingsScreen`: two radio options (Bahasa Indonesia, English); current language pre-selected
- [ ] Language switch calls `AppPreferences.setLanguage()` + immediately re-applies `LocalConfiguration` override in `WarungPosApp.kt`
- [ ] No app restart required — all strings switch in current session

## Testing
Manual: switch from ID to EN → all app strings change immediately without restart.

---

# TICKET-077: Settings — Table and Payment Method Management
**Roadmap:** P4-017 (cont.) · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-076, TICKET-056

## Affected Files
- `feature/settings/TableSettingsScreen.kt` — create
- `feature/settings/PaymentMethodSettingsScreen.kt` — create

## Acceptance Criteria
- [ ] `TableSettingsScreen`: list of all tables; FAB to add; each row: label (inline editable or dialog), active toggle; deactivated tables greyed and hidden from order flow
- [ ] `PaymentMethodSettingsScreen`: list of payment methods; per row: `Switch` for enable/disable, drag handle for reorder; reorder updates `sortOrder` in Room and RTDB
- [ ] Deactivating a payment method immediately removes it from PaymentScreen in same session

## Testing
Manual: disable QRIS → not available on payment screen. Reorder methods → new order reflected on payment screen.

---

# TICKET-078: Settings — Expense Categories and About Screen
**Roadmap:** P4-017 (cont.) · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-076

## Affected Files
- `feature/settings/ExpenseCategorySettingsScreen.kt` — create (read-only list for MVP)
- `feature/settings/AboutScreen.kt` — create

## Acceptance Criteria
- [ ] `ExpenseCategorySettingsScreen`: shows current expense category names in both languages; editing deferred to P2
- [ ] `AboutScreen`: shows `BuildConfig.VERSION_NAME`, `BuildConfig.VERSION_CODE`
- [ ] Both screens reachable from Settings hub

## Testing
Manual: navigate to About → version numbers visible and correct.

---

# TICKET-079: Sync Status Bar
**Roadmap:** P4-018 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-033, TICKET-011

## Affected Files
- `feature/sync/SyncStatusBar.kt` — create
- `feature/sync/SyncViewModel.kt` — create (application-scoped or hoisted to `WarungPosApp`)
- `WarungPosApp.kt` — modify (embed `SyncStatusBar` above `Scaffold` content)
- `res/values/strings.xml` + `res/values-id/strings.xml` — add sync status strings

## Acceptance Criteria
- [ ] Observes `SyncCoordinator.syncState` and `pendingCount`
- [ ] Hidden when online + `syncState = IDLE`
- [ ] Amber bar: `"Menyinkronkan N data... / Syncing N items..."` when `SYNCING`
- [ ] Red bar: `"Offline — data tersimpan lokal / Offline — data saved locally"` when offline
- [ ] Does not overlap `TopAppBar` or `BottomNavigationBar`
- [ ] Correct strings in both languages

## Testing
Manual: toggle airplane mode → red bar appears; reconnect → syncs → bar hides.

---

# TICKET-080: Version Gate Screen (Complete)
**Roadmap:** P4-019 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-013

> TICKET-013 created a stub. This ticket completes the full screen.

## Affected Files
- `feature/auth/UpdateRequiredScreen.kt` — modify (add full UI content)
- `res/values/strings.xml` + `res/values-id/strings.xml` — add update screen strings

## Acceptance Criteria
- [ ] Full-screen, non-dismissable; back button intercepted (no-op)
- [ ] Shows: app icon (or logo placeholder), `"Versi baru tersedia / Update available"`
- [ ] Current `BuildConfig.VERSION_NAME` displayed
- [ ] Message: `"Silakan minta file APK terbaru ke pemilik toko / Please ask the store owner for the latest APK"`
- [ ] No action button (manual update only)
- [ ] Both languages present

## Testing
Set RTDB `minVersionCode: 999` → gate screen shows full UI; back does nothing.

---

# TICKET-081: Stock Screen Stubs
**Roadmap:** P4-020 · **Scope:** P2 · **Est.:** ~30m · **Prereqs:** TICKET-011

## Affected Files
- `feature/stock/StockScreen.kt` — create (placeholder)
- `feature/stock/StockBatchScreen.kt` — create (placeholder)
- `feature/stock/OpnameScreen.kt` — create (placeholder)
- `feature/stock/StockViewModel.kt` — create (empty)
- `core/navigation/AppNavGraph.kt` — modify (add stock routes under More nav)

## Acceptance Criteria
- [ ] All 3 screens compile and are reachable from More navigation
- [ ] Each shows: centered icon + `"Segera Hadir / Coming Soon"` text
- [ ] No crash when navigating to any stock screen

## Testing
Manual: navigate to each stock screen → no crash; placeholder shown.

---

# TICKET-082: Full Reports Screen
**Roadmap:** P4-021 · **Scope:** P2 · **Est.:** ~2h · **Prereqs:** TICKET-075, TICKET-057

## Affected Files
- `feature/reports/ReportScreen.kt` — create
- `feature/reports/BestSellerScreen.kt` — create
- `feature/reports/ReportViewModel.kt` — modify (add date range state)
- `res/values/strings.xml` + `res/values-id/strings.xml` — add report strings

## Acceptance Criteria
- [ ] Date range picker: Day / Week / Month / Custom tabs
- [ ] Shows: revenue, expenses, gross profit, payment method breakdown, void summary, expense breakdown by category
- [ ] "Bagikan / Share" button calls `ExportReportUseCase`; shares CSV via Android share sheet
- [ ] `BestSellerScreen`: ranked list with quantity + revenue per item
- [ ] Owner-only route
- [ ] All monetary values Rp-formatted

## Testing
Manual: generate report for date range with known transactions; verify totals match Z-reports.
---

# TICKET-083: Phase 4 Gate — Full Service Day Simulation
**Roadmap:** P4 gate · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-069, TICKET-070, TICKET-071

> Verification ticket — no new code. Run the full simulation before starting Phase 5.

## Affected Files
None.

## Acceptance Criteria
- [ ] Log in as owner on a real device (not emulator)
- [ ] Open shift with Rp 100.000 float
- [ ] Take 1 grab-and-go order (1 item with variants); pay exact cash
- [ ] Open dine-in UPFRONT bill on Table 1; add items; pay with QRIS
- [ ] Open dine-in OPEN_BILL on Table 2; add items in 2 rounds; pay split cash+QRIS
- [ ] Void 1 order item with "Kitchen Error" reason
- [ ] Log 1 expense (Gas, Rp 30.000)
- [ ] Close shift — verify Z-report totals match manual calculation
- [ ] Check RTDB console — all bills, payments, expenses, order items present
- [ ] Both EN and ID language verified for at least 3 screens
- [ ] Zero crashes throughout

## Testing
Run the simulation. Each checklist item must be checked off before proceeding to Phase 5.

---

# TICKET-084: Unit Tests — ConfirmOrderUseCase
**Roadmap:** P5-001 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-051

## Affected Files
- `test/.../usecase/order/ConfirmOrderUseCaseTest.kt` — create
- `test/.../fake/FakeOrderRepository.kt` — create
- `test/.../fake/FakeShiftRepository.kt` — create
- `test/.../fake/FakeMenuRepository.kt` — create

## Acceptance Criteria
- [ ] Test: valid cart + active shift + new table → `Result.success`
- [ ] Test: empty cart → `Result.failure(EmptyCartException)`
- [ ] Test: no active shift → `Result.failure(ShiftNotOpenException)`
- [ ] Test: required variant group unfulfilled → `Result.failure(MissingRequiredVariantException)`
- [ ] Test: grab-and-go destination → bill has `tableId = null`, `type = UPFRONT`
- [ ] Test: `ExistingBill` destination with PAID bill → `Result.failure(BillAlreadyPaidException)`
- [ ] Test: price snapshot = `basePrice + sum(selectedOption.priceDelta)` — not recalculated later
- [ ] All tests run on JVM (no emulator)
- [ ] Fake repositories used — no MockK for repository layer

## Testing
`./gradlew test` → all 7 test cases pass.

---

# TICKET-085: Unit Tests — ProcessPaymentUseCase
**Roadmap:** P5-002 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-053

## Affected Files
- `test/.../usecase/payment/ProcessPaymentUseCaseTest.kt` — create
- `test/.../fake/FakePaymentRepository.kt` — create (if not already created)

## Acceptance Criteria
- [ ] Test: exact cash payment → `Result.success`, bill marked PAID
- [ ] Test: overpaid cash → `Result.success`, `changeGiven = tendered - amount`
- [ ] Test: cash tendered < amount → `Result.failure(InsufficientTenderedAmountException)`
- [ ] Test: split cash + QRIS summing to total → `Result.success`
- [ ] Test: split total < grandTotal → `Result.failure(InsufficientPaymentException)`
- [ ] Test: bill already PAID → `Result.failure(BillAlreadyPaidException)`
- [ ] Test: no active shift → `Result.failure(ShiftNotOpenException)`

## Testing
`./gradlew test` → all 7 cases pass.

---

# TICKET-086: Unit Tests — CalculateChangeUseCase
**Roadmap:** P5-005 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-053

## Affected Files
- `test/.../usecase/payment/CalculateChangeUseCaseTest.kt` — create

## Acceptance Criteria
- [ ] Test: `tendered(50000) > total(45000)` → `Rupiah(5000)`
- [ ] Test: `tendered(45000) == total(45000)` → `Rupiah.ZERO`
- [ ] Test: `tendered(40000) < total(45000)` → `Rupiah(-5000)` (negative = insufficient)
- [ ] Test: `tendered(0)` → negative Rupiah equal to `-total`
- [ ] Pure function — no mocking, no fakes

## Testing
`./gradlew test` → all 4 cases pass.

---

# TICKET-087: Unit Tests — CloseShiftUseCase
**Roadmap:** P5-003 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-055

## Affected Files
- `test/.../usecase/shift/CloseShiftUseCaseTest.kt` — create
- `test/.../fake/FakeShiftRepository.kt` — modify (add close shift support if needed)

## Acceptance Criteria
- [ ] Test: STAFF role → `Result.failure(InsufficientPermissionsException)`
- [ ] Test: open bills exist → `Result.failure(OpenBillsBlockShiftCloseException)` with correct bill list
- [ ] Test: no open bills + OWNER → `Result.success`, Z-report generated
- [ ] Test: `expectedCash` formula with known values: `openingFloat(100000) + cashPayments(75000) - cashExpenses(20000) = 155000`
- [ ] Test: `variance = countedCash - expectedCash`: `counted(150000) - expected(155000) = -5000`

## Testing
`./gradlew test` → all 5 cases pass.

---

# TICKET-088: Unit Tests — VoidBillUseCase and VoidOrderItemUseCase
**Roadmap:** P5-004 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-052

## Affected Files
- `test/.../usecase/bill/VoidBillUseCaseTest.kt` — create
- `test/.../usecase/bill/VoidOrderItemUseCaseTest.kt` — create

## Acceptance Criteria
- [ ] Test: STAFF role calls `VoidBillUseCase` → `InsufficientPermissionsException`
- [ ] Test: OWNER voids OPEN bill → `Result.success`
- [ ] Test: void already-VOID bill → `Result.failure(BillNotVoidableException)`
- [ ] Test: `VoidOrderItemUseCase` with `VoidReason.OTHER` + blank note → `Result.failure(IllegalArgumentException)`
- [ ] Test: `VoidReason.OTHER` + valid note → `Result.success`
- [ ] Test: `VoidReason.KITCHEN_ERROR` (non-OTHER) + no note → `Result.success`

## Testing
`./gradlew test` → all 6 cases pass.

---

# TICKET-089: Unit Tests — ConflictResolver
**Roadmap:** P5-006 · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-030

## Affected Files
- `test/.../sync/ConflictResolverTest.kt` — create

## Acceptance Criteria
- [ ] Test: `incomingUpdatedAt(1000) > existing(500)` → `ACCEPT`
- [ ] Test: `incomingUpdatedAt(500) < existing(1000)` → `REJECT`
- [ ] Test: `incomingUpdatedAt == existingUpdatedAt` → `REJECT` (local wins on tie)
- [ ] Test: `existingUpdatedAt == null` (new entity) → `ACCEPT`
- [ ] Test: `existingStatus = "PAID"`, `incomingStatus = "OPEN"` → `REJECT` regardless of timestamp
- [ ] Test: `existingStatus = "VOID"`, `incomingStatus = "OPEN"` → `REJECT`
- [ ] Test: `existingStatus = "OPEN"`, `incomingStatus = "PAID"`, newer timestamp → `ACCEPT`

## Testing
`./gradlew test` → all 7 cases pass.

---

# TICKET-090: Unit Tests — ConfirmOrderUseCase Price Snapshot
**Roadmap:** P5-001 (supplemental) · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-084

> Supplemental test focusing on snapshot immutability — a correctness property critical for financial integrity.

## Affected Files
- `test/.../usecase/order/ConfirmOrderUseCaseTest.kt` — modify (add snapshot tests)

## Acceptance Criteria
- [ ] Test: item with 2 variant options (priceDelta +3000, +2000) → `lineTotal = basePrice + 3000 + 2000`
- [ ] Test: change menu item `basePrice` in fake repo after confirm → `orderItem.priceSnapshot` unchanged
- [ ] Test: change option `priceDelta` in fake repo after confirm → `orderItem.selectedVariants` price unchanged
- [ ] Test: `nameSnapshot = item.name` at confirm time; rename item in fake repo → snapshot unchanged

## Testing
`./gradlew test` → all 4 snapshot tests pass.

---

# TICKET-091: Unit Tests — Shift Use Cases Integration
**Roadmap:** P5-003 (supplemental) · **Scope:** MVP · **Est.:** ~45m · **Prereqs:** TICKET-087

## Affected Files
- `test/.../usecase/shift/OpenShiftUseCaseTest.kt` — create

## Acceptance Criteria
- [ ] Test: open shift when no existing shift → `Result.success`
- [ ] Test: open shift when shift already OPEN → `Result.failure(IllegalStateException)`
- [ ] Test: `CheckSoldOutItemsUseCase` returns `true` when one item has `isSoldOut = true`
- [ ] Test: `CheckSoldOutItemsUseCase` returns `false` when zero sold-out items
- [ ] Test: `ResetSoldOutItemsUseCase` → `resetAllSoldOut()` called on menu repository

## Testing
`./gradlew test` → all 5 cases pass.

---

# TICKET-092: Unit Tests — Mappers Round-Trip
**Roadmap:** P5-007 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-044, TICKET-045

## Affected Files
- `test/.../mapper/MenuMapperTest.kt` — create
- `test/.../mapper/OrderItemMapperTest.kt` — create
- `test/.../mapper/BillMapperTest.kt` — create
- `test/.../mapper/ZReportMapperTest.kt` — create
- `test/.../mapper/ExpenseMapperTest.kt` — create

## Acceptance Criteria
- [ ] Each mapper has round-trip test: `entity → domain → entity` produces equal entity (all fields preserved; no silent drops)
- [ ] `OrderItemMapperTest`: `selectedVariantsJson` with 2 variant groups → `decodeFromString` → all fields correct
- [ ] `ZReportMapperTest`: complex `ZReport` with all fields → `toSnapshotJson()` → `toDomain()` → equal to original
- [ ] Monetary round-trip: `Long(15000L) → Rupiah(15000L) → Long(15000L)` verified in every monetary mapper
- [ ] Tests run on JVM

## Testing
`./gradlew test` → all mapper test files pass.

---

# TICKET-093: Integration Tests — BillDao
**Roadmap:** P5-008 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-023, TICKET-021

## Affected Files
- `androidTest/.../dao/BillDaoTest.kt` — create

## Acceptance Criteria
- [ ] Setup: `Room.inMemoryDatabaseBuilder(context, WarungDatabase::class.java).allowMainThreadQueries().build()`
- [ ] Test: insert OPEN bill → `observeOpenBills()` emits it
- [ ] Test: update bill to PAID → `observeOpenBills()` no longer includes it
- [ ] Test: two OPEN bills on same `tableId` → `observeOpenBillsForTable(tableId)` returns both
- [ ] Test: `countOpenBills()` returns 0 when table empty; 2 after two inserts
- [ ] Test: `Flow` emits a new value after each `insert` (Room invalidation working)
- [ ] Test: `voidBill()` sets `status = "VOID"` but `getById(id)` still returns the bill

## Testing
`./gradlew connectedAndroidTest` → all cases pass on emulator.

---

# TICKET-094: Integration Tests — ReportQueryDao
**Roadmap:** P5-009 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-027

## Affected Files
- `androidTest/.../dao/ReportQueryDaoTest.kt` — create

## Acceptance Criteria
- [ ] Test: 3 PAID bills (totals: 10000, 20000, 30000) in range → `totalSalesForDateRange` = 60000
- [ ] Test: 1 bill outside date range → excluded from sum (total = 50000)
- [ ] Test: 2 order items for item A (qty 3 each), 1 for item B (qty 5) → best seller = item B
- [ ] Test: voided order items excluded from best seller quantity
- [ ] Test: `salesByPaymentMethod` with 2 cash payments (15000, 20000) + 1 QRIS (30000) → 3 rows with correct totals

## Testing
`./gradlew connectedAndroidTest` → all cases pass.

---

# TICKET-095: Integration Tests — Payment Transaction Atomicity
**Roadmap:** P5-010 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-047

## Affected Files
- `androidTest/.../repository/PaymentRepositoryImplTest.kt` — create

## Acceptance Criteria
- [ ] Test: successful `processPayment()` → bill is PAID and all payment rows exist atomically
- [ ] Test: double `processPayment()` call on same bill → second fails (`BillAlreadyPaidException`), first payment rows preserved
- [ ] Test: simulated Room failure mid-payment (override DAO to throw on second call) → bill remains OPEN, zero payment rows inserted (atomicity verified)

## Testing
`./gradlew connectedAndroidTest` → all 3 tests pass.

---

# TICKET-096: UI Test — Grab-and-Go Order + Cash Payment
**Roadmap:** P5-011 · **Scope:** MVP · **Est.:** ~2h · **Prereqs:** TICKET-061, TICKET-063, TICKET-069

## Affected Files
- `androidTest/.../flow/GrabAndGoFlowTest.kt` — create

## Acceptance Criteria
- [ ] Test uses `@HiltAndroidTest` with in-memory Room DB + fake/seeded menu items
- [ ] Step 1: order screen loads → menu item visible → tap → cart shows qty 1
- [ ] Step 2: tap "Confirm Order" → select "Grab & Go" → payment screen opens
- [ ] Step 3: payment screen shows correct `grandTotal`
- [ ] Step 4: select Cash → enter tendered amount (overpay) → change shown correctly
- [ ] Step 5: tap Pay → navigate to Order screen; cart is empty
- [ ] Verify: bill in Room has `status = "PAID"`
- [ ] Test is repeatable (no shared state leakage)

## Testing
`./gradlew connectedAndroidTest` → test passes consistently (run 3x to verify no flakiness).

---

# TICKET-097: UI Test — Shift Close Blocked by Open Bill
**Roadmap:** P5-012 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-070, TICKET-069

## Affected Files
- `androidTest/.../flow/ShiftCloseBlockedFlowTest.kt` — create

## Acceptance Criteria
- [ ] Preconditions: seeded shift + 1 open bill in Room
- [ ] Step 1: navigate to Shift Close → blocked state shows; bill listed by session label
- [ ] Step 2: navigate to payment → pay the open bill
- [ ] Step 3: navigate back to Shift Close → `canClose = true`; counted cash input visible
- [ ] Step 4: enter counted cash → tap Close → Z-Report screen shown
- [ ] Verify: shift in Room has `status = "CLOSED"`
- [ ] Test is repeatable

## Testing
`./gradlew connectedAndroidTest` → test passes consistently.

---

# TICKET-098: Phase 5 Gate Verification
**Roadmap:** P5 gate · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-084 through TICKET-097

> Verification ticket — no new code.

## Affected Files
None.

## Acceptance Criteria
- [ ] `./gradlew test` passes — all use case + mapper + utility unit tests green
- [ ] `./gradlew connectedAndroidTest` passes — all DAO + repository + UI flow tests green
- [ ] Zero compilation warnings in `test/` and `androidTest/` source sets
- [ ] `ConfirmOrderUseCase`, `ProcessPaymentUseCase`, `CloseShiftUseCase` each have ≥ 5 unit test cases

## Testing
Run both Gradle tasks and verify all pass before starting Phase 6.

---

# TICKET-099: Release Signing Configuration
**Roadmap:** P6-001 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-001

## Affected Files
- `app/build.gradle.kts` — modify (`signingConfigs.release`)
- `local.properties` — modify (add keystore path + credentials — NOT committed to git)
- `.gitignore` — verify excludes `*.jks`, `*.keystore`, `local.properties`

## Acceptance Criteria
- [ ] `signingConfigs.release` reads from `local.properties` keys (not hardcoded values)
- [ ] `buildTypes.release.signingConfig = signingConfigs.getByName("release")`
- [ ] `.gitignore` confirmed to exclude keystore file and `local.properties`
- [ ] `./gradlew assembleRelease` produces a signed APK in `app/build/outputs/apk/release/`
- [ ] APK `debuggable = false` in release build type

## Testing
`./gradlew assembleRelease` → BUILD SUCCESSFUL. `apksigner verify app-release.apk` → shows signed.

---

# TICKET-100: Release Build Verification
**Roadmap:** P6-002 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-012, TICKET-099

## Affected Files
None (verification only — any R8 issues fixed in `proguard-rules.pro`).

## Acceptance Criteria
- [ ] Release APK installs on a real Android device (not emulator — R8 behavior differs)
- [ ] App launches to login screen without crash
- [ ] Firebase RTDB connects in release build (no ProGuard stripping Firebase classes)
- [ ] Room database creates and returns data correctly in release build
- [ ] `kotlinx.serialization` works in release: create order item with variants → `selectedVariantsJson` round-trips correctly
- [ ] APK size ≤ 30 MB
- [ ] Zero ANRs or crashes on standard navigation through all main screens

## Testing
Install release APK on real device; run mini smoke test (login → shift open → 1 order → payment).

---

# TICKET-101: Firebase Auth Account Setup
**Roadmap:** P6-003 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-004

> Manual task — no new code.

## Affected Files
- `docs/firebase-setup.md` — modify (add account setup steps)

## Acceptance Criteria
- [ ] Owner account created in Firebase Auth console (owner's email + strong password)
- [ ] Staff account created (staff's email + password)
- [ ] Owner custom claim set: `{ "role": "owner" }` via Firebase Admin SDK script or console
- [ ] Staff custom claim set: `{ "role": "staff" }`
- [ ] Verify on device: sign in as owner → `SessionManager.userRole` emits `OWNER`
- [ ] Verify on device: sign in as staff → emits `STAFF`; Reports route inaccessible
- [ ] Custom claim setup steps documented step-by-step in `docs/firebase-setup.md`

## Testing
Manual: log in as both accounts on device; verify role-gated screens behave correctly.

---

# TICKET-102: RTDB Security Rules Final Deployment
**Roadmap:** P6-004 · **Scope:** MVP · **Est.:** ~30m · **Prereqs:** TICKET-035, TICKET-101

## Affected Files
- `firebase/database.rules.json` — verify up to date (no code changes unless rules need adjustment)

## Acceptance Criteria
- [ ] Rules deployed to Firebase project (confirmed, not just draft)
- [ ] Unauthenticated REST `GET /bills` returns `{ "error": "Permission denied" }`
- [ ] Owner-authenticated write succeeds
- [ ] Status regression rule verified: try to set PAID bill `status = "OPEN"` via Firebase REST API → rejected
- [ ] `firebase/database.rules.json` in git repo matches deployed rules exactly

## Testing
Firebase console Simulator: unauthenticated read → denied. Status regression → denied.

---

# TICKET-103: Device Installation and Two-Device Sync Test
**Roadmap:** P6-005 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-100, TICKET-101

## Affected Files
None (installation and integration test).

## Acceptance Criteria
- [ ] Release APK installed on Device 1 (owner's phone) and Device 2 (staff's phone)
- [ ] Device 1 logged in as owner; Device 2 logged in as staff
- [ ] Online sync test: bill created on Device 1 → appears on Device 2 within 5 seconds
- [ ] Offline sync test: Device 2 in airplane mode → create an order → reconnect → order syncs to RTDB → appears on Device 1
- [ ] Sync status bar shows correct states on Device 2 throughout (amber → hidden)
- [ ] Zero data loss during sync test

## Testing
Run all 4 sync scenarios manually with both devices present.

---

# TICKET-104: Full Service Day Smoke Test
**Roadmap:** P6-006 · **Scope:** MVP · **Est.:** ~1.5h · **Prereqs:** TICKET-103

## Affected Files
- `docs/smoke-test-checklist.md` — create (record results)

## Acceptance Criteria
- [ ] Open shift with Rp 100.000 float
- [ ] 3 grab-and-go orders: cash (exact), QRIS, split cash+QRIS
- [ ] 2 dine-in UPFRONT orders on different tables; at least 1 with variant selection
- [ ] 1 dine-in OPEN_BILL on Table 3; add items in 2 rounds
- [ ] Void 1 order item with "Kitchen Error"; note required only for "Other"
- [ ] Process payment for all active bills
- [ ] Log 2 expenses (Gas + Packaging)
- [ ] Close shift — Z-report totals verified against mental arithmetic
- [ ] RTDB console: all bills, payments, expenses, order items present and correctly structured
- [ ] Both EN and ID language verified for ≥ 5 screens
- [ ] Zero crashes recorded

## Testing
Complete smoke test with `docs/smoke-test-checklist.md` marking each item as it's verified.

---

# TICKET-105: README and Setup Documentation
**Roadmap:** P6-007 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-104

## Affected Files
- `README.md` — create
- `docs/firebase-setup.md` — finalize

## Acceptance Criteria
- [ ] README covers: prerequisites (Android Studio, Firebase CLI), cloning the repo, placing `google-services.json`, building debug + release APKs
- [ ] `docs/firebase-setup.md`: step-by-step for creating Auth accounts, setting custom claims, seeding `appConfig`, deploying security rules
- [ ] "How to release a new version" section: build → sign → distribute APK → update `minVersionCode` in RTDB
- [ ] RTDB path structure reference section matches architecture Appendix C exactly
- [ ] A new developer can complete device setup by following README alone (tested by a fresh read-through)

## Testing
Do a fresh read-through of README as if setting up from scratch — every step executable.

---

# TICKET-106: Crashlytics Integration
**Roadmap:** P6-008 · **Scope:** MVP · **Est.:** ~1h · **Prereqs:** TICKET-002, TICKET-100

## Affected Files
- `app/build.gradle.kts` — modify (add Crashlytics plugin + dependency via BOM)
- `WarungPosApplication.kt` — modify (disable Crashlytics in debug builds)

## Acceptance Criteria
- [ ] `id("com.google.firebase.crashlytics")` plugin applied
- [ ] `firebase-crashlytics-ktx` declared (via Firebase BOM — no version needed)
- [ ] In `onCreate()`: `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)`
- [ ] Debug build: crashes do NOT appear in Crashlytics dashboard (verified by triggering a test exception)
- [ ] Release build: force a test crash → appears in Firebase Crashlytics dashboard within 5 minutes
- [ ] No PII in crash reports: no user email, no order item content in custom crash keys

## Testing
Release build: `FirebaseCrashlytics.getInstance().crash()` → verify dashboard entry within 5 minutes.

---

# Summary

| Phase | Tickets | Scope |
|-------|---------|-------|
| 1: Project Setup | 001–013 | All MVP |
| 2: Data Layer | 014–038 | 024 = P2 (stock entities) |
| 3: Domain Layer | 039–057 | 057 partial P2 |
| 4: UI Layer | 058–082 | 081–082 = P2 |
| 5: Testing | 083–098 | All MVP |
| 6: Release | 099–106 | All MVP |
| **Total** | **106** | |

## Execution order within each phase
Respect the `Prereqs` field. Within a phase, tickets with no shared prereqs can run in any order.

## Commit message format
```
TICKET-NNN (PX-NNN): short description
```
Example: `TICKET-047 (P3-005): BillRepositoryImpl and PaymentRepositoryImpl`
