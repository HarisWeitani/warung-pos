# Warung POS — Production Android Architecture

**Role:** Principal Android Engineer  
**Based on:** project-description-research.md + warung-pos-prd.md  
**Date:** 2026-06-25

---

## ⚠️ Principal Engineer Notes Before Reading

### On Retrofit
The PRD and tech stack require **no Retrofit**. Retrofit is an HTTP client for custom REST APIs. This app has no custom backend — Firebase RTDB SDK handles all remote communication natively with its own persistent connection, offline queue, and conflict model. Including Retrofit would be dead code and a confusing dependency. It has been **excluded** and replaced with the Firebase Android SDK. If a custom backend is ever added in a future phase, Retrofit can be introduced then.

### On Multi-Module
The PRD confirms this is a personal app built by one developer. A multi-module setup (`:feature:order`, `:feature:billing`, etc.) would introduce build complexity, inter-module dependency graphs, and slower iteration with zero benefit at this scale. **Single module, feature-separated by package** is the correct call. The architecture is still Clean Architecture — the boundaries are enforced by package visibility conventions and code review discipline, not by Gradle modules.

### On Naming Convention
All monetary values are `Long` (Rupiah, integer). A `@JvmInline value class Rupiah(val value: Long)` wraps it in the domain layer to prevent accidental mixing with non-money Longs. Room stores it as `Long`, the value class is domain-layer only.

---

## 1. Architecture Diagram

```
╔══════════════════════════════════════════════════════════════════╗
║                       PRESENTATION LAYER                         ║
║                                                                  ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │              Jetpack Compose Screens                       │  ║
║  │   Stateless composables. Accept UiState, emit UiEvent.     │  ║
║  │   No logic. No direct ViewModel calls in deep children.    │  ║
║  └─────────────────────────┬──────────────────────────────────┘  ║
║              UiEvent ▲     │ collectAsStateWithLifecycle          ║
║                      │     ▼                                     ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │              ViewModels  (one per screen)                  │  ║
║  │   StateFlow<UiState> — single observable state per screen  │  ║
║  │   SharedFlow<UiEffect> — one-shot events (nav, snackbar)   │  ║
║  │   Calls UseCases, maps results, never holds domain logic   │  ║
║  └─────────────────────────┬──────────────────────────────────┘  ║
╚═════════════════════════════╪════════════════════════════════════╝
                              │ suspend / Flow
╔═════════════════════════════╪════════════════════════════════════╗
║                        DOMAIN LAYER                              ║
║                (Pure Kotlin — zero Android deps)                 ║
║                                                                  ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │                     Use Cases                              │  ║
║  │   One class per business action. Enforce all rules:        │  ║
║  │   bill state machine, shift preconditions, void guards,    │  ║
║  │   cash reconciliation formula, variant validation, etc.    │  ║
║  └─────────────────────────┬──────────────────────────────────┘  ║
║                            │ uses                                 ║
║  ┌─────────────────────────▼──────────────────────────────────┐  ║
║  │   Repository Interfaces    │   Domain Models                │  ║
║  │   (abstractions only)      │   (pure data classes)          │  ║
║  │                            │   Rupiah value class           │  ║
║  └─────────────────────────┬──────────────────────────────────┘  ║
╚═════════════════════════════╪════════════════════════════════════╝
                              │ implements
╔═════════════════════════════╪════════════════════════════════════╗
║                         DATA LAYER                               ║
║                                                                  ║
║  ┌───────────────────────────────────────────────────────────┐   ║
║  │              Repository Implementations                    │   ║
║  │   Map Room entities ↔ domain models                       │   ║
║  │   Write to Room first (optimistic)                        │   ║
║  │   Trigger sync after writes                               │   ║
║  └──────────┬─────────────────────────────┬──────────────────┘   ║
║             │                             │                      ║
║  ┌──────────▼────────────┐   ┌────────────▼──────────────────┐   ║
║  │   LOCAL: Room DB       │   │   SyncCoordinator             │   ║
║  │                        │   │                               │   ║
║  │   Single Source of     │   │   Owns all Firebase logic.    │   ║
║  │   Truth. All reads     │   │   Pushes PENDING → RTDB.      │   ║
║  │   come from here via   │   │   Pulls RTDB changes → Room.  │   ║
║  │   DAO Flows.           │   │   WorkManager + RTDB listeners │   ║
║  └────────────────────────┘   └──────────┬────────────────────┘   ║
║                                          │                        ║
║                               ┌──────────▼────────────────────┐   ║
║                               │   REMOTE: Firebase            │   ║
║                               │   RTDB — background mirror    │   ║
║                               │   Auth — email/password+claim │   ║
║                               └───────────────────────────────┘   ║
╚══════════════════════════════════════════════════════════════════╝

Cross-cutting:
  ┌──────────────────────────────────────────────────────────────┐
  │  Core / Shared                                               │
  │  - SessionManager (auth state, user role, device ID)         │
  │  - AppPreferences (language, per-device settings)            │
  │  - NetworkMonitor (connectivity StateFlow)                   │
  │  - NavGraph + route definitions                              │
  │  - Theme, Typography, string resources (bilingual)           │
  └──────────────────────────────────────────────────────────────┘
```

---

## 2. Module Structure

Single Gradle module. Enforced separation by package, not by module boundary.

```
:app  (the only module)
│
├── Gradle dependencies (see Section 8)
├── google-services.json  ← already in place
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── kotlin/com/warungpos/
    │   └── res/
    │       ├── values/strings.xml          ← English (default)
    │       ├── values-id/strings.xml       ← Bahasa Indonesia
    │       └── ...
    └── test/ + androidTest/
```

**Why not multi-module?**  
Build time penalty, inter-module `api` vs `implementation` discipline, navigation graph complexity across modules — all cost more than they provide for a single developer building a personal app. If this ever becomes a commercial product with a team, modularisation is the right next step. For now, package boundaries with `internal` visibility and UseCase isolation give 90% of the architectural benefit.

---

## 3. Package Structure

