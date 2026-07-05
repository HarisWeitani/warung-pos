# PRD: Warung POS — Personal Android Point-of-Sale for Indonesian Food Stall

**Document status:** Draft v1.1 — All open questions resolved  
**Author role:** Senior PM + Senior Android Architect  
**Date:** 2026-06-25  
**Audience:** Developer (owner) + Claude Code agent

---

## Resolved Decisions Log

| # | Question | Decision |
|---|----------|----------|
| OQ-1 | Min Android API | Follow existing project setup |
| OQ-2 | Orientation | Portrait only |
| OQ-3 | Language | Bilingual — Bahasa Indonesia + English (user-selectable) |
| OQ-4 | Firebase custom claims setup | Firebase already configured, google-services.json in place. Only Gradle setup needed. Claims set manually via Firebase Console by owner. |
| OQ-5 | Menu item images | No images — saves storage, no Firebase Storage dependency |
| OQ-6 | Open bills on shift close | Block shift close (Option A) — show list of open bills, owner must close them first |
| OQ-7 | Bills spanning shifts | Not applicable — bills are daily, always closed within the same operating day |
| OQ-8 | Calculator UX | Tap increments count by 1; +/- buttons to adjust. No numpad. |
| OQ-9 | Unnamed table identification | Auto-increment label per session (e.g. "Bill #1", "Bill #2") |
| OQ-10 | APK update distribution | Manual — owner builds APK, distributes via private channel (WhatsApp/Drive) |
| OQ-11 | Inactivity lock | Not needed |
| OQ-12 | Data export | CSV and PDF export both in scope |
| OQ-13 | Sold-out reset | Always manual — operator resets explicitly |

---

## Amendment Log

| # | Date | Change | Supersedes |
|---|------|--------|------------|
| AM-1 | 2026-07-04 | **Shift → Day model.** There is no manual "shift" concept. Operation is tracked as Day 1, Day 2, Day 3, etc. A day auto-opens with no input (no opening float prompt) and stays open until either (a) the owner manually closes it via a button in Settings, or (b) the calendar day changes, in which case the app auto-closes the previous day (generates its Z-report automatically, no counted-cash/variance step since no one manually counted) and auto-opens the next day. | FR-SHIFT-1, FR-SHIFT-1a (opening float removed), "Shift & Cash Drawer" user stories |
| AM-2 | 2026-07-04 | **No table numbers / no open bills.** Every order is pay-first, hand-over-food-after — like a fast food counter. Remove dine-in tables, the Tables tab, Table Settings, `OPEN_BILL` type, and the "select table / existing bill" destination options. Only one order flow remains: create order → pay → done. | FR-TABLE (all), FR-BILL-1/3/4, FR-ORDER destination selection, "Hybrid dine-in + grab-and-go" store profile line |

---

## 1. Product Overview

**Product name:** Warung POS (working title)  
**Type:** Personal-use Android application, distributed as APK via private channel (not Play Store)  
**Purpose:** A fully offline-capable point-of-sale system for a small Indonesian food stall (warung), replacing paper-based ordering and cash management with a digitized, multi-device workflow.

**Store profile:**
- Counter service only (no dine-in tables) — customer pays first, food is prepared/handed over after, like a fast food chain. See AM-2.
- 2 operators — both handle cashiering and cooking simultaneously
- 1–3 Android devices, portrait orientation
- Billing: always pay upfront, no open/running tab
- No receipt printer
- Payment methods: cash, QRIS (static sticker), GoPay, OVO, bank transfer — all configurable
- Bills are daily — always closed within the same operating day

**Tech stack (locked):**
- Android only, Jetpack Compose, portrait mode
- Room — offline-first local DB, single source of truth
- Firebase Realtime Database (RTDB) — background sync, free Spark tier
- Firebase Auth — email/password, 2 accounts (owner / staff), role via custom claim
- google-services.json already in project; Gradle setup is the remaining step
- No third-party payment gateway

---

## 2. Target Users

### Primary: Store Owner
- Android developer — can handle Firebase console and APK builds
- Full access: all features including reports, menu management, stock opname, shift close, void

### Secondary: Store Staff (1 person)
- Non-technical daily operator
- Scoped access: order taking, payment, sold-out toggle, expense logging, void item (with reason log)
- No access: financial reports, menu price editing, shift close, void entire bill

**Role permission matrix:**

| Action | Owner | Staff |
|--------|-------|-------|
| Take orders | ✅ | ✅ |
| Process payment | ✅ | ✅ |
| Mark item sold out / revert | ✅ | ✅ |
| Add expense | ✅ | ✅ |
| Void order item (with reason) | ✅ | ✅ (logged) |
| Void entire bill | ✅ | ❌ |
| View daily summary | ✅ | ❌ |
| View full finance reports | ✅ | ❌ |
| Export reports | ✅ | ❌ |
| Edit menu (items, prices, variants) | ✅ | ❌ |
| Close shift | ✅ | ❌ |
| Stock management & opname | ✅ | ❌ |
| App settings | ✅ | ❌ |

