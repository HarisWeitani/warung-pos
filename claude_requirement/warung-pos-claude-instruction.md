# Claude Code Instructions — Warung POS

**Version:** 1.0  
**Authority:** This file overrides any general coding preferences. Every rule here was decided deliberately. Do not deviate without explicit owner instruction in the current session.

---

## 0. How to Use This File

Read this entire file before writing any code in a session. When uncertain about a decision, check this file first. If a rule conflicts with a suggestion from training data or general best practices, **this file wins**. This is a personal food-stall POS app with specific, locked-in architectural decisions.

**Reference documents (always available):**
- `warung-pos-architecture.md` — package structure, data flow diagrams, entity schema
- `warung-pos-prd.md` — feature requirements, user stories, business rules
- `warung-pos-roadmap.md` — build order, task IDs, definitions of done

---

## 1. Project Identity

```
App:       Warung POS — personal Android POS for a small Indonesian food stall
Platform:  Android only. Single module. Portrait orientation.
Language:  Kotlin only. No Java.
Min SDK:   Follow existing project setting.
```

**What this app is NOT:**
- Not a multi-tenant SaaS product
- Not distributed via Play Store
- Not connected to a payment gateway
- Not a multi-module project
- Not a REST API consumer (there is no REST backend)

---

## 2. Absolute Rules — Never Violate These

These rules have no exceptions. Do not rationalise around them.

### 2.1 Banned Libraries

| Library | Reason |
|---------|--------|
| `Retrofit` | No REST API exists. Firebase SDK handles all remote I/O. |
| `OkHttp` | Same reason. Firebase manages its own HTTP layer. |
| `Gson` | Breaks with R8 without manual ProGuard rules. Use `kotlinx-serialization`. |
| `Moshi` | Same reason as Gson. |
| `Glide` / `Coil` | No images in this app. PRD confirmed no images. |
| `Firestore` | RTDB chosen. Firestore has per-operation billing that can spike. |
| `Firebase Storage` | No images. Eliminate the cost dimension entirely. |
| `Paging 3` | Dataset is tiny (<200 items, <50 open bills). Paging is overkill. |
| `DataStore` | Only 1 preference exists (language). `EncryptedSharedPreferences` is correct. |
| `LeakCanary` | Development tool only. Never in production or release builds. |
| `KAPT` | Deprecated in Kotlin 2.x. KSP only. |

### 2.2 Money Rules — Zero Tolerance

```kotlin
// ✅ CORRECT — always Long in entities
val basePrice: Long = 15000L
val grandTotal: Long = 45000L

// ✅ CORRECT — Rupiah value class in domain layer
val price: Rupiah = Rupiah(15000L)
val total: Rupiah = price + Rupiah(3000L)  // = Rupiah(18000L)

// ❌ FORBIDDEN — never Float or Double for money
val price: Float = 15000.0f   // NEVER
val price: Double = 15000.0   // NEVER

// ❌ FORBIDDEN — never raw Long in domain models (use Rupiah)
data class Bill(val grandTotal: Long)  // WRONG in domain layer

// ❌ FORBIDDEN — never Rupiah in Room entities (unwrap to Long)
@Entity data class BillEntity(val grandTotal: Rupiah)  // WRONG
```

**Rule:** `Long` in Room entities. `Rupiah` in domain models. Never `Float`/`Double` anywhere.

### 2.3 Primary Key Rules

```kotlin
// ✅ CORRECT
@PrimaryKey val id: String = UuidGenerator.generate()

// ❌ FORBIDDEN
@PrimaryKey(autoGenerate = true) val id: Int  // breaks multi-device sync
@PrimaryKey(autoGenerate = true) val id: Long // same problem
```

**Rule:** All entity PKs are `String` UUIDs generated client-side. Never autoincrement.

### 2.4 Hardcoded Strings — Zero Tolerance

```kotlin
// ✅ CORRECT
Text(stringResource(R.string.order_confirm_button))

// ❌ FORBIDDEN
Text("Confirm Order")   // hardcoded English
Text("Konfirmasi")      // hardcoded Indonesian
```

**Rule:** Every display string goes in `res/values/strings.xml` (English) AND `res/values-id/strings.xml` (Bahasa Indonesia) in the same commit. Use `stringResource()` in Compose. No exceptions.

### 2.5 Room is the Single Source of Truth

```kotlin
// ✅ CORRECT — UI reads only from Room via Flow
val bills = billRepository.observeOpenBills()  // backed by Room DAO Flow

// ❌ FORBIDDEN — UI never reads from Firebase directly
val bills = firebaseDatabase.getReference("bills")  // NEVER in ViewModel or UseCase
```

**Rule:** The UI observes Room Flows exclusively. Firebase writes happen in the background via `SyncCoordinator`. ViewModels never hold a Firebase reference.

### 2.6 Domain Layer Imports

```kotlin
// ✅ CORRECT — domain layer only imports
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

// ❌ FORBIDDEN in domain/model/ or domain/usecase/ or domain/repository/
import androidx.room.*          // Room belongs in data layer
import com.google.firebase.*    // Firebase belongs in data layer
import android.content.Context  // Android context belongs in data layer
import androidx.lifecycle.*     // Lifecycle belongs in presentation layer
```

**Rule:** The domain layer (`domain/model/`, `domain/repository/`, `domain/usecase/`) has zero Android framework imports. Pure Kotlin + coroutines only.

