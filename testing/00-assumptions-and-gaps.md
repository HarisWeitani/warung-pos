# 00 — Assumptions, Ambiguities & PRD-vs-Implementation Gaps

**Read this file first.** The PRD (`claude_requirement/warung-pos-prd.md`) and Architecture doc describe an
intended design. The **shipped code is the source of truth** for this test suite. Where the code and the
PRD disagree, the test cases assert **the code's actual behaviour** and separately flag the divergence as a
defect candidate (severity noted). This lets QA verify what the app *does*, while giving the product owner a
list of PRD requirements that were never implemented or were implemented differently.

All findings below were confirmed by reading the code under
`app/src/main/java/com/wfx/warungpos/` on branch `main` (versionCode 1, versionName "1.0",
minSdk 26, targetSdk 36, DB schema version 3).

---

## A. Confirmed environment facts

| Fact | Value | Source |
|------|-------|--------|
| Package / applicationId | `com.wfx.warungpos` | `app/build.gradle.kts` |
| minSdk / targetSdk / compileSdk | 26 / 36 / 37 | `app/build.gradle.kts` |
| versionCode / versionName | 1 / "1.0" | `app/build.gradle.kts` |
| Orientation | Portrait only (per PRD; not explicitly locked in manifest — verify) | PRD OQ-2 |
| Room DB name | see `AppModule` | `core/di/AppModule.kt` |
| Room schema version | 3 | `data/local/db/WarungDatabase.kt` |
| Default language | `id` (Bahasa Indonesia) | `AppPreferences.DEFAULT_LANGUAGE` |
| Bottom nav destinations | Order, Reports, More (Reports gated by role==OWNER, but role is always OWNER) | `WarungPosApp.kt` |
| Money type | `Long` (integer Rupiah) everywhere | entities/use cases |
| All PKs | client-generated String UUID (except payment methods, which use fixed IDs) | `UuidGenerator`, `FirstRunManager` |

---

## B. AUTHENTICATION — MAJOR deviation from PRD (verified)

The PRD's entire `FR-AUTH` section (Firebase email/password, owner vs staff accounts, custom claims, role
permission matrix) is **NOT implemented**. Actual behaviour:

- **No remote user login.** First launch shows a **"Set up your PIN"** screen: enter a **username** + a
  **PIN (min 4 digits)** + confirm PIN. Stored in `EncryptedSharedPreferences` (PIN is SHA-256 hashed).
  Source: `feature/auth/PinViewModel.kt`, `core/common/SessionManager.kt`.
- Subsequent cold starts show a **PIN-only unlock** screen ("Hi, {username}").
- **Role is hardcoded `UserRole.OWNER`** for the single local user (`SessionManager._userRole`). Every
  owner-only guard therefore always passes. The `STAFF`/`NONE` roles exist in the enum but are never assigned.
- Firebase **Anonymous** auth runs in the background solely to satisfy RTDB security rules
  (`FirebaseAuthDataSource.ensureSignedIn()`), invoked from `AppViewModel.init`.

**Consequence for testing:** All "staff cannot do X" / "owner-only" permission test cases from the PRD are
**Not Applicable** to the shipped app and are marked as such. The role-based permission matrix is untestable
because there is only one role. This is the single largest scope gap and must be confirmed with the product
owner. (**Assumption A1:** the single-role, local-PIN model is the intended shipped behaviour, and the PRD's
FR-AUTH is superseded — consistent with `README.md` and `docs/firebase-setup.md`.)

---

## C. ORDER FLOW — deviation from PRD cart model (verified)

PRD/architecture describe an in-memory cart, `+/-` quantity steppers, a "Confirm Order" button, and a
`ConfirmOrderUseCase` that validates required variants and a non-empty cart. **The shipped UI does not use
this flow.** `ConfirmOrderUseCase` is **dead code — not referenced by any ViewModel** (verified by grep).

Actual shipped order flow:
1. **Order tab** (`OrderScreen`) lists **open bills** (cards) with a **`+` FAB**. Title is "Orders".
   Empty state text: "No open orders" / "Tap + to create a new order".
2. Tapping `+` calls `OrderViewModel.createBill()` which **immediately creates an empty `OPEN` bill**
   (grandTotal 0, `sessionLabel = "Counter - {HH:mm}"`) and navigates to **Bill Detail**.
3. **Bill Detail** (`BillDetailScreen`) shows current order items on top and a **menu picker** below
   (category filter chips + item rows). Tapping a menu item:
   - with **no variant groups** → adds qty 1; tapping the same no-variant line again **increments quantity**.
   - with **variant groups** → opens a **variant bottom sheet**; on confirm, always adds a **new line** (qty 1).