---

## 3. Core User Problems

| # | Problem | Impact |
|---|---------|--------|
| P-1 | Orders written on paper, totals calculated manually | Slow, error-prone, wrong change |
| P-2 | No visibility into which table owes what, multiple groups | Cannot manage simultaneous open bills |
| P-3 | No daily cash reconciliation | Cannot detect cash loss or honest counting errors |
| P-4 | Stock guessed, no warning before running out | Customer disappointment, wasted ingredients |
| P-5 | No expense tracking — "profit" is a feeling not a number | Cannot make informed pricing or restocking decisions |
| P-6 | Two operators with no shared order view | Communication failures, duplicate or missed orders |

---

## 4. User Stories

### Order Taking
- As an operator, I open the app and immediately see the menu grid so I can take an order without extra navigation.
- As an operator, I tap a menu item to add it to the current order; each tap increments the count by 1.
- As an operator, I use +/- buttons to adjust item quantity before confirming the order.
- As an operator, I tap a menu item with variants to select the applicable options (size, spice level, add-ons) before it's added.
- As an operator, I see the running total update in real time as I add or remove items.
- As an operator, I mark an item as sold out with one tap so it appears greyed out and cannot be ordered.

### Bill Management (AM-2: no tables, pay-first only)
- As an operator, I create a new bill, add items, and go straight to payment — there is no table selection and no open/running tab.
- As an operator, each new bill gets an auto-incremented label (Bill #1, Bill #2) so I can tell them apart without naming them.

### Payment
- As an operator, I select a payment method, enter the tendered cash amount, and see the change calculated immediately.
- As an operator, I process a split payment across two methods (e.g. cash + QRIS) for one bill.
- As an owner, I toggle payment methods on/off and reorder them from settings without reinstalling.

### Void & Cancel
- As an operator, I remove an item from an open bill and must select a reason (Customer Change / Kitchen Error / Item Unavailable / Test / Other).
- As an owner, I void an entire paid or open bill with a mandatory reason, leaving an immutable audit record.
- As an owner, I see all voids for the day in the Z-report at close.

### Stock Management
- As an owner, I record incoming stock (ingredient, quantity, unit, purchase price, date) to establish a cost basis.
- As an owner, I link ingredients to menu items via a recipe so the system auto-deducts stock when items are sold.
- As an owner, I see which stock items are below their reorder threshold.
- As an owner, I start a stock opname session, count each item, and submit to reconcile system vs physical count.
- As an owner, I categorize each variance (spoilage / loss / theft / counting error) so I understand where stock disappears.

### Day & Cash Drawer (AM-1: Shift renamed to Day, no manual open)
- As an owner, a new day opens automatically with no input — no operator name, no opening float prompt.
- As an owner, I cannot close a day while any bills are still open — the app shows me which bills are blocking.
- As an owner, I manually close the current day at any time via a button in Settings by entering the counted cash; the app shows me the expected vs counted variance.
- As an owner, if I never manually close and the calendar day rolls over, the app auto-closes the previous day (Z-report generated automatically, no counted-cash step) and auto-opens the next day.
- As an owner, I view the Z-report for any past closed day.

### Expenses
- As any operator, I log an operational expense with category, amount, and optional note.

### Reports & Finance
- As an owner, I see today's gross sales, transaction count, payment breakdown, and top items on a dashboard.
- As an owner, I view weekly and monthly revenue trends.
- As an owner, I see gross margin per menu item for items with recipes configured.
- As an owner, I export a report as CSV or PDF to share or archive.

### Sync & Multi-Device
- As an operator on Device 2, I see a bill created on Device 1 appear within seconds.
- As an operator, the app works fully when internet is down — service is never interrupted.
- As an operator, I can tell at a glance whether the app is synced, pending, or offline.

### Settings & Language
- As an owner, I switch the app language between Bahasa Indonesia and English from settings.
- As an owner, I manage menu, tables, payment methods, and expense categories from within the app.

---

## 5. Functional Requirements

### FR-AUTH: Authentication & Roles

**FR-AUTH-1:** Firebase Auth email/password. Two accounts: owner and staff.

**FR-AUTH-2:** Roles via Firebase custom claims: `role: "owner"` or `role: "staff"`. Set once via Firebase Console by the owner. Claims are cached in the ID token and enforced by RTDB security rules.

**FR-AUTH-3:** Auth state persists offline. Once logged in, operators never need to re-authenticate while offline. The refresh token auto-renews when connectivity is restored.

**FR-AUTH-4:** New device onboarding requires at least one internet connection for the first Firebase Auth login. This is a known constraint — document it in the setup guide.

**FR-AUTH-5:** All owner-only screens check `role == "owner"` in the ViewModel before rendering. RTDB security rules provide the server-side enforcement layer.

---

### FR-I18N: Internationalisation

**FR-I18N-1:** The app is fully bilingual: Bahasa Indonesia and English.

**FR-I18N-2:** Language is user-selectable from Settings. Selection is stored locally per device (not synced).

**FR-I18N-3:** All UI strings use Android string resources (`strings.xml` + `values-id/strings.xml`). No hardcoded strings in Compose code.

**FR-I18N-3a — Development rule (enforced from Day 1):** Every string written during development must be added to both `strings.xml` (English) and `values-id/strings.xml` (Bahasa Indonesia) in the same commit. Never hardcode a display string and plan to extract it later — retrofitting bilingual support across a full codebase is significantly more painful than doing it upfront. Claude Code must never emit a hardcoded string literal in any Composable or ViewModel. If a Bahasa Indonesia translation is not yet decided, use a placeholder `[TODO-ID: en_text_here]` in `values-id/strings.xml` so it is visibly incomplete rather than silently missing.

**FR-I18N-4:** Default language: Bahasa Indonesia (matches locale; falls back to English if a string is missing).

**FR-I18N-5:** Monetary formatting uses `Rp` prefix with Indonesian locale number formatting (no decimal places).

---

### FR-ORDER: Order Taking

**FR-ORDER-1:** The order-taking screen is the app's launch destination (default nav destination).

**FR-ORDER-2:** Menu items are displayed in a scrollable grid, grouped by category. Categories are horizontally filterable (chip strip at top).

**FR-ORDER-3:** Each item card shows: name, price, quantity badge (if >0 in cart). Sold-out items are greyed and non-tappable.

**FR-ORDER-4:** Tapping an item with no variants immediately adds 1 to the cart. Tapping an item with VariantGroups opens a bottom sheet for variant selection. Required groups must be fulfilled before the item can be added.

**FR-ORDER-5:** The cart (current order) is visible as a persistent bottom panel or side panel. It shows items, quantities (+/- buttons), line totals, and a running subtotal.

**FR-ORDER-6:** The +/- buttons adjust quantity; pressing − on quantity 1 removes the item from the cart.

**FR-ORDER-7 (superseded by AM-2):** Confirming an order always creates a new bill and goes straight to payment. There is no destination selection — no table, no "existing open bill" option.

**FR-ORDER-8 (superseded by AM-2):** All orders skip table selection and go directly to payment after confirmation.

**FR-ORDER-9:** Confirming the order writes OrderItems to Room immediately (optimistic). The UI never waits for RTDB sync.

---

### FR-BILL: Bill Management

**FR-BILL-1 (superseded by AM-2):** All bills are `UPFRONT` (pay before food is prepared). `OPEN_BILL` type is removed — there is no pay-after-eating flow.

**FR-BILL-2:** Bill status: `OPEN` → `PAID` or `VOID`. Status only ever moves forward. A paid bill cannot be re-opened — only voided (which creates an audit record) and replaced with a new bill if needed.

**FR-BILL-3 (removed by AM-2):** No table concept, so no multiple-bills-per-table.

**FR-BILL-4:** Each new bill gets an auto-incremented session label: "Bill #1", "Bill #2", etc. Labels reset at app startup or day open.

**FR-BILL-5:** An OPEN bill can receive additional OrderItems at any time before payment (append-only inserts — safe for concurrent writes from two devices).

**FR-BILL-6:** OrderItem stores snapshots of item name and price at time of ordering. Menu price changes never retroactively alter historical bills.

**FR-BILL-7:** Bills open for more than 12 hours trigger a soft warning on the open bills list (orange indicator). No hard block.

**FR-BILL-8:** Bills are daily. There are no cross-day bills. If a bill is somehow left open at end of day, it must be manually closed or voided before the next day can close.

---

### FR-PAYMENT: Payment Processing

**FR-PAYMENT-1:** Payment screen: bill summary, method selector (enabled methods only), tendered amount input (cash), computed change.

**FR-PAYMENT-2:** Change = `amountTendered − grandTotal`. If tendered < grandTotal, show a deficit warning and block completion.

**FR-PAYMENT-3:** Split payment: a single bill can hold multiple Payment rows across methods. The payment screen shows "remaining balance" as rows are added. Bill is marked PAID only when all payments sum to grandTotal.

**FR-PAYMENT-4:** Payment methods are fully configurable from Settings: toggle enabled/disabled, rename, reorder. Only enabled methods appear on the payment screen.

**FR-PAYMENT-5:** All monetary values stored as `Long` (integer Rupiah). No `Float` or `Double` anywhere in the money pipeline.

**FR-PAYMENT-6:** QRIS is recorded as a label only. No MDR surcharge is ever added (prohibited by Bank Indonesia regulation). No gateway integration.

---

### FR-VOID: Void & Cancel

**FR-VOID-1:** Any operator can void an OrderItem on an OPEN bill. Mandatory reason selection: `CUSTOMER_CHANGE`, `KITCHEN_ERROR`, `ITEM_UNAVAILABLE`, `TEST`, `OTHER`. If `OTHER`, a short text note is required.

**FR-VOID-2:** Voided items remain visible in the bill (struck-through). They are excluded from totals. Never hard-deleted.

**FR-VOID-3:** Voiding an entire bill is owner-only. Records: operator, timestamp, reason, total voided value. Voided bills remain in history.

**FR-VOID-4:** Day's Z-report includes: total void count, total voided value, breakdown by void reason.

---

### FR-MENU: Menu Management

**FR-MENU-1:** Menu items organised into Categories. Categories: create, rename, reorder, soft-delete (blocked if items reference them).

**FR-MENU-2:** MenuItem fields: name, category, base price (`Long`), `isAvailable` (toggle for hiding), `isSoldOut` (toggle for sold-out state). No image field.

**FR-MENU-3:** Sold-out toggle is manual only. It does NOT auto-reset. Operator must explicitly re-enable.

**FR-MENU-4:** VariantGroup: `id`, `menuItemId`, `name`, `selectionType (SINGLE | MULTIPLE)`, `isRequired`.

**FR-MENU-5:** VariantOption: `id`, `variantGroupId`, `name`, `priceDelta (Long)` (can be 0, positive, or negative).

**FR-MENU-6:** Editing a MenuItem's price or name does not affect historical OrderItem snapshots.

**FR-MENU-7:** Deleting a MenuItem is soft-delete only (`isAvailable = false`). Hard delete is blocked if the item appears in any bill.

---

### FR-TABLE: Table Management — REMOVED (AM-2)

Table management, the Tables tab, and Table Settings no longer exist. Every order is counter-service pay-first. This section (formerly FR-TABLE-1/2/3) is retained here only as a historical marker.

---

### FR-STOCK: Stock Management (Phase 2 — design now, build Phase 2)

**FR-STOCK-0 — Schema rule (enforced in Phase 1):** Even though stock management is a Phase 2 feature, the `StockItem`, `StockBatch`, and `MenuItemIngredient` Room entities **must be defined and included in the initial database schema during Phase 1.** Adding foreign key relationships to Room requires a database migration, which is error-prone and annoying to retrofit. Define the full schema once correctly upfront — the tables will simply be empty until Phase 2 is built. Claude Code must include these entities in the `@Database` definition from the start.

**FR-STOCK-1:** StockItem master: `id`, `name`, `unit`, `currentQuantity (Double)`, `lowStockThreshold`, `lastCostPrice (Long)`.

**FR-STOCK-2:** StockBatch (incoming): `id`, `stockItemId`, `quantity`, `purchasePrice (Long)`, `purchaseDate`, `supplier?`.

**FR-STOCK-3:** MenuItemIngredient (recipe): `menuItemId`, `stockItemId`, `quantityUsed`. Optional per item. Items without a recipe do not auto-deduct stock and do not contribute to COGS.

**FR-STOCK-4:** On bill payment, deduct ingredient quantities from StockItem.currentQuantity per the recipe. Uses `ServerValue.increment(-n)` for atomic concurrent safety.

**FR-STOCK-5:** Low-stock warning appears in-app (indicator on the stock screen and a badge on the More nav item) when `currentQuantity ≤ lowStockThreshold`.

**FR-STOCK-6:** Note for owner: items without a recipe configured will not deduct stock or appear in per-item profit reports. This is a deliberate tradeoff — configure recipes for items where margin visibility matters.

---

### FR-OPNAME: Stock Opname (Phase 2)

**FR-OPNAME-1:** Owner starts a StockOpname session. System snapshots `expectedQty` for every StockItem at session start time.

**FR-OPNAME-2:** Opname UI: scrollable list of all StockItems, `actualQty` input per item, real-time `variance = actualQty − expectedQty` display.

**FR-OPNAME-3:** Non-zero variance lines require a reason: `SPOILAGE`, `LOSS`, `THEFT`, `COUNTING_ERROR`, `OTHER`.

**FR-OPNAME-4:** Pre-submit summary: total variance, total cost impact (variance × lastCostPrice), total stock value.

**FR-OPNAME-5:** On submit, `StockItem.currentQuantity` is set to `actualQty`. Opname record is immutable.

**FR-OPNAME-6:** Sessions can be paused and resumed. Only one session can be IN_PROGRESS at a time.

**FR-OPNAME-7:** Stock deductions from sales during an active opname session are queued and applied post-commit against the submitted baseline.

---

### FR-EXPENSE: Expense Tracking

**FR-EXPENSE-1:** Any operator can log an expense: category, amount (`Long`), date, optional note.

**FR-EXPENSE-2:** Default categories: `GAS`, `ELECTRICITY`, `PACKAGING`, `WAGES`, `RENT`, `OTHER`. Categories editable in Settings.

**FR-EXPENSE-3:** Expenses appear in daily/weekly/monthly finance reports as a cost line.

---

### FR-DAY: Day Management (formerly FR-SHIFT, renamed and changed by AM-1)

**FR-DAY-1:** There is no manual "open" action. A Day auto-starts with no input — no operator name, no opening cash float. `openingFloat` defaults to `0`.

**FR-DAY-1a:** When a new Day starts (manual close or auto-rollover), if any menu items have `isSoldOut = true`, prompt: **"Reset all sold-out items?"** with Yes / No options. If Yes, all `isSoldOut` flags are set to `false`. The operator can still choose No and reset individually later.

**FR-DAY-2:** Only one Day can be OPEN at a time across all devices.

**FR-DAY-3:** Manual close is owner-only, triggered by a button in Settings. Pre-condition: zero OPEN bills. If open bills exist, the close screen shows a blocking list — owner must close or void all bills first.

**FR-DAY-4:** Manual close inputs: counted physical cash. The app computes:
- `expectedCash = openingFloat + Σ(cash payments in day) − Σ(cash expenses in day)`
- `cashVariance = countedCash − expectedCash`

**FR-DAY-4a (new, AM-1):** Auto-close on calendar day rollover: if the Day is still OPEN when the date changes, the app automatically closes it and opens the next Day. Auto-close skips the counted-cash step entirely — `countedCash = expectedCash` and `cashVariance = 0` in the generated Z-report, since no one manually counted. This auto-close still requires zero OPEN bills; if bills are open at rollover, the previous Day stays open until an owner manually resolves it (same blocking behavior as FR-DAY-3).

**FR-DAY-5:** On close (manual or auto), an immutable Z-report snapshot is created containing:
- Date, day open/close time, opened by (if manually reopened by an owner action), closed by (owner, or "system" for auto-close)
- Gross sales (all payment methods combined)
- Sales breakdown by payment method
- Total voids: count, value, reason breakdown
- Expenses by category, total
- Opening float, expected cash, counted cash, cash variance
- Gross profit (sales − COGS, only if recipes configured)
- Net (gross profit − expenses)

**FR-DAY-6:** Closed Days and their Z-reports are permanently immutable. No reopening. Errors discovered post-close are noted manually outside the app.

**FR-DAY-7:** Bills are always attributed to the Day in which they were PAID (not opened).

---

### FR-REPORTS: Financial Reports (Owner only)

**FR-REPORTS-1:** Dashboard: today's gross sales, transaction count, payment-method breakdown, top 5 items by quantity sold, cash variance.

**FR-REPORTS-2:** Date-range report: filter by day / week / month / custom range. Shows: revenue, expenses, gross profit, net, payment mix, void summary.

**FR-REPORTS-3:** Per-item margin report: for items with recipes, shows revenue, COGS, gross margin %, quantity sold. Items without recipes are excluded.

**FR-REPORTS-4:** Best-seller ranking: items sorted by quantity sold over selected period.

**FR-REPORTS-5:** Export: any report can be exported as CSV (raw data) or PDF (formatted summary). Share via Android share sheet (WhatsApp, Drive, email, etc.).

---

### FR-SYNC: Offline-First Sync

**FR-SYNC-1:** Room is the single source of truth. UI reads only from Room via DAO Flows. Never reads from RTDB directly.

**FR-SYNC-2:** All writes go to Room first (optimistic, instant UI). Each syncable entity has sync metadata: `updatedAt (Long epoch ms)`, `syncStatus (PENDING | SYNCED | CONFLICTED)`, `deviceId (String)`.

**FR-SYNC-3:** Background sync: WorkManager with network connectivity constraint pushes PENDING records to RTDB. RTDB listeners pull remote changes into Room.

**FR-SYNC-4:** Persistent, unobtrusive sync status indicator (synced ✓ / pending ⏳ / offline ✗) always visible. Never blocks UI.

**FR-SYNC-5:** RTDB writes are always field-level paths — never whole-object replacement. OrderItems use append-only RTDB push keys.

**FR-SYNC-6:** Stock quantities use `ServerValue.increment(-n)` for atomic concurrent safety.

**FR-SYNC-7:** Bill status transitions (OPEN→PAID) are guarded by RTDB transactions so a stale device cannot reopen a paid bill.

**FR-SYNC-8:** Last-write-wins by `updatedAt` for all other fields. Acceptable for 2-device warung where write collisions on the same field are rare.

**FR-SYNC-9:** On reconnect after a long offline period, all PENDING records flush in chronological order by `updatedAt` with a visible "Syncing N records…" progress note.

---

## 6. Non-Functional Requirements

### NFR-PERF: Performance
- Order-taking screen fully interactive within 1 second of cold start
- Adding item to cart reflects in UI within 100ms
- Payment completion (Room write + UI update) within 500ms
- Finance reports for 12 months of data load within 3 seconds
- All Room queries run on background dispatcher — main thread never blocked

### NFR-OFFLINE: Offline Capability
- 100% of core POS operations work without internet: order, payment, void, shift open/close, expense logging
- App functions from first launch onward even if internet is never restored (all master data seeded locally)
- Known constraint: first-time Firebase Auth login on a new device requires connectivity

### NFR-DATA: Data Integrity
- All money stored as `Long` (integer Rupiah). Zero `Float`/`Double` in financial code
- Paid bills and shift Z-reports are immutable after finalisation
- Void records are soft-delete only — no hard deletes on any financial record
- All entities use client-generated String UUID as PK (not autoincrement Int)

### NFR-SECURITY: Security
- RTDB security rules: only authenticated users with valid role claims can read/write
- Staff accounts cannot read financial report paths in RTDB
- No sensitive data stored in plain SharedPreferences — use EncryptedSharedPreferences for any tokens

### NFR-RELIABILITY: Reliability
- Zero crashes in order and payment flows — validate all inputs before write
- RTDB sync failures never block the operator — app continues normally, retries automatically with exponential backoff

### NFR-I18N: Language
- All UI strings externalised to `strings.xml` (English) + `values-id/strings.xml` (Bahasa Indonesia)
- Language switch applies immediately without app restart

### NFR-COST: Firebase Free Tier
- App must never require Firebase Blaze upgrade at warung scale
- RTDB Spark: 1 GB storage / 10 GB download / 100 connections. A 200 tx/day warung with 3 devices uses <1% of all limits
- No Firebase Storage (no menu images) — eliminates that cost dimension entirely

### NFR-DISTRIBUTION: APK Updates
- App distributed as signed APK via private channel (WhatsApp or Google Drive)
- Both devices must update together to avoid schema version mismatches
- Add a minimum version check on app launch: if `BuildConfig.VERSION_CODE` < `minVersionCode` stored in RTDB, show a blocking "Please update" screen

---

## 7. MVP Scope

MVP enables a complete real service day: **open shift → take orders → process payments → close shift.**

### In MVP

| Feature | Notes |
|---------|-------|
| Firebase Auth (2 users, role claims) | Claims set via Firebase Console once |
| Bilingual UI (Bahasa Indonesia + English) | Language toggle in Settings |
| Menu management (items, categories, variants, no images) | |
| Order-taking screen — item grid, variant sheet, tap +/- quantity | Launch destination; no table selection (AM-2) |
| Single bill flow: pay-first, no tables, no open bill (AM-2) | |
| Sold-out toggle (manual reset) | |
| Configurable payment methods (toggle, reorder) | |
| Cash payment + change calculator | |
| Split payment (multi-method per bill) | |
| Void order item with reason (owner + staff, always logged) | |
| Void entire bill (owner only) | |
| Day auto-open, no opening float input (AM-1) | |
| Day close — manual (Settings button) or automatic on date rollover; blocks on open bills, Z-report snapshot | |
| Expense logging (all operators) | |
| Daily revenue dashboard (owner only) | |
| Open bills overview screen | |
| Sync status indicator | |
| Room DB — full entity schema with sync metadata | |
| RTDB sync — offline-first, LWW, append-only order items, atomic stock | |
| Minimum version check on launch | |

### Explicitly Out of MVP

| Feature | Phase |
|---------|-------|
| Stock management + recipe links | Phase 2 |
| Stock opname | Phase 2 |
| Finance reports (beyond daily dashboard) | Phase 2 |
| CSV / PDF export | Phase 2 |
| Best-seller analytics | Phase 2 |
| Discounts / promos | Phase 2 |
| Kitchen / order queue display | Phase 2 |
| Per-item margin report | Phase 2 |
| Peak-hour heatmap | Phase 3 |
| PPh Final helper | Phase 3 |
| Item-level split bill | Phase 3 |

---

## 8. Future Scope

### Phase 2 — Operational Depth
- Stock management: incoming batches, recipe links, auto-deduction on sale
- Low-stock threshold alerts (in-app only)
- Stock opname: session-based, variance + reason categorisation, post-commit deduction replay
- Full finance reports: weekly/monthly, per-item margin, expense breakdown by category
- CSV and PDF export via Android share sheet
- Best-seller ranking
- Discount / promo support (per-item and per-bill, % or flat amount)
- Kitchen / order queue screen (chronological unfulfilled order items with Done checkbox)

### Phase 3 — Nice to Have
- PPh Final monthly turnover tracker (informational; not a tax filing tool)
- Peak-hour heatmap (derived from existing `createdAt` timestamps)
- Item-level split bill (split one bill's items across multiple payers)
- WhatsApp digital receipt (render bill to image, share via Android share intent)
- MDR net-settlement tracking per payment method (QRIS MDR 0%/0.3%)

---

## 9. Success Metrics

| Metric | Target | How to measure |
|--------|--------|----------------|
| Full service day runs end-to-end without paper backup | Day 1 of real use | Manual verification |
| Zero cash discrepancy attributable to app error | 0 unexplained variances after 30 days | Z-report review |
| Void rate < 5% of order items | Signals clean UX and operations | Void log in Z-report |
| App never crashes during order or payment flow | 0 crashes | Firebase Crashlytics (free tier) |
| RTDB remains on free Spark tier forever at warung scale | No billing alerts | Firebase console |
| No sync conflict causes lost payment or order | 0 lost records | Manual audit: Z-report totals vs RTDB raw data |
| Owner can name top 3 best-sellers after 1 week | Informal goal | Phase 2 best-seller report |

---

## 10. Risks and Assumptions

### Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| LWW silently drops a bill field update | Medium | High | Always write field-level RTDB paths. Never replace whole bill objects. Log all CONFLICTED records. |
| Large offline queue on reconnect causes slow flush | Low | Medium | Flush in chronological order with visible progress. Use WorkManager batch constraints. |
| Stock deduction corrupted during opname session | Medium | Medium | Queue deductions during opname, replay post-commit against submitted baseline |
| Firebase Spark free tier exceeded | Very Low | Medium | Current scale is <1% of limits. Monitor Firebase console. Set spend alert even on Spark. |
| Both devices lost/broken with stale last sync | Low | High | Advise owner: connect to internet at least once per operating day to ensure RTDB mirror is current |
| APK version mismatch between devices causes Room schema conflict | Medium | High | Minimum version check on launch blocks older APK from operating. Update both devices together. |

### Operational Risks

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Staff voids paid cash orders to pocket money | Medium | High | Void log in every Z-report. Owner reviews void breakdown daily. |
| Open bill forgotten past midnight | Low | Medium | 12-hour warning indicator on open bills list |
| Shift close fails because old bills are left open | Medium | Low | FR-SHIFT-3 blocks close with explicit blocking bill list |
| Sold-out not reset next morning (manual toggle) | High | Low | Owner habit required. Consider adding a "reset all sold-out" button to shift-open screen as a reminder prompt |

### Assumptions (Confirmed)

| Assumption | Status |
|-----------|--------|
| Portrait only | ✅ Confirmed |
| No receipt printing | ✅ Confirmed |
| No menu item images | ✅ Confirmed |
| No PPN/tax computation (micro warung, non-PKP) | ✅ Confirmed by Indonesian tax law |
| No QRIS MDR surcharge in app | ✅ Confirmed, banned by Bank Indonesia |
| Bills are daily, never span operating days | ✅ Confirmed |
| Sold-out toggle is always manual | ✅ Confirmed |
| Auto-increment bill labels per session | ✅ Confirmed |
| Block shift close on open bills | ✅ Confirmed |
| No inactivity lock | ✅ Confirmed |
| CSV + PDF export in scope (Phase 2) | ✅ Confirmed |

---

## Appendix A: Entity Schema (for Claude Code)

```
-- Sync metadata on every synced entity --
updatedAt: Long          (epoch ms, for LWW merge)
syncStatus: Enum         (PENDING | SYNCED | CONFLICTED)
deviceId: String         (origin device UUID)
All PKs: String UUID     (client-generated, never autoincrement Int)
All money: Long          (integer Rupiah, never Float/Double)

MenuCategory      id, name, sortOrder, + sync
MenuItem          id, categoryId(FK), name, basePrice(Long), isAvailable, isSoldOut + sync
VariantGroup      id, menuItemId(FK), name, selectionType(SINGLE|MULTIPLE), isRequired + sync
VariantOption     id, variantGroupId(FK), name, priceDelta(Long) + sync

Table             REMOVED (AM-2) — no table concept
Bill              id, type(UPFRONT only, AM-2), status(OPEN|PAID|VOID),
                  sessionLabel(e.g. "Bill #3"), createdAt, paidAt(nullable), subtotal(Long),
                  discountTotal(Long), grandTotal(Long), note(nullable), dayId(FK nullable, formerly shiftId),
                  voidReason(nullable), voidedBy(nullable) + sync
OrderItem         id, billId(FK), menuItemId(FK), nameSnapshot, priceSnapshot(Long),
                  quantity, selectedVariantsJson, lineTotal(Long),
                  status(ORDERED|DONE|VOID), voidReason(nullable), voidedBy(nullable),
                  createdAt + sync
Payment           id, billId(FK), paymentMethodId(FK), amount(Long),
                  amountTendered(Long nullable), changeGiven(Long nullable), paidAt + sync
PaymentMethod     id, name, isEnabled, sortOrder + sync

StockItem         id, name, unit, currentQuantity(Double), lowStockThreshold(Double),
                  lastCostPrice(Long) + sync
StockBatch        id, stockItemId(FK), quantity(Double), purchasePrice(Long),
                  purchaseDate, supplier(nullable) + sync
MenuItemIngredient menuItemId(FK), stockItemId(FK), quantityUsed(Double) + sync

StockOpname       id, startedAt, completedAt(nullable),
                  status(IN_PROGRESS|COMPLETED|CANCELLED), conductedBy, note(nullable) + sync
StockOpnameLine   id, opnameId(FK), stockItemId(FK), expectedQty(Double), actualQty(Double),
                  variance(Double), varianceReason, costImpact(Long) + sync

Expense           id, category, amount(Long), date, note(nullable), recordedBy + sync

Day (formerly Shift, AM-1)
                  id, openedAt(auto, no input), closedAt(nullable), closedBy(owner or "system" for auto-close),
                  openingFloat(Long, always 0 — no manual entry), countedCash(Long nullable, null if auto-closed),
                  expectedCash(Long), variance(Long nullable, 0 if auto-closed),
                  closeType(MANUAL|AUTO_ROLLOVER), status(OPEN|CLOSED) + sync
ZReport           id, dayId(FK, formerly shiftId), snapshotJson(String) -- immutable after write, no sync updates
```

---

## Appendix B: Navigation Structure (for Claude Code)

```
Bottom Nav (3 items — Tables removed, AM-2):
├── 🧾 Order       ← DEFAULT launch destination
├── 📊 Reports     ← owner only (role-gated)
└── ☰  More
    ├── Day Management (manual close button / history) — no manual "open" (AM-1)
    ├── Expense Log
    ├── Stock (Phase 2)
    ├── Stock Opname (Phase 2)
    ├── Menu Management
    ├── Payment Method Settings
    └── App Settings
         ├── Language (Bahasa Indonesia / English)
         ├── Expense Categories
         └── About / Version

Order flow (AM-2: single flow, no destination selection):
  Item Grid → [Variant Sheet if needed] → Confirm Order → Payment Screen → Done
```

---

## Appendix C: RTDB Flat Structure (for Claude Code)

```
/appConfig/minVersionCode: Int          ← version gate
/appConfig/openDayId: String | null     ← enforces single open day (formerly openShiftId)

/menuCategories/{id}/...
/menuItems/{id}/...
/variantGroups/{id}/...
/variantOptions/{id}/...

/bills/{id}/...                        ← no /tables node (AM-2)
/orderItems/{id}/...
/payments/{id}/...
/paymentMethods/{id}/...

/stockItems/{id}/...
/stockBatches/{id}/...
/menuItemIngredients/{menuItemId}/{stockItemId}/...

/opnames/{id}/...
/opnameLines/{id}/...
/expenses/{id}/...
/days/{id}/...                         ← formerly /shifts

-- Index nodes for efficient lookups --
/billsByDay/{dayId}/{billId}: true      ← formerly billsByShift
/pendingStockDeductions/{opnameId}/{deductionId}: { stockItemId, qty }
```

---

## Appendix D: Build Checklist for Claude Code

> **Note:** This checklist predates AM-1/AM-2 and reflects the original Shift/Table model. It is a historical build log (largely already completed); it is not being rewritten line-by-line. Treat FR-DAY and the removal of FR-TABLE (above) as authoritative over any Shift/Table wording still appearing in the checkboxes below.

### Phase 1 — Foundation
- [ ] Confirm existing project min SDK, apply Gradle dependencies (Room, Hilt, Firebase RTDB, Firebase Auth, WorkManager, Compose Navigation)
- [ ] Set up Hilt application and module structure
- [ ] Define all Room entities + DAOs (full schema from Appendix A, **including StockItem, StockBatch, MenuItemIngredient — empty tables, but schema defined now to avoid migrations later**)
- [ ] Define RTDB flat structure + security rules
- [ ] Implement sync metadata mixin pattern
- [ ] Implement Firebase Auth login screen (email/password) with role-claim read
- [ ] Set up Navigation graph (bottom nav + nested graphs)
- [ ] Add string resources for both languages (EN + ID)
- [ ] Language toggle in Settings (persisted to EncryptedSharedPreferences)

### Phase 1 — Core POS
- [ ] Menu management screens (category + item + variant CRUD)
- [ ] Table management screen (CRUD + active/inactive toggle)
- [ ] Payment method settings screen (toggle, reorder)
- [ ] Order-taking screen (item grid by category, chip filter, variant sheet, cart panel, +/- quantity)
- [ ] Order destination selector (grab-and-go / new table bill / existing bill)
- [ ] Open bills overview screen (grouped by table, running totals)
- [ ] Payment screen (method selector, tendered input, change calc, split payment)
- [ ] Void order item flow (reason picker + OTHER note)
- [ ] Void entire bill flow (owner only, reason required)
- [ ] Shift open screen (operator name, opening float)
- [ ] Shift close screen (open bill blocker list, counted cash input, variance display, Z-report generation)
- [ ] Z-report history viewer
- [ ] Expense logging screen
- [ ] Daily dashboard (owner only, role-gated)
- [ ] Sync status indicator (persistent, non-blocking)
- [ ] Minimum version gate (check RTDB /appConfig/minVersionCode on launch)

### Phase 2 — Stock & Reports
- [ ] StockItem + StockBatch management screens
- [ ] Recipe (MenuItemIngredient) management per menu item
- [ ] Auto stock deduction on bill payment (ServerValue.increment)
- [ ] Low-stock indicator (stock screen + More badge)
- [ ] Stock opname: session start, count entry, variance, reason, pre-submit summary, commit
- [ ] Deferred deduction queue during opname
- [ ] Full finance reports: date-range, per-item margin, best-seller, expense breakdown
- [ ] CSV export (generate + Android share sheet)
- [ ] PDF export (generate + Android share sheet)
- [ ] Kitchen order queue screen (unfulfilled items, Done checkbox)
- [ ] Discount / promo support (per-bill and per-item)

### Phase 3 — Polish
- [ ] PPh Final monthly turnover info widget
- [ ] Peak-hour heatmap chart
- [ ] Item-level split bill
- [ ] WhatsApp receipt sharing (bill → image → share intent)