### 2.7 RTDB Write Strategy

```kotlin
// ✅ CORRECT — field-level writes
rtdbDataSource.write("bills/$billId/status", "PAID")
rtdbDataSource.write("bills/$billId/paidAt", timestamp)

// ❌ FORBIDDEN — whole-object replacement loses concurrent updates
rtdbDataSource.write("bills/$billId", entireBillObject)  // NEVER
```

**Rule:** RTDB writes are always to specific field paths. Never replace a whole object node. The only exceptions are initial creation of a new entity (where no concurrent writes can exist yet).

---

## 3. Architecture Rules

### 3.1 Layer Dependency Direction

```
Presentation (feature/) → Domain (domain/) ← Data (data/)
                                   ↑
                           (interfaces only)

Rule: outer layers depend on inner. Inner layers NEVER import outer layers.
```

Concretely:
- `feature/` imports from `domain/`
- `data/` implements interfaces from `domain/`
- `domain/` imports nothing from `feature/` or `data/`
- `core/` is imported by all layers (utilities, DI, navigation, theme)

### 3.2 What Lives Where

| Location | What belongs there |
|----------|--------------------|
| `domain/model/` | Pure Kotlin data classes. Domain models, `CartItem`, `Rupiah`, exception classes. No Room/Firebase annotations. |
| `domain/repository/` | Interface definitions only. `suspend fun` and `Flow` return types using domain models. |
| `domain/usecase/` | One class per business action. `operator fun invoke()`. Business rules only. |
| `data/local/entity/` | Room `@Entity` classes. Sync metadata fields. No domain logic. |
| `data/local/dao/` | Room `@Dao` interfaces. SQL queries. Return `Flow` or `suspend`. |
| `data/local/db/` | `WarungDatabase.kt` only. |
| `data/remote/firebase/` | Firebase data sources. Coroutine wrappers. Return `Result<T>`. |
| `data/remote/sync/` | `SyncCoordinator`, `SyncWorker`, `RtdbListener`, `ConflictResolver`. |
| `data/repository/` | `*RepositoryImpl` classes. Map entities ↔ domain. Write Room first. Signal sync. |
| `data/mapper/` | Extension functions converting entity ↔ domain model. |
| `core/di/` | Hilt `@Module` classes only. |
| `core/navigation/` | `Routes`, `AppNavGraph`, `BottomNavBar`. |
| `core/common/` | `SessionManager`, `NetworkMonitor`, `AppPreferences`, `UiState`. |
| `core/util/` | `UuidGenerator`, `DateUtil`, `CurrencyFormatter`. |
| `core/theme/` | Material 3 theme, colors, typography. |
| `feature/{name}/` | Compose screens, ViewModels, screen-specific UiState/UiEffect/UiEvent, screen components. |

### 3.3 Single Module

This is one Gradle module (`:app`). Do not create submodules (`:feature:order`, `:core:domain`, etc.). Boundaries are enforced by package structure and code review, not by Gradle.

### 3.4 Hilt Scopes

| Scope | When to use |
|-------|-------------|
| `@Singleton` | `SessionManager`, `NetworkMonitor`, `SyncCoordinator`, `WarungDatabase`, Firebase instances, all repositories |
| `@ViewModelScoped` | Use cases injected into a single ViewModel (prefer `@Singleton` for use cases to avoid re-creation) |
| `@ActivityScoped` | Rare — avoid unless needed |

Default: use `@Singleton` for all application-level services and repositories. Use cases are `@Singleton` by default.

---

## 4. Naming Conventions

### 4.1 Files and Classes

| Type | Pattern | Example |
|------|---------|---------|
| Room entity | `{Name}Entity` | `BillEntity`, `OrderItemEntity` |
| DAO | `{Name}Dao` | `BillDao`, `ReportQueryDao` |
| Database | `WarungDatabase` | (fixed, one database) |
| Repository interface | `{Name}Repository` | `BillRepository` |
| Repository implementation | `{Name}RepositoryImpl` | `BillRepositoryImpl` |
| Domain model | `{Name}` | `Bill`, `MenuItem`, `CartItem` |
| Mapper | `{Name}Mapper` | `BillMapper`, `OrderItemMapper` |
| Use case | `{Verb}{Noun}UseCase` | `ConfirmOrderUseCase`, `CloseShiftUseCase` |
| ViewModel | `{FeatureName}ViewModel` | `OrderViewModel`, `PaymentViewModel` |
| Screen composable | `{FeatureName}Screen` | `OrderScreen`, `PaymentScreen` |
| Screen component | descriptive name | `MenuItemGrid`, `CartPanel`, `VariantSelectionSheet` |
| UiState | `{FeatureName}UiState` | `OrderUiState`, `PaymentUiState` |
| UiEffect | `{FeatureName}UiEffect` | `OrderUiEffect` |
| UiEvent | `{FeatureName}UiEvent` | `OrderUiEvent` |
| Exception | descriptive + `Exception` | `ShiftNotOpenException`, `BillAlreadyPaidException` |
| Hilt module | `{Purpose}Module` | `AppModule`, `RepositoryModule`, `WorkerModule` |

### 4.2 Functions