```
com.warungpos/
│
├── core/
│   ├── di/
│   │   ├── AppModule.kt              ← Room DB, RTDB, Auth instances
│   │   ├── RepositoryModule.kt       ← interface → impl bindings
│   │   └── WorkerModule.kt           ← Hilt worker factory
│   │
│   ├── navigation/
│   │   ├── AppNavGraph.kt            ← NavHost, all composable() entries, role guard
│   │   ├── BottomNavBar.kt
│   │   └── Routes.kt                 ← @Serializable data class/object route defs (type-safe Nav 2.8+)
│   │
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   │
│   ├── common/
│   │   ├── UiState.kt                ← sealed interface: Loading/Success/Error
│   │   ├── NetworkMonitor.kt         ← StateFlow<Boolean> isOnline
│   │   ├── SessionManager.kt         ← auth state, role, deviceId singleton
│   │   └── AppPreferences.kt         ← language pref (EncryptedSharedPrefs)
│   │
│   └── util/
│       ├── DateUtil.kt
│       ├── CurrencyFormatter.kt      ← Rp formatting, Indonesian locale
│       └── UuidGenerator.kt          ← client-side UUID for all entity IDs
│
├── domain/
│   ├── model/
│   │   ├── Bill.kt                   ← domain model (not Room entity)
│   │   ├── OrderItem.kt
│   │   ├── MenuItem.kt
│   │   ├── MenuCategory.kt
│   │   ├── VariantGroup.kt
│   │   ├── VariantOption.kt
│   │   ├── Table.kt
│   │   ├── Payment.kt
│   │   ├── PaymentMethod.kt
│   │   ├── Shift.kt
│   │   ├── ZReport.kt
│   │   ├── Expense.kt
│   │   ├── StockItem.kt
│   │   ├── StockBatch.kt
│   │   ├── StockOpname.kt
│   │   ├── StockOpnameLine.kt
│   │   ├── CartItem.kt               ← in-memory only, never persisted
│   │   └── Rupiah.kt                 ← @JvmInline value class Rupiah(val value: Long)
│   │
│   ├── repository/                   ← interfaces only, no implementation
│   │   ├── BillRepository.kt
│   │   ├── MenuRepository.kt
│   │   ├── OrderRepository.kt
│   │   ├── PaymentRepository.kt
│   │   ├── ShiftRepository.kt
│   │   ├── StockRepository.kt
│   │   ├── ExpenseRepository.kt
│   │   ├── ReportRepository.kt
│   │   └── SyncRepository.kt
│   │
│   └── usecase/
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   └── GetCurrentUserUseCase.kt
│       │
│       ├── order/
│       │   ├── GetMenuItemsUseCase.kt
│       │   ├── ConfirmOrderUseCase.kt          ← validates shift active, destination valid
│       │   └── AddItemsToExistingBillUseCase.kt
│       │
│       ├── bill/
│       │   ├── GetOpenBillsUseCase.kt
│       │   ├── GetBillDetailUseCase.kt
│       │   └── VoidBillUseCase.kt              ← owner-only guard enforced here
│       │
│       ├── payment/
│       │   ├── ProcessPaymentUseCase.kt        ← validates total, marks bill PAID
│       │   └── CalculateChangeUseCase.kt       ← pure, no repo needed
│       │
│       ├── void/
│       │   └── VoidOrderItemUseCase.kt         ← reason required, appends audit
│       │
│       ├── shift/
│       │   ├── OpenShiftUseCase.kt             ← checks no open shift exists
│       │   ├── CloseShiftUseCase.kt            ← checks zero open bills first
│       │   ├── CheckSoldOutItemsUseCase.kt     ← for shift-open prompt
│       │   ├── ResetSoldOutItemsUseCase.kt     ← batch reset on shift open
│       │   └── GenerateZReportUseCase.kt       ← assembles snapshot
│       │
│       ├── menu/
│       │   ├── UpsertMenuItemUseCase.kt
│       │   ├── ToggleSoldOutUseCase.kt
│       │   └── DeleteMenuItemUseCase.kt        ← soft-delete with bill-ref guard
│       │
│       ├── stock/
│       │   ├── RecordStockBatchUseCase.kt
│       │   ├── StartOpnameSessionUseCase.kt
│       │   ├── SubmitOpnameUseCase.kt
│       │   └── DeductStockOnPaymentUseCase.kt  ← called inside ProcessPaymentUseCase
│       │
│       ├── expense/
│       │   └── LogExpenseUseCase.kt
│       │
│       └── report/
│           ├── GetDailyDashboardUseCase.kt
│           ├── GetDateRangeReportUseCase.kt
│           ├── GetBestSellersUseCase.kt
│           └── ExportReportUseCase.kt          ← produces CSV/PDF bytes
│
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   └── WarungDatabase.kt               ← @Database, all entities, version 1
│   │   │
│   │   ├── entity/                             ← Room @Entity classes
│   │   │   ├── BillEntity.kt
│   │   │   ├── OrderItemEntity.kt
│   │   │   ├── MenuItemEntity.kt
│   │   │   ├── MenuCategoryEntity.kt
│   │   │   ├── VariantGroupEntity.kt
│   │   │   ├── VariantOptionEntity.kt
│   │   │   ├── TableEntity.kt
│   │   │   ├── PaymentEntity.kt
│   │   │   ├── PaymentMethodEntity.kt
│   │   │   ├── ShiftEntity.kt
│   │   │   ├── ZReportEntity.kt
│   │   │   ├── ExpenseEntity.kt
│   │   │   ├── StockItemEntity.kt              ← schema defined Phase 1, used Phase 2
│   │   │   ├── StockBatchEntity.kt             ← schema defined Phase 1, used Phase 2
│   │   │   ├── MenuItemIngredientEntity.kt     ← schema defined Phase 1, used Phase 2
│   │   │   ├── StockOpnameEntity.kt
│   │   │   └── StockOpnameLineEntity.kt
│   │   │
│   │   └── dao/
│   │       ├── BillDao.kt
│   │       ├── OrderItemDao.kt
│   │       ├── MenuItemDao.kt
│   │       ├── MenuCategoryDao.kt
│   │       ├── VariantDao.kt
│   │       ├── TableDao.kt
│   │       ├── PaymentDao.kt
│   │       ├── PaymentMethodDao.kt
│   │       ├── ShiftDao.kt
│   │       ├── ZReportDao.kt
│   │       ├── ExpenseDao.kt
│   │       ├── StockDao.kt
│   │       ├── StockOpnameDao.kt
│   │       └── ReportQueryDao.kt               ← aggregate queries (JOIN, GROUP BY)
│   │
│   ├── remote/
│   │   ├── firebase/
│   │   │   ├── FirebaseAuthDataSource.kt
│   │   │   └── FirebaseRtdbDataSource.kt       ← low-level RTDB read/write/listen
│   │   │
│   │   └── sync/
│   │       ├── SyncCoordinator.kt              ← the sync brain (see Section 6)
│   │       ├── SyncWorker.kt                   ← WorkManager worker
│   │       ├── RtdbListener.kt                 ← RTDB value event listeners
│   │       └── ConflictResolver.kt             ← LWW by updatedAt logic
│   │
│   ├── repository/
│   │   ├── BillRepositoryImpl.kt
│   │   ├── MenuRepositoryImpl.kt
│   │   ├── OrderRepositoryImpl.kt
│   │   ├── PaymentRepositoryImpl.kt
│   │   ├── ShiftRepositoryImpl.kt
│   │   ├── StockRepositoryImpl.kt
│   │   ├── ExpenseRepositoryImpl.kt
│   │   └── ReportRepositoryImpl.kt
│   │
│   └── mapper/
│       ├── BillMapper.kt                       ← entity ↔ domain model
│       ├── MenuMapper.kt
│       ├── OrderItemMapper.kt
│       ├── PaymentMapper.kt
│       ├── ShiftMapper.kt
│       ├── StockMapper.kt
│       └── ExpenseMapper.kt
│
└── feature/
    │   NOTE: every screen is split into XxxRoute.kt (ViewModel + nav callbacks)
    │         and XxxScreen.kt (pure UI). See Section 5 for the pattern.
    │
    ├── auth/
    │   ├── LoginRoute.kt               ← hiltViewModel(), effect collection
    │   ├── LoginScreen.kt              ← pure UI, no ViewModel reference
    │   ├── LoginViewModel.kt
    │   └── model/
    │       └── LoginUiState.kt         ← sealed interface
    │
    ├── order/
    │   ├── OrderRoute.kt               ← launch destination entry point
    │   ├── OrderScreen.kt              ← pure UI
    │   ├── OrderViewModel.kt           ← holds CartState in memory
    │   ├── component/
    │   │   ├── MenuItemGrid.kt
    │   │   ├── CategoryChipRow.kt
    │   │   ├── CartPanel.kt
    │   │   ├── VariantSelectionSheet.kt
    │   │   └── OrderDestinationSheet.kt
    │   └── model/
    │       └── OrderUiState.kt         ← sealed interface
    │
    ├── tables/
    │   ├── TablesRoute.kt
    │   ├── TablesScreen.kt
    │   ├── TablesViewModel.kt
    │   ├── BillDetailRoute.kt
    │   ├── BillDetailScreen.kt
    │   ├── BillDetailViewModel.kt
    │   ├── component/
    │   │   ├── TableCard.kt
    │   │   └── BillCard.kt
    │   └── model/
    │       ├── TablesUiState.kt        ← sealed interface
    │       └── BillDetailUiState.kt    ← sealed interface
    │
    ├── payment/
    │   ├── PaymentRoute.kt
    │   ├── PaymentScreen.kt
    │   ├── PaymentViewModel.kt
    │   ├── component/
    │   │   ├── PaymentMethodSelector.kt
    │   │   ├── ChangeCalculatorPanel.kt
    │   │   └── SplitPaymentRow.kt
    │   └── model/
    │       └── PaymentUiState.kt       ← sealed interface
    │
    ├── shift/
    │   ├── ShiftOpenRoute.kt
    │   ├── ShiftOpenScreen.kt
    │   ├── ShiftCloseRoute.kt
    │   ├── ShiftCloseScreen.kt
    │   ├── ShiftHistoryRoute.kt
    │   ├── ShiftHistoryScreen.kt
    │   ├── ZReportRoute.kt
    │   ├── ZReportScreen.kt
    │   ├── ShiftViewModel.kt
    │   └── model/
    │       ├── ShiftOpenUiState.kt     ← sealed interface
    │       ├── ShiftCloseUiState.kt    ← sealed interface
    │       └── ZReportUiState.kt       ← sealed interface
    │
    ├── menu/
    │   ├── MenuManagementRoute.kt
    │   ├── MenuManagementScreen.kt
    │   ├── MenuItemEditRoute.kt
    │   ├── MenuItemEditScreen.kt
    │   ├── VariantEditRoute.kt
    │   ├── VariantEditScreen.kt
    │   ├── CategoryManagementRoute.kt
    │   ├── CategoryManagementScreen.kt
    │   ├── MenuViewModel.kt
    │   └── model/
    │       └── MenuUiState.kt          ← sealed interface
    │
    ├── expense/
    │   ├── ExpenseLogRoute.kt
    │   ├── ExpenseLogScreen.kt
    │   ├── ExpenseViewModel.kt
    │   └── model/
    │       └── ExpenseUiState.kt       ← sealed interface
    │
    ├── stock/                          ← Phase 2 screens (empty stubs in Phase 1)
    │   ├── StockRoute.kt
    │   ├── StockScreen.kt
    │   ├── StockBatchRoute.kt
    │   ├── StockBatchScreen.kt
    │   ├── OpnameRoute.kt
    │   ├── OpnameScreen.kt
    │   ├── StockViewModel.kt
    │   └── model/
    │       └── StockUiState.kt         ← sealed interface
    │
    ├── reports/
    │   ├── DashboardRoute.kt           ← owner only, daily
    │   ├── DashboardScreen.kt
    │   ├── ReportRoute.kt              ← date range + export
    │   ├── ReportScreen.kt
    │   ├── BestSellerRoute.kt
    │   ├── BestSellerScreen.kt
    │   ├── ReportViewModel.kt
    │   └── model/
    │       ├── DashboardUiState.kt     ← sealed interface
    │       └── ReportUiState.kt        ← sealed interface
    │
    └── settings/
        ├── SettingsRoute.kt
        ├── SettingsScreen.kt
        ├── TableSettingsRoute.kt
        ├── TableSettingsScreen.kt
        ├── PaymentMethodSettingsRoute.kt
        ├── PaymentMethodSettingsScreen.kt
        ├── ExpenseCategorySettingsRoute.kt
        ├── ExpenseCategorySettingsScreen.kt
        ├── LanguageSettingsRoute.kt
        ├── LanguageSettingsScreen.kt
        ├── SettingsViewModel.kt
        └── model/
            └── SettingsUiState.kt      ← sealed interface
```

