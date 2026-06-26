# Developer-Ready Research: Building a Personal Android POS for an Indonesian Warung

## TL;DR
- **Firebase Realtime Database (RTDB) is the correct choice over Firestore for this scale** — a 2-operator warung will consume a trivial fraction of the free Spark tier (1 GB storage, 10 GB/month download, 100 simultaneous connections), and RTDB's flat-JSON + `ServerValue.increment()` model fits write-heavy POS sync better and more cheaply than Firestore's per-operation billing.
- **The data model and feature set are sound; the highest-value additions are shift/cash-drawer closing, void-with-reason logging, expense tracking, and a sold-out toggle.** Skip PPN/tax entirely — a micro warung under Rp4.8 billion/year is legally barred from collecting PPN and only owes 0.5% PPh Final (and nothing under Rp500 million/year).
- **Architecture: Room is the single source of truth; RTDB is a background mirror with last-write-wins at the field level and atomic transactions reserved only for shared stock counters.** Build the order-taking screen as the launch destination; everything else is secondary.

## Key Findings

### 1. Firebase Realtime Database vs Firestore on the free tier
**Verdict: RTDB wins for this use case.** Firestore bills per document read/write/delete; RTDB bills only on data stored and data downloaded, with no per-operation charge. For a POS where two devices repeatedly read/write small order and bill objects all day, RTDB's model avoids the read/write metering that makes Firestore expensive for chatty, write-heavy sync.

**RTDB Spark (free) limits, confirmed:**
- **1 GB stored data** — text-only POS data (orders, bills, menu, stock) is tiny; a warung will store on the order of a few MB per year.
- **10 GB/month download** — the realistic constraint, but a 2-3 device store syncing small JSON deltas will use a small fraction of this.
- **100 simultaneous connections** — a hard cap that cannot be raised on Spark. Confirmed by Firebase's official Realtime Database limits and the firebase-talk group: "If you have reached the maximum number of connections to your database, subsequent connection attempts will be rejected by the server. Once a connection becomes available again, the next connection request will be accepted." One connection = one device/tab/server, so with 1-3 devices you use 1-3 connections — a non-issue.
- **1 database per project** on the free tier. Per Firebase Blaze pricing (Tekpon, 2026): "Firebase Realtime Database: $5 per GB stored, $1 per GB downloaded" — charges that apply only if you ever exceed the free quota and move to Blaze.

**Realistic daily consumption for a warung:** With perhaps 50-200 transactions/day, each order/bill object measured in hundreds of bytes to a couple KB, plus listener overhead, daily download is on the order of a few MB — far under the ~333 MB/day implied by the 10 GB monthly allowance. Storage growth is a few MB/year. **You will essentially never leave the free tier at this scale.**

**Structure: keep it flat (denormalized).** Firebase's own guidance: "in practice, it's best to keep your data structure as flat as possible," because fetching a node downloads all its children and grants access to everything beneath it. Do NOT nest order items deep under tables under days. Use separate top-level paths (`/bills`, `/orderItems`, `/menu`, `/stock`) keyed by push IDs, with index nodes (`/openBillsByTable/{tableId}/{billId}: true`) for relationships. Limit nesting to 3-4 levels; deep nesting measurably increases latency and forces large downloads.

**Firebase Auth for 2-user access control:** Email/password Auth is free at this scale. Per Firebase docs (updated 2026-06-22), the Blaze free tier covers 50,000 monthly active users, and projects on the no-cost Spark plan have a limit of 3,000 daily active users for most sign-in providers once upgraded to Identity Platform — 2 users is trivially within both. Auth state persists locally on the device and the ID token auto-refreshes via the stored refresh token, so **operators stay logged in across app restarts and offline** — login is cached and works without connectivity once established. Use **custom claims** (e.g. `role: "owner"` vs `role: "staff"`) for RTDB security rules (`auth.token.role === 'owner'`); claims are embedded in the ID token so rule checks need no extra DB lookup. Note custom claims must be set from a privileged environment (Admin SDK / Cloud Function), and there is a ~1000-byte claim limit. For a 2-person personal app, even simpler: a single shared owner account plus one staff account — but email/password with a role claim is the clean choice.