```kotlin
// Repository methods:
fun observeX(): Flow<List<X>>   // streaming — always Flow, never suspend
suspend fun getX(): X            // one-shot read — always suspend
suspend fun insertX()            // write
suspend fun updateX()            // write
suspend fun deleteX() / softDeleteX()  // delete (prefer soft)

// UseCase:
operator fun invoke(...): Flow<X>       // for streaming
suspend operator fun invoke(...): Result<X>  // for actions that can fail
fun invoke(...): X               // for pure computation (CalculateChangeUseCase)

// ViewModel:
fun on{EventName}(...)           // handles user actions: onAddItem, onConfirmOrder
private fun load{Data}()         // internal data loading
```

### 4.3 Variables

```kotlin
// StateFlow pattern:
private val _uiState = MutableStateFlow(FooUiState())
val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

private val _uiEffect = MutableSharedFlow<FooUiEffect>(replay = 0)
val uiEffect: SharedFlow<FooUiEffect> = _uiEffect.asSharedFlow()

// Room query results (in DAO):
fun observeOpenBills(): Flow<List<BillEntity>>  // always plural for lists
suspend fun getBillById(id: String): BillEntity?  // nullable for single lookups

// Sync fields on entities (always this exact naming):
val updatedAt: Long
val syncStatus: String   // stored as enum name string
val deviceId: String
```

### 4.4 Constants and Enums

```kotlin
// Enums stored in Room as String names (never ordinals):
enum class BillStatus { OPEN, PAID, VOID }
// TypeConverter: BillStatus.name ↔ String (not BillStatus.ordinal)

// Route constants:
object Routes {
    const val ORDER = "order"
    const val TABLES = "tables"
    const val REPORTS = "reports"
    const val SHIFT_OPEN = "shift/open"
}
```

---

## 5. Coding Standards

### 5.1 Coroutines

```kotlin
// ✅ Use viewModelScope for ViewModel coroutines
viewModelScope.launch { ... }
viewModelScope.launch(Dispatchers.IO) { ... }  // for heavy operations

// ✅ IO dispatcher for all Room and Firebase operations
withContext(Dispatchers.IO) { dao.insert(entity) }

// ✅ StateFlow conversion
useCase().stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = emptyList()
)

// ❌ Never use GlobalScope
GlobalScope.launch { }  // FORBIDDEN

// ❌ Never block a coroutine with runBlocking in production code
runBlocking { dao.insert(entity) }  // FORBIDDEN in production
```

### 5.2 Result Type

All suspend functions that can fail return `Result<T>`:

```kotlin
// ✅ CORRECT
suspend fun processPayment(billId: String, payments: List<Payment>): Result<Unit>

// In implementation:
return try {
    // do work
    Result.success(Unit)
} catch (e: BillAlreadyPaidException) {
    Result.failure(e)
}

// In ViewModel:
viewModelScope.launch {
    processPaymentUseCase(billId, payments)
        .onSuccess { /* update state */ }
        .onFailure { e -> /* show error */ }
}

// ❌ WRONG — throwing from a suspend function that callers won't wrap
suspend fun processPayment(...) { throw BillAlreadyPaidException() }  // forces try-catch everywhere
```

### 5.3 Flow in DAOs

```kotlin
// ✅ CORRECT — observe methods return Flow (no suspend)
@Query("SELECT * FROM bills WHERE status = 'OPEN'")
fun observeOpenBills(): Flow<List<BillEntity>>

// ✅ CORRECT — one-shot reads are suspend
@Query("SELECT * FROM bills WHERE id = :id")
suspend fun getById(id: String): BillEntity?

// ❌ WRONG — observe method as suspend (loses reactivity)
@Query("SELECT * FROM bills WHERE status = 'OPEN'")
suspend fun getOpenBills(): List<BillEntity>  // not reactive, stale data

// ❌ WRONG — one-shot read as Flow (unnecessary, doesn't emit updates)
@Query("SELECT * FROM bills WHERE id = :id")
fun getById(id: String): Flow<BillEntity?>  // overkill for point lookups
```

### 5.4 callbackFlow for Firebase

```kotlin
// ✅ CORRECT — always close the channel and remove listener
fun observe(path: String): Flow<DataSnapshot> = callbackFlow {
    val ref = database.getReference(path)
    val listener = ref.addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot) }
        override fun onCancelled(error: DatabaseError) { close(error.toException()) }
    })
    awaitClose { ref.removeEventListener(listener) }
}

// ❌ WRONG — listener leaked, no awaitClose
fun observe(path: String): Flow<DataSnapshot> = flow {
    val ref = database.getReference(path)
    ref.addValueEventListener(...)  // listener never removed
}
```

### 5.5 Kotlin Features to Use

```kotlin
// ✅ Use data classes for domain models and UiState
data class OrderUiState(val isLoading: Boolean = false, ...)

// ✅ Use sealed classes for UiEffect and UiEvent
sealed class OrderUiEffect {
    data class NavigateToPayment(val billId: String) : OrderUiEffect()
    object OrderConfirmed : OrderUiEffect()
}

// ✅ Use value classes for type safety
@JvmInline value class Rupiah(val value: Long)

// ✅ Use extension functions for mappers
fun BillEntity.toDomain(items: List<OrderItem>, payments: List<Payment>): Bill = ...
fun Bill.toEntity(): BillEntity = ...

// ✅ Prefer expression bodies for simple functions
fun BillEntity.isOverdue(): Boolean = 
    System.currentTimeMillis() - createdAt > 12 * 60 * 60 * 1000L

// ✅ Use scope functions appropriately
val entity = BillEntity(...).also { it.syncStatus = SyncStatus.PENDING.name }
```