---

## 3b. Navigation — Type-Safe Routes (Navigation 2.8+)

`Routes.kt` uses `@Serializable` data classes and objects — NOT string literals or sealed class with string values. This gives compile-time argument type safety (e.g., `billId: String` cannot be passed where an `Int` is expected).

```kotlin
// core/navigation/Routes.kt
import kotlinx.serialization.Serializable

// Routes with no arguments → data object
@Serializable data object OrderRoute
@Serializable data object TablesRoute
@Serializable data object ShiftOpenRoute
@Serializable data object ShiftCloseRoute
@Serializable data object ShiftHistoryRoute
@Serializable data object MenuManagementRoute
@Serializable data object ExpenseLogRoute
@Serializable data object DashboardRoute
@Serializable data object ReportRoute
@Serializable data object SettingsRoute

// Routes with arguments → data class
@Serializable data class PaymentRoute(val billId: String)
@Serializable data class BillDetailRoute(val billId: String)
@Serializable data class ZReportRoute(val shiftId: String)
@Serializable data class MenuItemEditRoute(val menuItemId: String?)  // null = new item
@Serializable data class VariantEditRoute(val menuItemId: String)
```

```kotlin
// AppNavGraph.kt — NavHost registration
NavHost(navController, startDestination = OrderRoute) {
    composable<OrderRoute> {
        OrderRoute(onNavigateToPayment = { billId ->
            navController.navigate(PaymentRoute(billId))
        })
    }
    composable<PaymentRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<PaymentRoute>()
        PaymentRoute(
            billId = route.billId,
            onNavigateBack = navController::popBackStack,
        )
    }
    // etc.
}

// Reading args in ViewModel:
@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val billId: String = savedStateHandle.toRoute<PaymentRoute>().billId
}
```