**Conflict resolution for 2 simultaneous writers:** RTDB merges with **last-write-wins** by default — whichever write reaches the server last overwrites. For a POS this is acceptable for most fields if you write at the field/child level rather than overwriting whole objects. The danger is wholesale object replacement (writing the entire bill node), which guarantees lost updates. Two concrete rules:
  1. **Write granular paths** (`/bills/{id}/status`, `/orderItems/{id}`) not whole-bill blobs.
  2. **Use `runTransaction()` or `ServerValue.increment()` for shared counters** — specifically stock quantity — because these are the only values two devices realistically corrupt by concurrent modification. `increment()` runs server-side with no conflict possibility; `runTransaction()` re-runs your update function if another client wrote first.

### 2. Recommended additional features and prioritization
The owner's list is strong. Based on how production restaurant POS systems (Square, Lightspeed, Lavu, Moka, Pawoon) structure their feature sets, here are the additions, ranked.

**MVP / must-have additions:**
- **End-of-day / shift closing + cash drawer reconciliation.** This is the single most important missing feature. Industry practice: at shift close, generate a Z-report style summary (total sales, breakdown by payment method, voids, discounts), record the starting cash float, count the physical drawer, and compute variance (over/short). A warung with 2 operators handling cash needs this to catch errors and shrinkage. Make a "Z-report resets the shift" concept: each closing snapshots and locks the period.
- **Void/cancel order with reason logging.** Every void should record who, when, and why (e.g. "customer changed order," "kitchen error," "test"). Unrecorded voids are the most common cash-theft mechanism in F&B — a cashier voids a paid cash order and pockets the money. This is cheap to build and high-value.
- **Menu item availability toggle (sold out for the day).** Essential for a warung where dishes run out. One boolean per menu item, resettable daily.
- **Expense tracking (non-stock operational costs).** Gas (LPG), electricity, packaging, wages, rent. Without this, "profit" from stock cost alone is misleading. A simple Expense entity with category, amount, date, note.
- **Daily revenue summary dashboard.** Today's gross sales, transaction count, payment-method split, cash expected vs counted, top items.

**Nice-to-have (Phase 2):**
- **Discount/promo support** (per-item and per-bill, percentage or flat). Build the bill total calculation to accommodate discount lines from day one even if the UI comes later.
- **Best-selling item analytics** — straightforward `GROUP BY menuItemId` aggregation.
- **Low-stock threshold alerts** — per-ingredient reorder point.
- **Split bill** — your model (multiple independent bills per table) already covers the "separate groups" case; true item-level splitting of one bill is a Phase 2 refinement.
- **Order queue / kitchen display concept.** Since there's no printer and operators cook, a simple on-screen "open orders" queue (chronological list of unpaid/unfulfilled order items with a "done" checkbox) replaces a kitchen printer. This is genuinely useful for the grab-and-go flow.

**Lower priority / probably skip:**
- **Peak-hour heatmap** — nice analytics but derivable later from timestamped transactions; don't block MVP on it.
- **Tax (PPN)** — **do not build.** See Indonesian context below; a micro warung is legally a non-PKP and cannot charge PPN.

### 3. Room database schema (entity design)
Offline-first means Room is authoritative. Every syncable entity carries sync metadata. Recommended entities:

**Sync metadata mixin (on every synced entity):**
- `updatedAt: Long` (epoch millis, for LWW merge)
- `syncStatus: enum { SYNCED, PENDING, CONFLICTED }`
- `deviceId: String` (origin device, for tie-breaks/debugging)
- Use a stable client-generated **String UUID** as primary key (NOT autoincrement Int) so IDs are globally unique across devices and match RTDB push keys.

**MenuItem**
- `id (String PK)`, `name`, `categoryId`, `basePrice (Long, store rupiah as integer — never floating point for money)`, `isAvailable (Boolean)`, `isSoldOut (Boolean)`, `imageUri (nullable)`, sync metadata.

**Variant / AddOn — separate tables, not nested.** A dish has flexible variants (e.g. spice level, size) and add-ons (extra egg). Model as:
- **VariantGroup**: `id`, `menuItemId (FK)`, `name` ("Size", "Spice"), `selectionType (SINGLE/MULTIPLE)`, `isRequired`.
- **VariantOption**: `id`, `variantGroupId (FK)`, `name` ("Large"), `priceDelta (Long)`.
This normalized approach (separate tables linked by FK) is the standard relational pattern for menu modifiers and avoids the rigidity of embedding.