### 5.6 Null Safety

```kotlin
// ✅ Prefer safe call + Elvis
val tableLabel = tableEntity?.label ?: "Meja"

// ✅ Use let for null-checking blocks
openShift?.let { shift ->
    // use shift safely
}

// ❌ Avoid !! except in tests or when guaranteed non-null by context
val shift = getOpenShift()!!  // avoid — use let or requireNotNull with message
```

---

## 6. Compose Guidelines

### 6.1 Stateless Composables

```kotlin
// ✅ CORRECT — stateless, receives state and callbacks
@Composable
fun OrderScreen(
    uiState: OrderUiState,
    onAddItem: (MenuItem) -> Unit,
    onConfirmOrder: () -> Unit,
) { ... }

// In the route:
@Composable
fun OrderRoute(viewModel: OrderViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OrderScreen(
        uiState = uiState,
        onAddItem = viewModel::onAddItem,
        onConfirmOrder = viewModel::onConfirmOrder,
    )
}

// ❌ WRONG — composable holds ViewModel reference and calls it directly in deep children
@Composable
fun MenuItemCard(viewModel: OrderViewModel) {  // never pass ViewModel to children
    Button(onClick = { viewModel.onAddItem(...) }) { ... }
}
```

### 6.2 State Collection

```kotlin
// ✅ CORRECT — lifecycle-aware collection
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

// ❌ WRONG — not lifecycle-aware, leaks collection in background
val uiState by viewModel.uiState.collectAsState()
```

### 6.3 One-Shot Effects

```kotlin
// ✅ CORRECT — collect effects in LaunchedEffect
LaunchedEffect(Unit) {
    viewModel.uiEffect.collect { effect ->
        when (effect) {
            is OrderUiEffect.NavigateToPayment -> navController.navigate(Routes.payment(effect.billId))
            OrderUiEffect.OrderConfirmed -> snackbarHostState.showSnackbar(context.getString(R.string.order_confirmed))
        }
    }
}

// ❌ WRONG — collecting effects in composable body (re-runs on every recomposition)
viewModel.uiEffect.collect { ... }  // not in LaunchedEffect
```

### 6.4 No Business Logic in Composables

```kotlin
// ✅ CORRECT — all logic in ViewModel
@Composable
fun CartPanel(cart: List<CartItemUi>, onIncrement: (String) -> Unit, ...) {
    // only rendering logic here
}

// ❌ WRONG — business logic in composable
@Composable
fun CartPanel(cartItems: List<CartItem>) {
    val total = cartItems.sumOf { it.lineTotal.value }  // calculation in composable
    val isValid = total > 0 && activeShift != null      // business rule in composable
}
```

### 6.5 Theme Tokens

```kotlin
// ✅ CORRECT
Text(color = MaterialTheme.colorScheme.onSurface)
Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface))

// ❌ WRONG
Text(color = Color(0xFF333333))  // hardcoded color
Card(colors = CardDefaults.cardColors(containerColor = Color.White))
```

### 6.6 String Resources

```kotlin
// ✅ CORRECT
Text(stringResource(R.string.menu_sold_out_badge))
Button(onClick = ...) { Text(stringResource(R.string.order_confirm_button)) }

// ❌ WRONG
Text("Habis")           // hardcoded Indonesian
Text("Sold Out")        // hardcoded English
```

### 6.7 Bottom Sheets and Dialogs

- Bottom sheets use `ModalBottomSheet` from Material 3
- Dialogs use `AlertDialog` from Material 3
- Both are driven by state (e.g. `showVariantSheet: Boolean` in UiState), not by imperative show/hide calls
- Dismissal calls a ViewModel event (e.g. `onDismissVariantSheet()`)

---

## 7. State Management Rules

### 7.1 ViewModel Structure — Every ViewModel Follows This Pattern

```kotlin
@HiltViewModel
class FooViewModel @Inject constructor(
    private val somUseCase: SomeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FooUiState())
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<FooUiEffect>(extraBufferCapacity = 1)
    val uiEffect: SharedFlow<FooUiEffect> = _uiEffect.asSharedFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            someUseCase().collect { data ->
                _uiState.update { it.copy(items = data) }
            }
        }
    }

    fun onSomeEvent(param: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            someUseCase(param)
                .onSuccess { _uiEffect.emit(FooUiEffect.NavigateToNext) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
        }
    }
}
```

### 7.2 UiState Rules

- Every screen has its own `*UiState` data class. Never share UiState between screens.
- Default values must be provided for all fields so the screen renders in an empty/loading state on first emission.
- Boolean flags like `isLoading`, `isSubmitting` prevent double-taps on action buttons.
- Error state is a `String?` — null means no error.
- Role-dependent visibility (e.g. `canVoidBill: Boolean`) is computed in the ViewModel, not in the Composable.

```kotlin
data class PaymentUiState(
    val bill: BillDetailUi? = null,
    val enabledPaymentMethods: List<PaymentMethodUi> = emptyList(),
    val payments: List<PaymentRowUi> = emptyList(),
    val remainingBalance: Rupiah = Rupiah.ZERO,
    val canComplete: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null,
)
```

### 7.3 Cart State — Special Rule

The cart **exclusively** lives in `OrderViewModel` as in-memory state:

```kotlin
// ✅ CORRECT — cart is in-memory ViewModel state
private val _cart = MutableStateFlow<List<CartItem>>(emptyList())

// The cart is NEVER:
// - Written to Room until ConfirmOrderUseCase is called
// - Observed from Room
// - Synced to RTDB
// - Passed between ViewModels
```

Navigating away from the Order screen (ViewModel cleared) clears the cart. This is intentional.

### 7.4 Role Gate

Role gates are applied at two levels, never just one:

1. **Navigation level:** The route composable in `AppNavGraph` checks role before rendering. Staff navigating to an owner-only route is redirected.
2. **Domain level:** Use cases like `VoidBillUseCase` and `CloseShiftUseCase` check the role parameter and return `Result.failure(InsufficientPermissionsException)` for staff.

```kotlin
// In AppNavGraph:
composable(Routes.REPORTS) {
    if (userRole != UserRole.OWNER) {
        LaunchedEffect(Unit) { navController.navigate(Routes.ORDER) }
        return@composable
    }
    ReportRoute()
}

// In VoidBillUseCase:
if (userRole != UserRole.OWNER) return Result.failure(InsufficientPermissionsException())
```

---

## 8. Repository Pattern Rules

### 8.1 Interface (in `domain/repository/`)

```kotlin
// ✅ CORRECT interface
interface BillRepository {
    fun observeOpenBills(): Flow<List<Bill>>              // observe = Flow
    suspend fun getBillById(id: String): Bill?            // one-shot = suspend
    suspend fun createBill(bill: Bill, items: List<OrderItem>)  // write = suspend, no return needed
    suspend fun processPayment(billId: String, payments: List<Payment>)  // write
}

// ❌ WRONG — domain interface exposes Room or Firebase types
interface BillRepository {
    fun observeOpenBills(): Flow<List<BillEntity>>        // exposes Room entity
    fun observeOpenBills(): Flow<DataSnapshot>            // exposes Firebase type
}
```

### 8.2 Implementation (in `data/repository/`)

The implementation must follow this exact write sequence:

```kotlin
// ✅ CORRECT write sequence
override suspend fun createBill(bill: Bill, items: List<OrderItem>) {
    // 1. Map domain → entity
    val billEntity = bill.toEntity().copy(
        syncStatus = SyncStatus.PENDING.name,
        updatedAt = System.currentTimeMillis(),
        deviceId = sessionManager.deviceId,
    )
    val itemEntities = items.map { it.toEntity(syncStatus = SyncStatus.PENDING) }

    // 2. Write to Room (immediate, optimistic)
    withContext(Dispatchers.IO) {
        billDao.insert(billEntity)
        orderItemDao.insertAll(itemEntities)
    }

    // 3. Signal sync (async, does not block caller)
    syncCoordinator.notifyPendingSync()
}
```

### 8.3 What Repositories Must Never Do

- Never call RTDB directly — all Firebase interaction goes through `SyncCoordinator`
- Never expose Room entity types in return values or parameters
- Never expose Firebase types (`DataSnapshot`, `DatabaseReference`) in return values
- Never hold business logic — that belongs in UseCases
- Never call one repository from another — compose in UseCases

### 8.4 Transaction Rule

Payment processing uses a Room `@Transaction`. Whenever a write touches multiple tables atomically, use a `@Transaction`-annotated DAO method:

```kotlin
// In PaymentDao or a composite DAO:
@Transaction
suspend fun processPaymentTransaction(
    payments: List<PaymentEntity>,
    billId: String,
    status: String,
    paidAt: Long,
) {
    insertAll(payments)
    updateBillStatus(billId, status, paidAt)
}
```

---

## 9. UseCase Pattern Rules

### 9.1 Structure — One Class, One Invoke

```kotlin
// ✅ CORRECT
class ConfirmOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val shiftRepository: ShiftRepository,
) {
    suspend operator fun invoke(
        cartItems: List<CartItem>,
        destination: OrderDestination,
        operatorId: String,
    ): Result<Unit> {
        // Validate ALL preconditions first
        if (cartItems.isEmpty()) return Result.failure(EmptyCartException())
        val activeShift = shiftRepository.getOpenShift()
            ?: return Result.failure(ShiftNotOpenException())
        // ... more validation ...

        // Build domain objects
        // ...

        // Call repository
        return runCatching {
            orderRepository.confirmOrder(bill, orderItems)
        }
    }
}
```

### 9.2 UseCase Rules

- **One class, one `operator fun invoke()`** — no methods named anything other than `invoke`.
- **Validate first, act second** — all precondition checks happen at the top of `invoke`, before any repository calls.
- **Return `Result<T>` for actions, `Flow<T>` for streaming reads.** Pure computation UseCases (like `CalculateChangeUseCase`) can return the value directly.
- **No Android imports** — UseCases are pure Kotlin. The only allowed import from the Kotlin stdlib or coroutines library.
- **Do not call other UseCases** — if multiple use cases are needed, the ViewModel orchestrates them, or a new composite UseCase is created (but this is rare).
- **Do not hold mutable state** — UseCases are stateless. All state lives in Room or in ViewModels.

### 9.3 Business Rules Live Here

UseCases are where every business rule from the PRD is enforced:

| PRD Rule | Enforced in |
|----------|-------------|
| Shift must be open before taking orders | `ConfirmOrderUseCase` |
| Bill status only moves forward | `ProcessPaymentUseCase`, `VoidBillUseCase` |
| Void entire bill = owner only | `VoidBillUseCase` |
| Void item requires reason; OTHER requires note | `VoidOrderItemUseCase` |
| Shift close blocked by open bills | `CloseShiftUseCase` |
| Shift close = owner only | `CloseShiftUseCase` |
| Cash tendered >= amount for cash payment | `ProcessPaymentUseCase` |
| Required variants must be fulfilled | `ConfirmOrderUseCase` |
| Sold-out prompt on shift open | `OpenShiftUseCase` + `CheckSoldOutItemsUseCase` |

---

## 10. Data Layer Rules — Room

### 10.1 Entity Rules

Every Room entity must have these sync fields (except `ZReportEntity` which is immutable):

```kotlin
@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    // ... business fields ...

    // Sync metadata — REQUIRED on every entity except ZReportEntity
    val updatedAt: Long = 0L,
    val syncStatus: String = SyncStatus.PENDING.name,
    val deviceId: String = "",
)
```

`ZReportEntity` explicitly has NO sync fields — it is write-once and never synced from RTDB.

### 10.2 Enum Storage

```kotlin
// ✅ CORRECT — store as name string
class Converters {
    @TypeConverter fun toBillStatus(value: String) = enumValueOf<BillStatus>(value)
    @TypeConverter fun fromBillStatus(status: BillStatus) = status.name
}

// ❌ WRONG — ordinal breaks if enum order changes
@TypeConverter fun toBillStatus(value: Int) = BillStatus.values()[value]
```

### 10.3 Foreign Keys

All FK columns must have explicit `@ForeignKey` and `@Index`:

```kotlin
@Entity(
    tableName = "order_items",
    foreignKeys = [ForeignKey(
        entity = BillEntity::class,
        parentColumns = ["id"],
        childColumns = ["billId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("billId"), Index("menuItemId")],
)
data class OrderItemEntity(...)
```

### 10.4 Stock Entities — Schema Before Features

Stock-related entities (`StockItemEntity`, `StockBatchEntity`, `MenuItemIngredientEntity`, `StockOpnameEntity`, `StockOpnameLineEntity`) are included in `WarungDatabase` from version 1, even though their use cases and screens are built in PRD Phase 2. **Do not add them in a migration.** They go in the initial schema.

### 10.5 Migrations

If the schema must change after version 1, write an explicit `Migration(from, to)` object. Never use `fallbackToDestructiveMigration()` — it wipes all user data.

---

## 11. Data Layer Rules — Firebase

### 11.1 RTDB Path Structure

All RTDB paths must match `warung-pos-architecture.md` Appendix C exactly:

```
/appConfig/minVersionCode
/appConfig/openShiftId
/bills/{uuid}
/orderItems/{uuid}
/payments/{uuid}
/menuItems/{uuid}
/menuCategories/{uuid}
/variantGroups/{uuid}
/variantOptions/{uuid}
/tables/{uuid}
/paymentMethods/{uuid}
/shifts/{uuid}
/expenses/{uuid}
/stockItems/{uuid}         ← Phase 2
/stockBatches/{uuid}       ← Phase 2
/opnames/{uuid}            ← Phase 2

Index nodes:
/openBillsByTable/{tableId}/{billId}: true
/billsByShift/{shiftId}/{billId}: true
```

### 11.2 Firebase Data Source Contract

`FirebaseRtdbDataSource` methods always return `Result<T>`. They never throw to callers:

```kotlin
// ✅ CORRECT
suspend fun write(path: String, value: Any): Result<Unit> = runCatching {
    database.getReference(path).setValue(value).await()
}

// ❌ WRONG — unhandled exception propagates to caller
suspend fun write(path: String, value: Any) {
    database.getReference(path).setValue(value).await()  // can throw
}
```

### 11.3 Atomic Operations

These specific operations use RTDB atomic primitives, not regular writes:

| Operation | Method |
|-----------|--------|
| Bill OPEN → PAID status transition | `runTransaction()` |
| Stock quantity decrement | `ServerValue.increment(-n)` |
| Single open shift enforcement | `runTransaction()` on `/appConfig/openShiftId` |

---

## 12. Sync Rules

### 12.1 SyncCoordinator Contract

```kotlin
// After EVERY Room write in any repository:
syncCoordinator.notifyPendingSync()  // always called, no exceptions

// notifyPendingSync() does:
// → Enqueues SyncWorker as OneTimeWorkRequest with NetworkType.CONNECTED constraint
// → Uses ExistingWorkPolicy.APPEND_OR_REPLACE (no duplicate workers)
```

### 12.2 Conflict Resolution Logic

`ConflictResolver` implements last-write-wins (LWW):

```
incoming.updatedAt > room.updatedAt  → ACCEPT (write incoming to Room)
incoming.updatedAt <= room.updatedAt → REJECT (local is at least as fresh)
incoming.status would reverse PAID → OPEN or VOID → anything → always REJECT
no existing Room record → always ACCEPT (new entity from remote)
```

### 12.3 SyncWorker Ordering

When flushing pending records, always process in `updatedAt ASC` order. This preserves the logical order of writes (older writes processed before newer writes).

---

## 13. Testing Requirements

### 13.1 What to Test (Priority Order)