---

## 4. Data Flow

### 4a. Write Flow (Order Confirmation — the critical path)

```
User taps "Confirm Order"
        │
        ▼
OrderViewModel.onConfirmOrder(cartItems, destination)
        │  [cart lives in ViewModel memory only, never persisted until here]
        ▼
ConfirmOrderUseCase.invoke(cartItems, destination, userId)
        │  Validates:
        │  - Active shift exists (ShiftRepository.getOpenShift() != null)
        │  - If destination = existing bill → bill is OPEN
        │  - All required VariantGroups fulfilled for each item
        │  - Cart is non-empty
        │  Builds:
        │  - Bill entity (if new) with UUID, sessionLabel, shiftId
        │  - OrderItem entities with name/price snapshots, UUID keys
        ▼
OrderRepository.confirmOrder(bill, orderItems)
        │
        ├──▶ Room: INSERT Bill + OrderItems (syncStatus = PENDING, updatedAt = now())
        │          [instant, on IO dispatcher]
        │
        └──▶ SyncCoordinator.notifyPendingSync()
                │
                ▼
           WorkManager schedules SyncWorker (if network available: immediate;
           if offline: queued with NETWORK_CONNECTED constraint)
                │
                ▼
           SyncWorker pushes PENDING records to RTDB (field-level paths):
           /bills/{id}/...
           /orderItems/{id}/...
                │
                ▼
           On success → Room: UPDATE syncStatus = SYNCED

Room emits updated Flow to BillRepositoryImpl
        │
        ▼
GetOpenBillsUseCase collects → TablesViewModel.uiState updates
        │
        ▼
TablesScreen recomposes — Device 1 sees the new bill immediately

Meanwhile, RTDB listener on Device 2:
RtdbListener receives /bills/{id} ValueEvent
        │
        ▼
ConflictResolver.resolve(incoming, existingInRoom)
        │  If incoming.updatedAt > room.updatedAt → write incoming to Room
        │  If incoming.updatedAt <= room.updatedAt → discard (local is newer)
        ▼
Room updated → Device 2's ViewModel observes → UI updates
```

### 4b. Read Flow (Reactive, always Room)

```
Screen enters composition
        │
        ▼
ViewModel declares uiState via stateIn (NOT viewModelScope.launch { collect }):

    val uiState: StateFlow<FooUiState> =
        useCase()
            .map { FooUiState.Success(it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FooUiState.Loading,
            )

    WHY stateIn over launch/collect:
    - WhileSubscribed(5_000) unsubscribes the upstream Room DAO Flow 5 seconds
      after the last collector disappears (e.g. screen goes to background).
    - launch { collect {} } keeps the DAO Flow active forever — wasting battery
      and preventing Room's SQLite invalidation tracker from being released.
        │
        ▼
UseCase calls repository.observeX() → returns Flow<List<DomainModel>>
        │
        ▼
RepositoryImpl maps DAO Flow<List<Entity>> → Flow<List<DomainModel>> via mapper
        │
        ▼
Room DAO Flow (backed by SQLite invalidation tracker)
        │  Emits new value whenever underlying table changes
        ▼
stateIn catches each emission → updates StateFlow value
        │
        ▼
Compose collectAsStateWithLifecycle() → UI recomposes
```

### 4c. Payment Flow (most complex write)

```
ProcessPaymentUseCase.invoke(billId, payments)
        │
        Validates:
        │  - Bill is OPEN
        │  - Sum of all payment amounts == bill.grandTotal
        │  - Cash tendered >= amount for cash payment rows
        │  - Active shift exists
        │
        Writes (all in a single Room transaction):
        │  - INSERT Payment rows
        │  - UPDATE Bill status = PAID, paidAt = now(), shiftId
        │  - If recipes exist: DeductStockOnPaymentUseCase (Phase 2)
        │
        ▼
BillRepository.processPayment(billId, payments)
        │
        ├──▶ Room: @Transaction { insert payments, update bill } (syncStatus = PENDING)
        │
        └──▶ SyncCoordinator.notifyPendingSync()
                │
                ▼
           RTDB: /bills/{id}/status = "PAID"         ← field-level write
                 /bills/{id}/paidAt = timestamp
                 /bills/{id}/shiftId = shiftId
                 /payments/{paymentId}/... = payment  ← append
                 [Use RTDB transaction for bill status to prevent stale-device reopen]
```