**Table** (dine-in)
- `id`, `label` ("Meja 1" or null for unnamed), `isActive`. Tables are optional — grab-and-go bills have `tableId = null`.

**Bill**
- `id`, `tableId (FK, nullable)`, `type (enum: UPFRONT, OPEN_BILL)`, `status (enum: OPEN, PAID, VOID)`, `createdAt`, `paidAt (nullable)`, `subtotal`, `discountTotal`, `grandTotal`, `note`, `shiftId (FK, nullable)`, sync metadata. Multiple OPEN bills can share one `tableId` (upfront-separate-groups case); the open-bill case is one OPEN bill per group.

**OrderItem**
- `id`, `billId (FK)`, `menuItemId (FK)`, `menuItemNameSnapshot (String)`, `unitPriceSnapshot (Long)`, `quantity`, `selectedVariantsJson (String)` or a child **OrderItemVariant** table, `lineTotal`, `status (enum: ORDERED, DONE, VOID)`, `voidReason (nullable)`, `voidedBy (nullable)`, `createdAt`. **Snapshot the name and price** onto the order item so historical bills don't change when the menu is edited.

**Payment**
- `id`, `billId (FK)`, `paymentMethodId (FK)`, `amount`, `amountTendered (nullable, for cash)`, `changeGiven (nullable)`, `paidAt`. One bill can have multiple Payment rows (split payment / partial).

**PaymentMethod** (configurable list)
- `id`, `name` ("Tunai", "QRIS", "GoPay", "OVO", "Transfer Bank"), `type (enum)`, `isEnabled (Boolean)`, `sortOrder`. Toggling on/off is just `isEnabled`.

**Stock / StockBatch** (incoming stock with purchase price)
- `id`, `itemName` or `ingredientId (FK)`, `quantity`, `unit`, `purchasePrice (Long, per batch or per unit)`, `purchaseDate`, `supplier (nullable)`, `remainingQuantity`. Link stock to menu items via a join (recipe) table if you want true COGS per dish.
- **StockItem** (the master stock/ingredient record with current quantity): `id`, `name`, `unit`, `currentQuantity`, `lowStockThreshold`, `lastCostPrice`. This is the entity whose `currentQuantity` needs atomic updates across devices.

**MenuItemIngredient (recipe link, optional but enables profit/loss):**
- `menuItemId (FK)`, `stockItemId (FK)`, `quantityUsed`. Lets you compute COGS and profit margin per dish and deduct stock on sale.

**StockOpname (audit session) + StockOpnameLine**
- **StockOpname**: `id`, `startedAt`, `completedAt (nullable)`, `status (enum: IN_PROGRESS, COMPLETED, CANCELLED)`, `conductedBy`, `note`.
- **StockOpnameLine**: `id`, `opnameId (FK)`, `stockItemId (FK)`, `expectedQty (system value at snapshot)`, `actualQty (counted)`, `variance (computed)`, `varianceReason (enum: SPOILAGE, LOSS, THEFT, COUNTING_ERROR, OTHER)`, `costImpact`.

**Expense**
- `id`, `category (enum: GAS, ELECTRICITY, PACKAGING, WAGES, RENT, OTHER)`, `amount`, `date`, `note`, `recordedBy`, sync metadata.

**Shift / CashDrawerSession** (for end-of-day)
- `id`, `openedAt`, `closedAt (nullable)`, `openedBy`, `openingFloat (Long)`, `expectedCash (computed: opening + cash sales − cash payouts)`, `countedCash (Long)`, `variance`, `status (enum: OPEN, CLOSED)`. Bills/payments reference `shiftId`.

**DailySummary — derive, don't store (mostly).** Compute daily/weekly/monthly reports on the fly from transactions via SQL aggregation. Optionally persist an immutable end-of-day snapshot row (a closed `Shift` + computed totals) so historical reports don't drift if old records are edited.