| Priority | What | Where |
|----------|------|-------|
| 🔴 Critical | Business-critical UseCases: `ConfirmOrderUseCase`, `ProcessPaymentUseCase`, `CloseShiftUseCase`, `VoidBillUseCase` | `test/` (JVM) |
| 🔴 Critical | `ConflictResolver` — all conflict scenarios | `test/` (JVM) |
| 🔴 Critical | All Mappers — round-trip tests | `test/` (JVM) |
| 🟡 Important | `BillDao`, `ReportQueryDao` — SQL correctness | `androidTest/` |
| 🟡 Important | ViewModels — UiState transitions | `test/` (JVM) |
| 🟡 Important | 3 critical UI flows (see below) | `androidTest/` |
| 🟢 Optional | Other UseCases, other DAOs | as time permits |

### 13.2 The 3 Required UI Tests

1. Grab-and-go order → cash payment → cart cleared
2. Open bill on table → add items in two rounds → pay
3. Shift close blocked by open bill → close bill → shift close succeeds

Do not write UI tests for every screen. These 3 cover the highest-risk paths.

### 13.3 Test Structure

```kotlin
// UseCase unit test — use fake repositories, not MockK
class ConfirmOrderUseCaseTest {
    private val fakeShiftRepository = FakeShiftRepository()
    private val fakeOrderRepository = FakeOrderRepository()
    private val useCase = ConfirmOrderUseCase(fakeOrderRepository, fakeShiftRepository)

    @Test
    fun `empty cart returns EmptyCartException`() = runTest {
        val result = useCase(emptyList(), OrderDestination.GrabAndGo, "operator-1")
        assertTrue(result.exceptionOrNull() is EmptyCartException)
    }
}

// DAO integration test — in-memory Room database
@RunWith(AndroidJUnit4::class)
class BillDaoTest {
    private lateinit var db: WarungDatabase
    private lateinit var dao: BillDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WarungDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.billDao()
    }

    @After
    fun teardown() { db.close() }
}

// ViewModel test — use TestCoroutineScheduler
@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {
    @get:Rule val coroutineRule = MainDispatcherRule()

    @Test
    fun `adding item increments cart quantity`() = runTest {
        val viewModel = OrderViewModel(FakeGetMenuItemsUseCase(), ...)
        viewModel.onAddItem(testMenuItem)
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.cart.first().quantity)
        }
    }
}
```

### 13.4 Fake vs Mock

Prefer **Fake implementations** over MockK for repositories:

```kotlin
// ✅ PREFERRED — Fake (clean, readable, reusable)
class FakeShiftRepository : ShiftRepository {
    var openShift: Shift? = null
    override suspend fun getOpenShift(): Shift? = openShift
    override fun observeOpenShift(): Flow<Shift?> = flowOf(openShift)
    override suspend fun openShift(shift: Shift) { openShift = shift }
    // ...
}

// 🟡 ACCEPTABLE — MockK for complex setup or when Fake is more work
val mockRepository = mockk<ShiftRepository>()
coEvery { mockRepository.getOpenShift() } returns null
```

Use MockK for `FirebaseRtdbDataSource` and `SyncCoordinator` where Fakes would be complex.

### 13.5 Flow Testing with Turbine

```kotlin
// ✅ CORRECT — use Turbine for Flow assertions
viewModel.uiState.test {
    val initial = awaitItem()
    assertFalse(initial.canComplete)
    
    viewModel.onAddPayment(cashMethod, Rupiah(50000L))
    
    val updated = awaitItem()
    assertTrue(updated.canComplete)
    cancelAndIgnoreRemainingEvents()
}
```

---

## 14. Dependency Injection Rules

### 14.1 Module Locations

| Module | Location | Provides |
|--------|----------|---------|
| `AppModule` | `core/di/AppModule.kt` | `WarungDatabase`, all DAOs, `FirebaseDatabase`, `FirebaseAuth`, `FirebaseRtdbDataSource`, `FirebaseAuthDataSource`, `SyncCoordinator` |
| `RepositoryModule` | `core/di/RepositoryModule.kt` | `@Binds` for every repository interface → implementation |
| `WorkerModule` | `core/di/WorkerModule.kt` | `WorkManagerConfiguration` with `HiltWorkerFactory` |

### 14.2 WorkManager + Hilt

WorkManager initialization must be manual when using Hilt workers. Remove the default WorkManager `<provider>` from `AndroidManifest.xml` and provide `WorkManagerConfiguration` manually:

```xml
<!-- In AndroidManifest.xml -->
<provider
    android:name="androidx.startup.InitializationProvider"
    tools:node="remove" />  <!-- ← removes default WorkManager initializer -->
```

```kotlin
// In AppModule:
@Provides
fun provideWorkManagerConfiguration(workerFactory: HiltWorkerFactory): Configuration =
    Configuration.Builder().setWorkerFactory(workerFactory).build()
```

### 14.3 Scope Rules

```kotlin
// ✅ Application-scoped singletons (most services):
@Provides @Singleton fun provideWarungDatabase(...): WarungDatabase
@Provides @Singleton fun provideBillDao(db: WarungDatabase): BillDao = db.billDao()

// ✅ ViewModel-scoped (use cases if needed, but prefer Singleton):
@HiltViewModel class OrderViewModel @Inject constructor(useCase: ConfirmOrderUseCase)

// ❌ Never provide Firebase instances as non-singletons (creates multiple connections)
@Provides fun provideFirebaseDatabase() = FirebaseDatabase.getInstance()  // missing @Singleton
```