### 4d. Conflict Resolution (two devices, same open bill)

```
Device 1 adds "Nasi Goreng" to Bill #3
Device 2 adds "Es Teh" to Bill #3 simultaneously

Both:
  → Generate new OrderItem UUID
  → INSERT to local Room (no collision — different UUID keys)
  → Push to RTDB at /orderItems/{newUUID}/...  (no collision — different push keys)

Result: both items survive in RTDB and both devices sync both items.
Append-only order items = zero conflict by design.

The ONLY conflict scenario: two devices update the same Bill field.
Example: both try to set bill.note simultaneously.
  → Last write to RTDB wins (last updatedAt wins in Room on inbound sync)
  → For bill STATUS (OPEN→PAID): RTDB runTransaction() guards this transition
    so no stale device can overwrite PAID → OPEN.
```

---

## 5. State Management

### Pattern: Unidirectional Data Flow (UDF) per screen

Every screen ViewModel exposes exactly two observables:

```kotlin
// In every ViewModel:
val uiState: StateFlow<FooUiState> =
    useCase()
        .map { FooUiState.Success(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FooUiState.Loading)

// One-shot effects (navigation, snackbar) — NEVER replay on recomposition
private val _uiEffect = MutableSharedFlow<FooUiEffect>(
    replay = 0,             // do NOT replay — a "navigate to payment" must not fire twice
    extraBufferCapacity = 1 // allows tryEmit() to succeed even with no active collector
)
val uiEffect: SharedFlow<FooUiEffect> = _uiEffect.asSharedFlow()

// Screen sends events via:
fun onEvent(event: FooUiEvent)
```

### Route-Screen pattern (required for every screen)

Every screen is split into two composables. This is mandatory — not optional.

```kotlin
// XxxRoute.kt — owns ViewModel + navigation callbacks
// Collected here so the Screen is not tied to hiltViewModel()
@Composable
internal fun OrderRoute(
    onNavigateToPayment: (billId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is OrderUiEffect.NavigateToPayment -> onNavigateToPayment(effect.billId)
            }
        }
    }

    OrderScreen(
        uiState = uiState,
        onAddItem = viewModel::onAddItem,
        onConfirmOrder = viewModel::onConfirmOrder,
        modifier = modifier,
    )
}

// XxxScreen.kt — pure UI, receives all data and callbacks as parameters
// Testable with createComposeRule() alone, no Hilt required
@Composable
internal fun OrderScreen(
    uiState: OrderUiState,
    onAddItem: (MenuItemUi) -> Unit,
    onConfirmOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        OrderUiState.Loading -> CircularProgressIndicator()
        is OrderUiState.Error -> ErrorState(uiState.message)
        is OrderUiState.Success -> { /* render menu grid + cart */ }
    }
}
```

**Rules:**
- `XxxRoute.kt` is the NavGraph entry point — it calls `hiltViewModel()` and `collectAsStateWithLifecycle()`
- `XxxScreen.kt` receives only plain data and lambdas — zero ViewModel/Hilt references
- `LaunchedEffect` for `uiEffect` collection lives in the Route, never the Screen
- Every screen file pair: `LoginRoute.kt` + `LoginScreen.kt`, `PaymentRoute.kt` + `PaymentScreen.kt`, etc.

### UiState design

Each screen has its own **sealed interface**. Never use a flat data class with `isLoading: Boolean`
(that allows invalid states like `isLoading = true` + non-empty data simultaneously).
Never share UiState between screens.

```kotlin
// CORRECT — sealed interface per screen
sealed interface OrderUiState {
    data object Loading : OrderUiState
    data class Success(
        val menuItems: List<MenuItemUi>,   // grouped by category
        val cart: List<CartItemUi>,        // in-memory, never persisted until confirmed
        val cartTotal: Rupiah,
        val activeShift: ShiftSummary?,    // null = no shift open, blocks ordering
        val selectedCategory: String?,
    ) : OrderUiState
    data class Error(val message: String) : OrderUiState
}

// WRONG — do not use this pattern
// data class OrderUiState(val menuItems: List<MenuItemUi>, val isLoading: Boolean, ...)

sealed interface PaymentUiState {
    data object Loading : PaymentUiState
    data class Success(
        val bill: BillDetailUi,
        val enabledPaymentMethods: List<PaymentMethodUi>,
        val payments: List<PaymentRowUi>,   // split payment entries so far
        val remainingBalance: Rupiah,
        val changeAmount: Rupiah,           // for cash row
        val canComplete: Boolean,           // remaining == 0 and all rows valid
    ) : PaymentUiState
    data class Error(val message: String) : PaymentUiState
}

sealed interface ShiftCloseUiState {
    data object Loading : ShiftCloseUiState
    data class Success(
        val openBills: List<OpenBillBlockerUi>,  // empty = can proceed
        val canClose: Boolean,                   // openBills.isEmpty()
        val countedCash: Rupiah,
        val expectedCash: Rupiah,
        val variance: Rupiah,
        val isSubmitting: Boolean,
    ) : ShiftCloseUiState
    data class Error(val message: String) : ShiftCloseUiState
}
```

### Cart State (special case — in-memory only)

Cart is NOT a database concern. It lives exclusively in `OrderViewModel`:

```
OrderViewModel holds:
  private val _cart = MutableStateFlow<List<CartItem>>(emptyList())

CartItem = { menuItem, selectedVariants, quantity }

Cart is built from in-memory operations (add, increment, decrement, remove).
It is ONLY written to Room when the operator confirms the order.
Navigating away from the Order screen clears the cart (ViewModel lifecycle).
```

### Role-Gated UI