### 4. Room ↔ RTDB sync strategy
**Pattern: offline-first, Room as single source of truth, RTDB as a background mirror.**
- **UI reads only from Room** via DAO `Flow`s exposed as `StateFlow`. The UI never observes Firebase directly. When sync writes to Room, the UI reacts automatically.
- **Writes go to Room first** (optimistic, instant UI), marked `syncStatus = PENDING`. A background sync component then pushes pending records to RTDB; on success it flips them to `SYNCED`.
- **Inbound sync:** an RTDB listener (or periodic pull) writes remote changes into Room, applying the merge rule.
- **Trigger:** Because RTDB has its own offline persistence and realtime listeners, you can let the Firebase SDK handle queueing while online/offline and simply mirror its snapshots into Room. For deferred/batch work use WorkManager with a connectivity constraint. (RTDB's SDK already queues writes locally and flushes on reconnect, which simplifies this versus a REST API.)

**Conflict handling (the 2-device-same-bill case):**
- **Field-level last-write-wins by `updatedAt`** for most fields. Always write child paths, never whole objects.
- **Append-only for order items:** adding items to an open bill is modeled as inserting new `OrderItem` rows (each its own push key), so two operators adding items concurrently never collide — both inserts survive. This is the key insight that makes the open-bill flow safe.
- **Atomic transactions only for stock:** `StockItem.currentQuantity` uses RTDB `runTransaction()` / `ServerValue.increment(-n)` so concurrent sales don't corrupt the count.
- **Bill status transitions** (OPEN→PAID): guard with a transaction or a rule that status only moves forward, so a stale device can't reopen a paid bill. Surface conflicts quietly (a small retry indicator) rather than blocking modals.

**What to sync vs keep local:**
- **Sync:** menu, variants, payment methods, bills, order items, payments, stock items/quantities, expenses, shifts, opname results — everything the owner wants visible across devices and as a backup.
- **Local-only:** UI state, draft/unsubmitted orders in progress, device settings, the auth token cache, transient cart state. Don't sync a half-typed order until it's committed.

### 5. Stock opname feature design
Modern mobile stock-take apps (DealPOS, Lightspeed, iReapPOS's Stock Count, TAG Samurai) converge on this UX:
- **Session-based counting:** the user starts an opname session, which snapshots the current system quantity (`expectedQty`) for each item at that moment.
- **Scan/scroll through items, enter actual count.** For a warung (few SKUs, likely no barcodes on ingredients), a simple scrollable list with a number field per item is right; barcode scanning is optional. Support partial/multiple sessions.
- **System computes variance = actual − expected** per line, and a **cost variance** using purchase price, so the owner sees both quantity lost and rupiah lost.
- **Categorize variance** (spoilage / loss / theft / counting error) per line — this is what turns a raw count into actionable insight.
- **Review before submit:** show total variance, total cost variance, and total stock value; allow recount of a line before final submission. On submit, the system adjusts `currentQuantity` to the counted value and writes an immutable opname record.
- **Locking during opname:** to prevent writes corrupting the count mid-session, the cleanest approach for a tiny shop is a **logical lock** — set a flag (`stockLocked = true`) that pauses automatic stock deduction from sales during the session, queuing those deductions to apply after the opname commits; or simply run opname when the stall is closed. Avoid hard-blocking the POS; instead snapshot expected quantity at session start and apply the counted value as the new baseline, then replay any sales that happened during counting.

### 6. Financial reporting data model
What a warung owner actually needs:
- **Daily:** gross sales, transaction count, sales by payment method, cash expected vs counted (variance), gross profit (sales − COGS), expenses today, net.
- **Weekly/monthly:** revenue trend, best/worst sellers, profit margin per item, expense breakdown by category, payment-method mix.

**Room query patterns (all aggregate from transaction tables):**
- **Daily sales:** `SELECT SUM(grandTotal), COUNT(*) FROM Bill WHERE status='PAID' AND paidAt BETWEEN :start AND :end`.
- **Payment method breakdown:** join `Payment`→`PaymentMethod`, `GROUP BY paymentMethodId`.
- **Per-item profit margin:** join `OrderItem`→`MenuItemIngredient`→`StockBatch` cost; `revenue = SUM(lineTotal)`, `cogs = SUM(quantity × ingredient cost)`, `margin = revenue − cogs`, `GROUP BY menuItemId`.
- **Expense vs revenue:** `SUM(Expense.amount)` vs `SUM(Bill.grandTotal)` over the period.
- **Best sellers:** `SELECT menuItemId, SUM(quantity) qty FROM OrderItem ... GROUP BY menuItemId ORDER BY qty DESC`.

**End-of-day report content:** date/shift, opening float, gross sales, sales-by-payment-method, total discounts, total voids (count + value, with reasons), cash expected vs counted + variance, expenses, gross profit, net. Snapshot this as an immutable record at shift close.

**Money handling gotcha:** store all monetary values as **integer rupiah (`Long`)**, never `Float`/`Double`. Rupiah has no sub-unit in practice and floating point causes rounding errors in totals.

### 7. Android architecture
**Stack: Jetpack Compose + ViewModel + StateFlow + Hilt + Room + Firebase, MVVM with a repository layer.**
- **Layers:** UI (Compose) → ViewModel (exposes `StateFlow<UiState>`) → Repository (single source of truth, owns Room + the Firebase sync) → Room DAO + Firebase data source. The UI observes Room-backed `Flow`s converted to `StateFlow` via `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)`. Keep domain models separate from Room entities to avoid DB concerns leaking into UI.
- **DI:** Hilt. Provide the database, DAOs, repositories, and Firebase references as singletons; provide interface implementations via `@Provides`/`@Binds` modules.
- **Async:** Coroutines + Flow throughout; writes in `viewModelScope.launch`; sync work via WorkManager with a connectivity constraint for deferred pushes.
- **Navigation:** **Order-taking is the launch destination and must be reachable in one tap from anywhere.** Suggested top-level nav (bottom bar or nav rail on tablet): **Order** (default) → **Tables/Bills** → **Reports** → **More** (stock, opname, expenses, menu management, settings, shift close). The cashier flow (pick items → calculator-style quantity entry → choose payment → change calc → done) should be the most optimized path.
- **Modules/packages:** for a personal app, a single module split by feature package is fine and faster to build: `data/` (room, firebase, repository, model), `feature/order`, `feature/tables`, `feature/menu`, `feature/stock`, `feature/reports`, `feature/settings`, `core/` (di, ui theme, common). Multi-module is optional polish, not required at this scale.

### 8. Indonesian context
- **QRIS, GoPay, OVO, transfer = just payment-method labels.** No gateway integration needed; the app records which method was used for reporting. For a warung this is almost certainly a **static QRIS sticker** (customer enters the amount), the cheapest option. On QRIS fees: per Bank Indonesia Deputy Governor Letter No. 26/3/KEP.DpG/2024, effective 1 December 2024, micro-merchant (UMI) QRIS MDR is **0% for transactions ≤ Rp500,000 and 0.3% above that** (vs 0.7% for larger merchant tiers UKE/UME/UBE). Crucially, BI explicitly bans QRIS surcharging — per BI's own guidance ("MDR QRIS Bagi Merchant," bi.go.id): "biaya MDR ini ditanggung oleh merchant dan tidak boleh dibebankan kepada konsumen" (the MDR fee is borne by the merchant and must not be charged to the consumer). So the app should **not** add a QRIS fee line. If the owner wants accurate net revenue, you could optionally store an MDR rate per payment method and compute net settlement, but tracking the method is enough for MVP.
- **PPN/tax — do NOT build tax computation.** A micro warung (sole proprietor, turnover far below Rp4.8 billion/year) is a *pengusaha kecil*: it is **not a PKP and is legally barred from collecting PPN or issuing a faktur pajak**. Its only income-tax obligation is **PPh Final UMKM at 0.5% of monthly turnover**, with **zero tax on the first Rp500 million of annual turnover** for an individual taxpayer — per DJP/pajak.go.id citing UU PPh Pasal 7 ayat (2a): "Wajib Pajak orang pribadi yang memiliki peredaran bruto tertentu...tidak dikenai Pajak Penghasilan atas bagian peredaran bruto sampai dengan Rp500.000.000 dalam 1 tahun pajak" (DDTCNews, 31 May 2026, confirms PP 20/2026 left this Rp500 million exemption unchanged). As of 2026 this 0.5% regime was made **permanent for individuals**: per DJP (PP 20/2026, promulgated 22 April 2026), the rate now applies "tanpa batas waktu" for WP Orang Pribadi and perseroan perorangan with turnover ≤ Rp4.8 billion; the Kementerian UMKM (25 June 2026, via Bisnis.com) reaffirmed "Wajib pajak orang pribadi dan perusahaan perseorangan yang omzetnya di bawah Rp4,8 miliar tetap dapat memanfaatkan tarif PPh Final 0,5 persen tanpa batas waktu." Practical implication: the app needs **no PPN/tax field**; at most, an optional "0.5% PPh Final" monthly turnover report could help the owner set aside tax money — a nice-to-have, not a transaction-level feature.
- **Common Indonesian small-F&B operational patterns** (validated against Moka, Pawoon, Qasir, Kasir Pintar, ESB/Kasflo feature sets): offline-first with auto-sync on reconnect is a headline expectation (local apps explicitly market that the store "keeps running" when internet drops); **open bill / save bill** and **split bill** are standard F&B features; **ingredient-level stock with HPP (COGS) tracking** in grams/ml is expected for accurate margins; **multi-user access with anti-fraud roles** (limiting what staff can void/discount/see) is a common selling point; **QRIS + e-wallet acceptance** is now considered baseline. Your offline-first Room + RTDB design directly matches the pattern these incumbents compete on.

## Recommendations
**Stage 1 — MVP (build first):**
1. Core data model in Room (MenuItem + Variant tables, Table, Bill, OrderItem, Payment, PaymentMethod) with String UUID keys and sync metadata from day one.
2. Order-taking screen as launch destination: item grid → variant/add-on selection → quantity calculator → cash change calculator → payment method → done.
3. Bill model supporting all three flows (dine-in upfront with multiple bills/table, dine-in open bill, grab-and-go upfront).
4. Configurable payment methods (toggle), cash change calculator.
5. Firebase Auth (email/password, 2 accounts, role claim), RTDB sync with field-level LWW + append-only order items, Room as source of truth.
6. **Shift close + cash drawer reconciliation** and **void-with-reason** — promote these into MVP; they are core to cash integrity.
7. Sold-out toggle and basic daily summary.

**Stage 2 — operational depth:**
8. Stock management (batches with purchase price, recipe links, atomic quantity via RTDB transactions), low-stock alerts.
9. Stock opname (session-based, variance + categorization, logical lock).
10. Expense tracking; finance reports (profit margin per item, expense vs revenue, payment-method breakdown).
11. Discounts/promos; best-seller analytics.

**Stage 3 — nice-to-have:**
12. Kitchen/order queue display; peak-hour heatmap; item-level split bill; optional MDR-net and PPh-Final-setaside reports.

**Benchmarks that would change the plan:**
- If simultaneous device count ever exceeds ~100 (impossible here) or monthly download approaches 10 GB → revisit Blaze (still cheap at $1/GB download, $5/GB stored) or batch sync more aggressively.
- If turnover ever approaches **Rp4.8 billion/year** → the business becomes a PKP and you'd need to add PPN handling and faktur logic. Not a concern for a personal warung.
- If conflict/lost-update bugs appear in testing → tighten from LWW toward transaction-guarded status transitions and consider a per-bill version counter.

## Caveats
- **Free-tier limits are real and enforced as hard boundaries** (connections rejected past 100; data access blocked if you downgrade off Blaze with non-default instances). At 1-3 devices this never bites, but don't architect anything that opens many connections.
- **Last-write-wins can silently lose data if you ever overwrite whole objects.** The append-only-order-items and field-level-write discipline is essential, not optional — this is the most likely source of subtle bugs.
- **Some cost figures cited by third-party blogs vary** (e.g. exact MDR thresholds and "free tier sufficiency" framing); the authoritative numbers are Firebase's own docs (1 GB / 10 GB / 100 connections) and Bank Indonesia for QRIS MDR. Treat aggregator blog cost estimates as indicative.
- **The 2026 tax position is confirmed via primary Indonesian government sources** (DJP/pajak.go.id and PP 20/2026, promulgated 22 April 2026): the 0.5% PPh Final regime is permanent for individuals, PKP threshold remains Rp4.8 billion, and the Rp500 million exemption floor stands. Policy discussion about lowering the PKP threshold exists but is not enacted; if the owner's turnover is anywhere near Rp4.8 billion, get a tax professional involved.
- **No receipt printing** is in scope as requested; if the owner later wants digital receipts, RTDB-stored bills can be rendered to a shareable image/PDF or sent via WhatsApp without a printer.