---

## 15. Internationalisation (i18n) Rules

### 15.1 The Core Rule

Every string added in any commit must appear in both files simultaneously:
- `res/values/strings.xml` — English (default)
- `res/values-id/strings.xml` — Bahasa Indonesia

### 15.2 Placeholder Convention

If the Indonesian translation is not yet confirmed, use a `[TODO-ID]` placeholder rather than leaving the string missing:

```xml
<!-- In values-id/strings.xml -->
<string name="order_confirm_button">[TODO-ID: Confirm Order]</string>
```

This makes missing translations visibly obvious in the app rather than silently falling back to English.

### 15.3 No Hardcoded Strings — Anywhere

Applies to:
- All Compose `Text()` content
- All content descriptions (`contentDescription = stringResource(...)`)
- All SnackBar messages
- All Dialog titles and bodies
- All Button labels
- All placeholder/hint text on `TextField`

Does NOT apply to:
- Logging strings (`Log.d("tag", "Internal debug message")`)
- Test assertion messages
- Technical identifiers (route names, Firebase paths, enum names)

### 15.4 Monetary Formatting

```kotlin
// ✅ CORRECT — always use CurrencyFormatter
CurrencyFormatter.format(15000L)  // → "Rp 15.000"
CurrencyFormatter.format(rupiah.value)  // → "Rp 15.000"

// ❌ WRONG
"Rp ${total}"       // no thousand separator
"Rp ${"%.0f".format(total)}"  // Float formatting
```

---

## 16. Claude Code Behavior Rules

These rules govern how Claude Code should operate in sessions, not just what code to write.

### 16.1 Before Writing Any Code

1. Read the task description and its DoD from `warung-pos-roadmap.md`
2. Identify the task's package location from `warung-pos-architecture.md` Section 3
3. Check if the file already exists before creating it
4. Check what dependencies the task requires (are they already built?)

### 16.2 File Creation Rules

- **Always create files in the exact package specified in the architecture doc.** Do not create new packages without explicit instruction.
- **One class per file.** No exceptions for production code (tests may have multiple helper classes).
- **File name = class name.** `BillRepositoryImpl.kt` contains only `BillRepositoryImpl`.
- **Never overwrite existing files without showing the diff first.**

### 16.3 When to Stop and Ask

Stop and ask the owner before proceeding when:
- A business rule is ambiguous and not covered by the PRD
- A new dependency would need to be added that isn't in the approved list
- Two existing files need to be modified to complete a task and the impact is unclear
- A migration would be required for a schema change

Do not stop and ask for:
- Standard Kotlin syntax choices
- Method ordering within a file
- Which IDE formatter to use
- Standard Compose patterns that follow the rules in this file

### 16.4 Commit Granularity

Each task from `warung-pos-roadmap.md` should be one commit. Commit message format:
```
P2-009: WarungDatabase with all entities

- Added @Database class listing all 19 entities
- Version 1, no destructive migration fallback
- Includes stock entities (schema-only, no use cases)
```

### 16.5 DoD Verification

Before marking a task complete, verify every checklist item in the task's Definition of Done from `warung-pos-roadmap.md`. Do not mark a task done if any DoD item is unchecked.

### 16.6 What to Never Generate

- **Never generate Retrofit interfaces.** There is no REST API.
- **Never generate Firestore queries.** This app uses RTDB.
- **Never generate image loading code.** There are no images.
- **Never generate hardcoded string literals in Composables.**
- **Never generate autoincrement integer PKs for entities.**
- **Never generate Float or Double fields for monetary values.**
- **Never add a new Gradle dependency without noting it is not in the approved list.**

---

## 17. Quick Reference — Anti-Pattern Checklist

Before submitting any code, verify none of these anti-patterns are present:

```
□ Float or Double used for any monetary value
□ Hardcoded string in any Composable or ViewModel
□ Room entity returned from a repository method (should be domain model)
□ Firebase type (DataSnapshot, DatabaseReference) in a repository interface
□ Android imports in any file under domain/ package
□ RTDB whole-object write (should be field-level path write)
□ Entire bill object written to RTDB (should be field-level update)
□ collectAsState() used instead of collectAsStateWithLifecycle()
□ ViewModel passed as parameter to a child Composable
□ Autoincrement Int primary key on a Room entity
□ Business logic (calculation, validation) inside a Composable function
□ GlobalScope used anywhere in production code
□ runBlocking used in production code (test code is OK)
□ Retrofit, Gson, Glide, Coil, Firestore, Firebase Storage imported anywhere
□ KAPT used instead of KSP
□ Cart written to Room before operator confirms the order
□ SyncCoordinator.notifyPendingSync() omitted after a repository write
□ Enum stored by ordinal instead of name in Room
□ ZReportEntity given syncStatus/updatedAt/deviceId fields (it must not have these)
□ Stock entities missing from WarungDatabase @Database entities list
□ Missing @Index on a foreign key column in a Room entity
□ Missing strings.xml entry in either English or Indonesian for a new string
```

---

## 18. Session Startup Checklist

At the start of every Claude Code session:

1. Read this file
2. Identify which roadmap phase and task is being worked on
3. Confirm the task's dependencies are complete
4. Identify the exact file paths from the architecture package structure
5. Check the current state of `WarungDatabase` if any entity changes are involved
6. Proceed with implementation