Role check happens at the NavGraph level (route guard), not just in the composable. Owner-only routes are unreachable by staff — the bottom nav item is hidden and direct navigation to the route is rejected.

```
In AppNavGraph:
  val userRole by sessionManager.role.collectAsStateWithLifecycle()
  
  // Reports tab: only rendered in bottom nav if role == OWNER
  // Reports route: composable checks role before rendering, redirects if staff
```

### Sync Status (cross-cutting UI state)

A global `SyncStatusBar` composable sits above the `Scaffold` content. It observes:

```
SyncViewModel (application-scoped, Hilt):
  - networkMonitor.isOnline: StateFlow<Boolean>
  - pendingCount: StateFlow<Int>  (Room query: COUNT WHERE syncStatus = PENDING)

Renders:
  - SYNCED + online: hidden
  - PENDING (N records): subtle amber bar "Syncing N items…"
  - OFFLINE: red bar "Offline — data saves locally"
```

---

## 6. Offline Strategy

### Core Principle

> Room is the single source of truth. Every feature works 100% offline. Firebase is a background mirror.

### SyncCoordinator — the sync brain

`SyncCoordinator` is an application-scoped singleton (Hilt `@Singleton`) responsible for:

1. **Outbound sync** — pushing `syncStatus = PENDING` records from Room to RTDB
2. **Inbound sync** — listening to RTDB and writing remote changes to Room
3. **Conflict resolution** — via `ConflictResolver` (LWW by `updatedAt`)

```
SyncCoordinator responsibilities:

Outbound:
  - Triggered by: WorkManager (periodic + on network reconnect)
  - Queries Room for all entities WHERE syncStatus = PENDING ORDER BY updatedAt ASC
  - Writes each to RTDB at its flat path using field-level setValue (not the whole object)
  - On RTDB success → update Room syncStatus = SYNCED
  - On RTDB failure → leave as PENDING, WorkManager retries with exponential backoff

Inbound:
  - RTDB ValueEventListeners on all top-level paths (/bills, /orderItems, /payments, etc.)
  - On each snapshot change → ConflictResolver.resolve(incoming, existingInRoom)
  - If incoming wins → write to Room with syncStatus = SYNCED
  - If local wins → no write, local is already newer

Atomic operations (not via SyncCoordinator, done directly):
  - Bill status OPEN→PAID: RTDB runTransaction() in PaymentRepository
  - Stock quantity deduction: ServerValue.increment(-n) in StockRepository
  - Single open shift: /appConfig/openShiftId updated with RTDB transaction
```

### Sync metadata on every entity

```kotlin
// Present on every Room entity (as fields, not embedded — Room doesn't support Kotlin delegation)
val updatedAt: Long        // System.currentTimeMillis() at time of write
val syncStatus: SyncStatus // PENDING | SYNCED | CONFLICTED
val deviceId: String       // stable per-device UUID, set once at install
```

### RTDB Offline Persistence

Firebase RTDB Android SDK is configured with offline persistence enabled:

```
FirebaseDatabase.getInstance().setPersistenceCacheSizeBytes(5 * 1024 * 1024) // 5 MB
FirebaseDatabase.getInstance().setPersistenceEnabled(true)
```

This means RTDB SDK caches writes locally and sends them on reconnect — this doubles with Room's own PENDING queue but provides a safety net. Room is still SoT; RTDB's local cache is just a flush buffer.

### WorkManager sync scheduling

```
SyncWorker configuration:
  - Constraints: requiresNetwork = CONNECTED
  - Triggered on: any write that sets syncStatus = PENDING
  - Retry policy: ExponentialBackoff (10s base, 5 min max)
  - Periodic backup sync: every 15 minutes while network available
  - On reconnect (NetworkMonitor detects online): immediate enqueue
```

### Version gate

On every app start, before any other operation:

```
AppStartupManager checks RTDB /appConfig/minVersionCode
  If BuildConfig.VERSION_CODE < minVersionCode:
    → Block app with UpdateRequiredScreen
    → No data access until app is updated
  Else: proceed normally
```

---

## 7. Testing Strategy

### Layer 1: Unit Tests (fast, pure Kotlin, no Android)

**Target:** UseCases + Domain logic  
**Framework:** JUnit 4 + `kotlinx-coroutines-test` + Turbine + Fake repositories (no MockK)

Key tests per UseCase:

```
ConfirmOrderUseCase:
  ✓ Happy path: valid cart, active shift, new table bill → OrderItem list created
  ✓ No active shift → throws ShiftNotOpenException
  ✓ Empty cart → throws EmptyCartException
  ✓ Bill destination OPEN_BILL → order items appended
  ✓ Required variant missing → throws MissingRequiredVariantException
  ✓ Grab-and-go → tableId = null, type = UPFRONT

ProcessPaymentUseCase:
  ✓ Cash exact amount → change = 0
  ✓ Cash overpaid → correct change calculated
  ✓ Cash underpaid → throws InsufficientTenderedAmountException
  ✓ Split payment: cash + QRIS summing to total → bill marked PAID
  ✓ Split payment total < grandTotal → cannot complete
  ✓ Bill already PAID → throws BillAlreadyPaidException

CloseShiftUseCase:
  ✓ Open bills exist → throws OpenBillsBlockShiftCloseException(openBills)
  ✓ No open bills → Z-report generated with correct totals
  ✓ Cash variance computed correctly (openingFloat + cashSales - cashExpenses - countedCash)

VoidBillUseCase:
  ✓ Owner role → void proceeds
  ✓ Staff role → throws InsufficientPermissionsException
  ✓ Bill already PAID + no override → throws BillNotVoidableException

CalculateChangeUseCase:
  ✓ Pure function, no repo needed — tests trivial
```

**Test doubles:** `FakeBillRepository`, `FakeShiftRepository`, etc. implementing domain interfaces. No MockK, no Mockito — hand-written fakes expose test-hook methods and exercise real code paths.

### Layer 2: Integration Tests (Room DAOs)

**Target:** Room DAOs, query correctness  
**Framework:** JUnit 4 + `androidx.room.testing` (in-memory DB) + Robolectric (for JVM speed)

Key tests:

```
BillDao:
  ✓ Insert bill → getOpenBillsForTable returns it
  ✓ Update status to PAID → getOpenBillsForTable excludes it
  ✓ Multiple bills on same tableId → all returned
  ✓ Flow emits on insert

ReportQueryDao:
  ✓ Aggregate: sumGrandTotal for date range
  ✓ GROUP BY paymentMethodId returns correct breakdown
  ✓ Best seller query ranks by quantity DESC

OrderItemDao:
  ✓ Insert order items for bill → getAllForBill returns them
  ✓ Void item → item still returned (soft void), lineTotal excluded from sum
  ✓ Append-only: two concurrent inserts → both survive (different PKs)
```

### Layer 3: ViewModel Tests (coroutines + StateFlow)

**Target:** ViewModels, UiState transitions, UiEffect emissions  
**Framework:** JUnit 4 + `coroutines-test` + Turbine + Fake repositories

```
OrderViewModel:
  ✓ onAddItem → cart contains item with quantity 1
  ✓ onAddItem (same item again) → quantity = 2
  ✓ onDecrementItem at qty 1 → item removed from cart
  ✓ No active shift → uiState.activeShift == null, confirm blocked
  ✓ onConfirmOrder → ConfirmOrderUseCase called, cart cleared, uiEffect = NavigateToBills

PaymentViewModel:
  ✓ onAddPayment(cash, 50000) on bill.grandTotal = 50000 → remainingBalance = 0, canComplete = true
  ✓ onAddPayment(cash, 30000) + onAddPayment(QRIS, 20000) → split, remainingBalance = 0
  ✓ onAddPayment(cash, 40000) on total 50000 → canComplete = false

ShiftViewModel:
  ✓ openBills not empty → ShiftCloseUiState.canClose = false
  ✓ openBills empty → canClose = true
  ✓ onClose submission → Z-report emitted, uiEffect = NavigateToZReport
```

### Layer 4: UI Tests (Compose, key flows only)

**Target:** Critical user journeys end-to-end on device/emulator  
**Framework:** `androidx.compose.ui.test` + Hilt test injection + in-memory Room

Scope: test the 3 most critical flows only. UI tests are expensive — don't test every screen.

```
Flow 1 — Grab-and-go order + payment:
  1. Tap menu item → cart shows 1 item
  2. Tap "Confirm Order" → select grab-and-go
  3. On payment screen → select Cash, enter amount
  4. Tap Pay → success state shown, cart cleared

Flow 2 — Open bill: add items across two steps:
  1. Create open bill on Table 1
  2. Confirm first order
  3. Return to order screen, add more items
  4. Confirm "Add to existing bill" on Table 1
  5. Bill detail shows both sets of items

Flow 3 — Shift close blocked by open bill:
  1. Create an open bill
  2. Navigate to shift close
  3. Shift close screen shows blocking bill list
  4. Close the bill
  5. Return to shift close → canClose = true
```

### Test file layout

```
src/
├── test/                          ← JVM unit tests
│   └── com/warungpos/
│       ├── domain/usecase/        ← UseCase tests
│       └── feature/*/             ← ViewModel tests
│
└── androidTest/                   ← instrumented tests
    └── com/warungpos/
        ├── data/local/dao/        ← DAO integration tests
        └── feature/*/             ← Compose UI flow tests
```

---

## 8. Dependency List

### Kotlin & Build

```kotlin
// build.gradle.kts (project)
kotlin = "2.0.21"                  // or latest stable at build time
agp = "8.7.x"

// build.gradle.kts (app)
compileSdk = 35
minSdk = [follow existing project]
targetSdk = 35
```

### Jetpack Compose

```kotlin
// Use BOM to keep all Compose versions in sync
implementation(platform("androidx.compose:compose-bom:2025.xx.xx"))  // latest stable
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.9.x")
```

### Navigation

```kotlin
implementation("androidx.navigation:navigation-compose:2.8.x")
implementation("androidx.hilt:hilt-navigation-compose:1.2.x")
```

### Lifecycle & ViewModel

```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.x")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.x")
// collectAsStateWithLifecycle — included in lifecycle-runtime-compose
```

### Dependency Injection

```kotlin
implementation("com.google.dagger:hilt-android:2.52")
ksp("com.google.dagger:hilt-android-compiler:2.52")   // KSP only — KAPT is banned
```

> **KAPT is banned in this project** (deprecated in Kotlin 2.x). Always use `ksp(...)`, never `kapt(...)`.
> Required plugin in `build.gradle.kts`: `id("com.google.devtools.ksp")`

### Room

```kotlin
implementation("androidx.room:room-runtime:2.7.x")
implementation("androidx.room:room-ktx:2.7.x")    // Flow + suspend support
ksp("androidx.room:room-compiler:2.7.x")           // KSP only — never kapt
```

> **DAO upsert:** use `@Upsert` annotation (Room 2.5+) for insert-or-replace operations.
> Never use `@Insert(onConflict = OnConflictStrategy.REPLACE)` — `@Upsert` is cleaner and
> handles the update-vs-insert distinction correctly.

### Firebase

```kotlin
implementation(platform("com.google.firebase:firebase-bom:33.x.x"))  // latest stable
implementation("com.google.firebase:firebase-database-ktx")           // RTDB
implementation("com.google.firebase:firebase-auth-ktx")               // Auth
// No Firestore. No Firebase Storage. No Cloud Messaging in scope.
```

### Background Work

```kotlin
implementation("androidx.work:work-runtime-ktx:2.10.x")
implementation("androidx.hilt:hilt-work:1.2.x")
ksp("androidx.hilt:hilt-compiler:1.2.x")           // KSP only — never kapt
```

**Hilt WorkManager wiring — three mandatory steps (all three must be done or it crashes at runtime):**

**Step 1** — Remove the default initializer from `AndroidManifest.xml`:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="remove" />
```

**Step 2** — Bind `HiltWorkerFactory` in `WorkerModule.kt`:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface WorkerModule {
    @Binds
    fun bindWorkerFactory(factory: HiltWorkerFactory): WorkerFactory
}
```