4. There is **no `+/-` quantity stepper** on order items and **no way to decrement** an item's quantity in the
   UI. The only removal is the **trash/void icon** (which requires a void reason).
5. **"Pay"** button in the bottom bar (enabled only when ≥1 active item) → Payment screen.

**Assumption A2:** the bill-first (no-cart) flow is the intended behaviour. Test cases assert it; the missing
`+/-` steppers and dead `ConfirmOrderUseCase` are flagged as low-severity divergences.

---

## D. Other confirmed deviations / unimplemented PRD features

| # | PRD requirement | Actual shipped behaviour | Severity of gap |
|---|-----------------|--------------------------|-----------------|
| D-1 | FR-ORDER-3: sold-out items greyed & non-tappable in order screen | `observeAvailable()` filters only `isAvailable=1`; **sold-out (`isSoldOut`) items still appear and remain tappable/orderable** in the Bill Detail menu picker. No greying, no "Sold Out" badge. | High (can sell sold-out items) |
| D-2 | FR-PAYMENT-3: split payment across methods | **Not implemented in UI.** Payment screen allows exactly **one** method; `amount` is always the full `grandTotal`. (`ProcessPaymentUseCase` accepts multiple rows, but the UI never sends more than one.) | Medium |
| D-3 | FR-VOID-3: void an entire **paid or open** bill | `VoidBillUseCase` rejects any bill whose status ≠ OPEN (`BillNotVoidableException`). **A PAID bill cannot be voided.** | Medium |
| D-4 | FR-I18N-1/3: fully bilingual, all strings externalised | Only bottom-nav labels + version-gate use string resources. **Nearly all screen text is hardcoded English** (e.g. "Orders", "Pay", "Close Day", "More"). Switching language changes almost nothing visible. `strings.xml` has only ~50 keys; `values-id` mirrors them but most are unused. | High vs PRD; but app is personal-use |
| D-5 | FR-DAY-1a: "Reset all sold-out items?" prompt on new day | `CheckSoldOutItemsUseCase` / `ResetSoldOutItemsUseCase` exist but are **not wired to any screen**. **No prompt appears** on day rollover/open. | Medium |
| D-6 | FR-BILL-4: bill labels "Bill #1", "Bill #2" per session | Labels are **`"Counter - {HH:mm}"`** (time-stamped), not incrementing "#N". | Low |
| D-7 | FR-BILL-7: 12-hour open-bill soft warning (orange) | **Not implemented.** Bill cards show creation time only, no age warning. | Low |
| D-8 | FR-PAYMENT-4: payment methods rename & reorder | Settings only **toggles active/inactive**. **No rename, no reorder.** | Low |
| D-9 | FR-EXPENSE-2 categories `GAS, ELECTRICITY, PACKAGING, WAGES, RENT, OTHER` | Enum is `SUPPLIES, UTILITIES, SALARY, RENT, TRANSPORT, OTHER`. **Not editable** (Expense Categories screen is a read-only bilingual list). | Low (naming) |
| D-10 | FR-OPNAME-3 reasons `SPOILAGE, LOSS, THEFT, COUNTING_ERROR, OTHER` | Enum is `COUNT_ERROR, DAMAGE, THEFT, EXPIRY, OTHER`. | Low (naming) |
| D-11 | FR-REPORTS-5: PDF export | ~~CSV export only.~~ **Closed 2026-07-10** (commit `7410026`, "Add PDF export alongside CSV in Full Report share") — the Full Report export menu now offers both "Export as CSV" and "Export as PDF"; both confirmed working via the share sheet, including on an empty-range dataset (see `testing/results/EXECUTION_LOG.md` TC-RPT-020/022 retest notes). | Closed |
| D-12 | FR-REPORTS-3: per-item gross-margin (COGS) report | Report's `grossProfit = revenue − expenses` (**not** revenue − COGS). No per-item margin screen; COGS is never computed even when recipes exist. | Medium (mislabeled metric) |
| D-13 | FR-DAY-7: bill attributed to Day in which **PAID** | Bill's `shiftId` is set at **creation** and never updated at payment. Z-report/Close-Day totals are keyed on `shiftId` (creation day); Dashboard & date-range reports are keyed on `paidAt`. The two can disagree across a day boundary. | Medium |
| D-14 | FR-OPNAME-7: defer stock deductions during an active opname; replay post-commit | No deferral/queue logic exists. Payments during an open opname deduct stock immediately; the opname `submit` then **overwrites** `currentQuantity` with the counted value, discarding those concurrent deductions. | Medium |
| D-15 | MVP scope: Stock, Opname, full Reports, CSV export marked "out of MVP / Phase 2" | These are **implemented and reachable** in the shipped app (More → Stock / Stock Batches / Stock Opname; Reports → Dashboard → Full Report). Test them as live features. | N/A (extra scope) |
| D-16 | Day auto-close on calendar rollover | Only triggers when `EnsureDayOpenUseCase` runs (app cold start, or when creating a new bill). **No background timer** — if the app is simply left open past midnight with no action, the old day stays open until the next bill/restart. | Medium |
| D-17 | NFR-DISTRIBUTION version gate | Implemented, but if RTDB is unreachable **or** `appConfig/minVersionCode` missing, the gate defaults to **Allowed** (fails open). Also the gate check is non-blocking: the PIN screen shows while the check runs. | Info |
| D-18 | FR-MENU-1: category create/rename/reorder/soft-delete | **No in-app category creation UI.** `MenuRepository.saveCategory/deleteCategory` exist but are unwired. Item-edit only *selects* from existing categories; on a fresh install that list is empty, so new items are `categoryId=null` → grouped under `Uncategorized`. Named categories can only be created via RTDB/DB. | High (menu can't be organised in-app) |
| D-19 | Stock `lastCostPrice` upkeep | `ReceiveStockBatchUseCase` increments `currentQuantity` but does **not** update `StockItem.lastCostPrice`; any cost-impact/COGS basis that reads it may be stale. | Medium |
| D-20 | Menu item validation | `UpsertMenuItemUseCase` rejects **blank name** and **price ≤ 0** (Result.failure). Confirm the error is surfaced on `MenuItemEditScreen`; if the failure is silent, that is a separate UX defect. | Info |

---

## E. Ambiguities resolved by explicit assumption

| ID | Ambiguity | Assumption used by the tests |
|----|-----------|------------------------------|
| A3 | Whether a "Day" and a "Shift" are the same thing | They are the same entity (`ShiftEntity` / `Shift`), surfaced in the UI as **"Day"**. Routes/classes retain the `Shift` name. |
| A4 | Whether Firebase is configured in the test environment | Two variants: **(a) Firebase configured + online** (sync + version gate active); **(b) no Firebase / offline** (app fully functional locally, writes stay PENDING, version gate = Allowed). Each network/sync test states which variant it needs. |
| A5 | Multi-device testing feasibility | Requires the **same** `google-services.json` project on ≥2 devices with Anonymous auth enabled and the RTDB rules deployed. Multi-device cases are marked *Automation Candidate: No* (manual, 2 physical devices). |
| A6 | Amount input formatting | Tender/price/amount fields accept **digits only** (non-digits are stripped by the ViewModel). No thousands separators are typed; display uses `Rp` formatting. |
| A7 | "Change" for non-cash methods | The tender field is shown for **all** methods. For non-cash, leaving it blank makes tender default to `grandTotal` (change 0). Entering a tender < total on **any** method is rejected with `InsufficientTenderedAmountException`. |
| A8 | Currency display exact format | `CurrencyFormatter.format(x)` — verify exact prefix/grouping in `core/util/CurrencyFormatter.kt` before asserting literal strings; tests assert the **numeric value** and `Rp` prefix, not exact grouping characters, unless a case says otherwise. |
| A9 | Baseline BL-2 categories | Because there is no in-app category creation (D-18), the `Makanan`/`Minuman` categories in BL-2 must be **seeded via RTDB `/menuCategories` or the Room DB** before running category-dependent cases. Menu **items** can be created in-app (uncategorized) without this. |

---

## F. Known code-level risk hotspots to probe (from architecture analysis + code read)

These are not user stories but are high-value targets for negative/robustness testing:

1. **`SyncWorker.markSynced()` race** — after `writeMulti`, it re-queries `getPendingSync()` and flips those to
   SYNCED. A row written **between** `writeMulti` and `markSynced` can be marked SYNCED **without being pushed**
   → silent data loss on that field until its next edit. (See `SyncWorker.kt`.) Tests: rapid writes during sync.
2. **Firebase inbound deserialization** — `RtdbListener` uses reflection `getValue(Entity::class.java)`.
   A field-name/JSON mismatch or malformed `selectedVariantsJson` yields silent nulls → later crash in mapper.
3. **Optimistic bill totals** — totals are recomputed in the ViewModel/use case from active items; concurrent
   two-device edits to the same bill rely on append-only order items + LWW on the bill row.
4. **Change can be negative internally** — `CalculateChangeUseCase` returns `tendered − total` (may be < 0);
   UI clamps with `maxOf(0, …)`. Ensure no negative change is ever persisted/displayed.
5. **Payment on a zero-total bill** — UI disables Pay when there are no active items, but confirm the guard
   holds if the last item is voided while on the bill screen.
6. **Day rollover with open bills** — previous day stays OPEN; a stale-dated open day must still block manual
   close until bills are resolved, and must not double-generate Z-reports.