**Step 3** — Initialize WorkManager manually in `Application`:
```kotlin
@HiltAndroidApp
class WarungPosApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

**Step 4** — Annotate every worker with `@HiltWorker` + `@AssistedInject`:
```kotlin
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncCoordinator: SyncCoordinator,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result { ... }
}
```

### Coroutines

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.x")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.x")
// ↑ Required for Firebase Task → coroutine await() extension
```

### Security (EncryptedSharedPreferences)

```kotlin
implementation("androidx.security:security-crypto:1.1.0-alpha06")
// Use latest stable; 1.0.0 stable does not support API 23+ features we need
```

### JSON (for selectedVariantsJson in OrderItem)

```kotlin
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.x")
// Add plugin: id("org.jetbrains.kotlin.plugin.serialization")
// Do NOT use Gson — it uses reflection and breaks with R8 without proguard rules
```

### Export (Phase 2 — CSV + PDF)

```kotlin
// CSV: no library needed — write comma-separated strings manually
// PDF: iText or Apache PDFBox are heavy. Recommend:
implementation("com.itextpdf:itext7-core:7.x.x")   // or
// Alternative: generate HTML string and use Android WebView to print-to-PDF
// Simpler alternative for MVP: share as plain text / CSV only, add PDF in Phase 2
```

### Testing

```kotlin
// Unit tests (JVM)
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.x")
testImplementation("app.cash.turbine:turbine:1.2.x")       // Flow testing

// No MockK / no Mockito. Use Fake implementations of domain interfaces instead:
// FakeBillRepository, FakeShiftRepository, FakeMenuRepository, etc.
// Fakes are hand-written classes that implement the repository interface and expose
// test-hook methods (e.g. sendBills(listOf(...))). This produces less brittle tests
// and exercises more production code paths than mocks.

// Instrumented tests (Android)
androidTestImplementation("androidx.test.ext:junit:1.2.x")
androidTestImplementation("androidx.test.espresso:espresso-core:3.6.x")
androidTestImplementation(platform("androidx.compose:compose-bom:2025.xx.xx"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
androidTestImplementation("androidx.room:room-testing:2.7.x")
androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
kspAndroidTest("com.google.dagger:hilt-android-compiler:2.52")   // KSP — never kaptAndroidTest

// Debug
debugImplementation("androidx.compose.ui:ui-tooling")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

**Test doubles pattern (Fake, not Mock):**

```kotlin
// In src/test/ — hand-written fake, no MockK needed
class FakeBillRepository : BillRepository {

    private val billsFlow = MutableSharedFlow<List<Bill>>(replay = 1)

    // Test hook — call from test to push data
    fun sendBills(bills: List<Bill>) { billsFlow.tryEmit(bills) }

    override fun observeOpenBills(): Flow<List<Bill>> = billsFlow
    override suspend fun confirmOrder(bill: Bill, items: List<OrderItem>) { /* no-op */ }
    // ... remaining interface methods
}

// ViewModel test
class OrderViewModelTest {
    @get:Rule val dispatcherRule = TestDispatcherRule()

    private val fakeBillRepository = FakeBillRepository()
    private val fakeShiftRepository = FakeShiftRepository()
    private lateinit var viewModel: OrderViewModel

    @Before fun setup() {
        viewModel = OrderViewModel(
            confirmOrderUseCase = ConfirmOrderUseCase(fakeBillRepository, fakeShiftRepository),
        )
    }

    @Test fun `uiState is Loading initially`() = runTest {
        assertEquals(OrderUiState.Loading, viewModel.uiState.value)
    }
}
```

**TestDispatcherRule** (place in `src/test/kotlin/com/warungpos/util/`):

```kotlin
class TestDispatcherRule(
    val testDispatcher: TestCoroutineDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) { Dispatchers.setMain(testDispatcher) }
    override fun finished(description: Description) { Dispatchers.resetMain() }
}
```

### Explicitly NOT included (and why)

| Library | Why excluded |
|---------|-------------|
| **Retrofit** | No custom REST API. Firebase SDK handles all remote I/O. |
| **OkHttp** | Same reason. Firebase manages its own HTTP layer. |
| **Glide / Coil** | No images in scope (PRD OQ-5 confirmed). |
| **Gson** | R8 reflection issues. Use `kotlinx-serialization` instead. |
| **Moshi** | Same reason — `kotlinx-serialization` is sufficient. |
| **Firestore** | RTDB chosen for free-tier bandwidth model. |
| **Firebase Storage** | No images. Eliminates an entire Firebase product dependency. |
| **Paging 3** | Small dataset — a warung has <200 menu items, <50 open bills. Paging is overkill. |
| **DataStore** | Only used for language preference. EncryptedSharedPreferences is simpler for 1 key. |
| **LeakCanary** | Add only during development, never ship it (it makes APK heavy). |

---

## Appendix: Key Architectural Decisions (ADR Summary)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Module structure | Single module | Personal app, single dev, faster iteration |
| Network layer | Firebase SDK only | No REST backend; Retrofit would be dead code |
| Local DB | Room | Official Jetpack, Flow support, type-safe queries |
| Remote sync | RTDB (not Firestore) | No per-read/write billing; warung scale never hits download limit |
| Source of truth | Room | UI never reads from Firebase directly |
| Money type | Long (Rupiah) + Rupiah value class | No floating point errors in currency |
| PK strategy | Client-generated UUID String | Global uniqueness across devices without server round-trip |
| Cart persistence | In-memory (ViewModel) only | Never dirty the DB with uncommitted orders |
| Conflict model | LWW by updatedAt, field-level writes | Sufficient for 2-device warung; append-only OrderItems avoids collisions entirely |
| Bill status guard | RTDB runTransaction() on OPEN→PAID | Prevents stale device from reopening a paid bill |
| Stock quantity | ServerValue.increment() | Atomic server-side decrement — no race condition |
| Serialisation | kotlinx-serialization | R8-safe, Kotlin-native, no reflection |
| Testing scope | UseCase + DAO + 3 critical UI flows | Right balance for personal app; avoid over-testing |
| Language | Bilingual from Day 1, strings.xml only | Retrofit is painful; bilingual retrofit is more painful |
| Stock schema | Defined in Phase 1, used in Phase 2 | Avoid Room migrations for FK additions |
