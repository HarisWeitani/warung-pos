# 03 — Order Taking & Bill Lifecycle

**Backing code:** `feature/order/OrderScreen.kt`, `OrderViewModel.kt` (`createBill()`),
`feature/order/BillDetailScreen.kt`, `BillDetailViewModel.kt` (`onMenuItemTapped`, `addItem`,
`confirmVariantSelection`, `recalculateBillTotals`), `feature/order/component/VariantSelectionSheet.kt`,
`data/repository/BillRepositoryImpl.kt`, `data/local/dao/BillDao.kt`, `OrderItemDao`, `MenuItemDao.observeAvailable`.

**Shipped flow (verified — differs from PRD cart model, see `00-assumptions-and-gaps.md` §C):**
Order tab lists **open bills** + a **`+` FAB**. `+` creates an empty OPEN bill (`sessionLabel="Counter - HH:mm"`,
grandTotal 0) and navigates to Bill Detail. On Bill Detail, tap menu rows to add items:
- no-variant item → adds qty 1; tapping the same no-variant line again **increments** its quantity.
- item with variant groups → opens a bottom sheet; **required** groups must be satisfied; confirm adds a new
  line (qty 1). Variants add `priceDelta` to the unit price.
There is **no `+/-` stepper** and **no way to reduce quantity** except voiding the whole line. **"Pay"** is
enabled only when ≥1 active (non-void) item exists.

Baseline for most cases: **BL-2 seeded menu** — Category "Makanan" with `Nasi Goreng` Rp15000 (no variants);
`Ayam` Rp20000 with a **required** SINGLE group "Spice" {Mild +0, Hot +2000} and an optional MULTIPLE group
"Add-ons" {Telur +5000, Kerupuk +2000}; Category "Minuman" with `Es Teh` Rp5000.

> **Setup note (see assumption A9 / gap D-18):** the app has **no in-app category creation**, so the named
> categories `Makanan`/`Minuman` must be seeded via RTDB `/menuCategories` or the Room DB before running
> category cases. Menu **items** can be created in the app (they will be `Uncategorized` unless a category is
> seeded and selected). Cases that only need items (not named categories) work on the uncategorized group.

---

## Order list (Order tab)

### TC-ORD-001 — Empty state on Order tab
- **Priority:** High | **Severity:** Minor | **Type:** Empty state
- **Preconditions:** BL-1 (no open bills).
- **Steps:** 1. Open the app to the Order tab.
- **Expected Result:** Title `Orders`; centered text `No open orders` and `Tap + to create a new order`; a `+`
  FAB bottom-right.
- **Automation Candidate:** Yes.

### TC-ORD-002 — Create a new (empty) bill via FAB
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional
- **Preconditions:** BL-2 (menu exists), an OPEN day.
- **Steps:** 1. On Order tab tap the `+` FAB.
- **Expected Result:** Navigates to **Bill Detail** titled `Counter - HH:mm` (current time). Sections shown:
  `Order Items` with `No items yet. Add from the menu below.`, then `Add Items` with category chips and menu
  rows. Bottom bar shows `Total Rp 0` and a **Pay** button that is **disabled**.
- **Postconditions:** One OPEN bill row exists (grandTotal 0, shiftId = current shift).
- **Edge Case Notes:** `createBill()` calls `ensureDayOpenUseCase()` first, so a bill can be created even right
  after midnight rollover.
- **Automation Candidate:** Yes.

### TC-ORD-003 — Newly created empty bill appears in the Order list on back-navigation
- **Priority:** High | **Severity:** Major | **Type:** State
- **Preconditions:** BL-2.
- **Steps:** 1. Create a bill (`+`). 2. Without adding items, press Back.
- **Expected Result:** Order list now shows one card `Counter - HH:mm` with amount `Rp 0`. (Empty bills persist
  as OPEN — see cleanup note.)
- **Edge Case Notes:** Empty OPEN bills will later **block day close** (TC-DAY-*). Flag: no auto-cleanup of
  abandoned empty bills.
- **Automation Candidate:** Yes.

### TC-ORD-004 — Multiple open bills coexist and are ordered newest-first
- **Priority:** Medium | **Severity:** Major | **Type:** Functional / Sorting
- **Preconditions:** BL-2.
- **Steps:** 1. Create bill A, add `Es Teh`, Back. 2. Create bill B, add `Nasi Goreng`, Back.
- **Expected Result:** Order list shows two cards; **B above A** (`observeOpenBills` orders by `createdAt DESC`).
  Amounts: B `Rp 15.000`, A `Rp 5.000`.
- **Automation Candidate:** Yes.

### TC-ORD-005 — Tap an existing open bill card opens its detail
- **Priority:** High | **Severity:** Major | **Type:** Navigation
- **Preconditions:** ≥1 open bill.
- **Steps:** 1. Tap a bill card.
- **Expected Result:** Bill Detail for that bill, showing its existing items and running total.
- **Automation Candidate:** Yes.

---

## Adding items (Bill Detail)

### TC-ORD-010 — Add a no-variant item (single tap adds qty 1)
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional
- **Preconditions:** New empty bill open.
- **Steps:** 1. Ensure category `Makanan` chip is selected. 2. Tap the `Nasi Goreng` row.
- **Expected Result:** An `Order Items` row appears: `Nasi Goreng`, `1 × Rp 15.000`, line total `Rp 15.000`.
  Bottom-bar Total becomes `Rp 15.000`; **Pay** becomes enabled.
- **Postconditions:** One OrderItem (qty 1, priceSnapshot 15000, lineTotal 15000, status ORDERED, PENDING).
- **Automation Candidate:** Yes.

### TC-ORD-011 — Tapping the same no-variant item again increments quantity (not a new line)
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / Boundary
- **Preconditions:** Bill with one `Nasi Goreng` (qty 1).
- **Steps:** 1. Tap the `Nasi Goreng` menu row two more times.
- **Expected Result:** Still a single `Nasi Goreng` order line, now `3 × Rp 15.000`, line total `Rp 45.000`;
  Total `Rp 45.000`. No duplicate lines.
- **Edge Case Notes:** `addItem` finds an existing active line with the same `menuItemId` **and empty variants**
  and increments it (`existing.priceSnapshot * newQty`).
- **Automation Candidate:** Yes.

### TC-ORD-012 — Rapid repeated taps on a no-variant item are all counted (no lost taps)
- **Priority:** High | **Severity:** Major | **Type:** Performance-scenario / User behaviour
- **Preconditions:** New empty bill.
- **Steps:** 1. Tap `Es Teh` **10 times** as fast as possible.
- **Expected Result:** Quantity resolves to `10` (line `10 × Rp 5.000` = `Rp 50.000`). No crash, no lost or
  double-counted taps beyond the 10 intended.
- **Edge Case Notes:** Each tap launches a coroutine that reads the current active items then upserts. Because
  each tap re-reads `getActiveItems`, very fast taps could theoretically read a stale qty and under-count →
  this case specifically probes that race. Record the final qty; if < 10, log a data-race defect (Major).
- **Automation Candidate:** Yes (but verify the race with a fling of taps).

### TC-ORD-013 — Add different no-variant items → separate lines
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** New bill.
- **Steps:** 1. Tap `Nasi Goreng`. 2. Switch chip to `Minuman`. 3. Tap `Es Teh`.
- **Expected Result:** Two order lines: `Nasi Goreng 1 × Rp 15.000` and `Es Teh 1 × Rp 5.000`; Total `Rp 20.000`.
- **Automation Candidate:** Yes.

### TC-ORD-014 — Category filter chips scope the menu picker
- **Priority:** Medium | **Severity:** Minor | **Type:** Filter
- **Preconditions:** BL-2, bill open.
- **Steps:** 1. Observe default selected chip (first category). 2. Tap the `Minuman` chip.
- **Expected Result:** Menu rows update to show only `Minuman` items (`Es Teh`); `Makanan` items hidden. The
  Order Items section above is unaffected.
- **Edge Case Notes:** Default selected category = first category id when none chosen.
- **Automation Candidate:** Yes.

### TC-ORD-015 — Item with a required variant group opens the sheet and blocks confirm until satisfied
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / Validation
- **Preconditions:** Bill open, `Ayam` has required SINGLE group "Spice".
- **Steps:** 1. Tap the `Ayam` row. 2. In the variant sheet, do **not** select a Spice option; attempt to Confirm/Add.
- **Expected Result:** The bottom sheet appears listing groups. The Add/Confirm control is **disabled (or
  rejects)** until a Spice option is chosen — the required SINGLE group must have exactly one selection.
- **Edge Case Notes:** Verify against `VariantSelectionSheet.kt` for the exact enable rule (required groups must
  be fulfilled). No line is added while blocked.
- **Automation Candidate:** Yes.

### TC-ORD-016 — Select required variant + optional add-ons, price reflects deltas
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / Money
- **Preconditions:** Bill open.
- **Steps:** 1. Tap `Ayam`. 2. Select Spice `Hot` (+2000). 3. Select add-ons `Telur` (+5000) and `Kerupuk` (+2000). 4. Confirm.
- **Expected Result:** A new line `Ayam` with `1 × Rp 29.000` (20000+2000+5000+2000), line total `Rp 29.000`.
  Total updates accordingly.
- **Postconditions:** OrderItem `priceSnapshot=29000`, `selectedVariants` holds the 3 selections.
- **Edge Case Notes:** MULTIPLE group allows 0..n selections; SINGLE group allows exactly 1.
- **Automation Candidate:** Yes.

### TC-ORD-017 — Two `Ayam` with different variants are separate lines (not merged)
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** Bill open.
- **Steps:** 1. Add `Ayam` Spice=Mild. 2. Add `Ayam` Spice=Hot +Telur.
- **Expected Result:** Two distinct `Ayam` lines (Rp 20.000 and Rp 27.000). Variant items are **never**
  quantity-merged (`existing` lookup only applies when `variants.isEmpty()`).
- **Edge Case Notes:** Even identical variant selections create a **new line** each tap (no merge for variant
  items) — document as intended, and note the qty can only ever be 1 per variant line.
- **Automation Candidate:** Yes.

### TC-ORD-018 — Adding the same variant item twice yields two qty-1 lines (no increment path)
- **Priority:** Medium | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** Bill open.
- **Steps:** 1. Add `Ayam` Spice=Hot. 2. Add `Ayam` Spice=Hot again (same selections).
- **Expected Result:** Two separate `Ayam` lines each `1 × Rp 22.000`. There is no way to make a variant line
  qty 2. (Flag as UX limitation vs PRD +/- adjust.)
- **Automation Candidate:** Yes.

### TC-ORD-019 — Dismiss the variant sheet without confirming adds nothing
- **Priority:** Medium | **Severity:** Minor | **Type:** Negative / Cancel-midway
- **Preconditions:** Bill open.
- **Steps:** 1. Tap `Ayam` (sheet opens). 2. Select Spice Hot. 3. Swipe the sheet down / tap scrim to dismiss.
- **Expected Result:** Sheet closes; **no** order line added; Total unchanged.
- **Edge Case Notes:** `dismissVariantSheet()` nulls the sheet state without calling `addItem`.
- **Automation Candidate:** Yes.

### TC-ORD-020 — Back mid-variant-selection cancels cleanly
- **Priority:** Medium | **Severity:** Minor | **Type:** Cancel-midway / Navigation
- **Preconditions:** Variant sheet open.
- **Steps:** 1. Press system Back while the sheet is open.
- **Expected Result:** The sheet closes (Back consumed by the sheet), returning to Bill Detail with no item
  added; a second Back leaves the bill. No crash.
- **Automation Candidate:** No (sheet Back handling varies).

---

## Sold-out & availability in the order picker

### TC-ORD-030 — Sold-out item is STILL orderable (gap D-1, defect verification)
- **Priority:** High | **Severity:** Major | **Type:** Negative / Gap verification
- **Preconditions:** BL-2. Mark `Es Teh` sold out (More → Menu Management → toggle sold-out on `Es Teh`).
- **Steps:** 1. Create/open a bill. 2. Select the `Minuman` chip. 3. Observe the `Es Teh` row and tap it.
- **Expected Result (current behaviour):** `Es Teh` appears **normally (not greyed, no "Sold Out" badge)** and
  is **tappable** — tapping adds it to the bill. This **violates FR-ORDER-3** (sold-out should be greyed &
  non-tappable). Log a Major defect; the expected *shipped* result is that it is orderable.
- **Edge Case Notes:** `observeAvailable()` filters only `isAvailable=1`; `isSoldOut` is ignored by the picker.
- **Automation Candidate:** Yes.

### TC-ORD-031 — Hidden (unavailable) item does NOT appear in the order picker
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** BL-2. Hide `Nasi Goreng` (Menu Management → hide → confirm).
- **Steps:** 1. Open a bill. 2. Select `Makanan`.
- **Expected Result:** `Nasi Goreng` is **absent** from the picker (isAvailable=false filtered out). Existing
  bills that already contain it keep their snapshot line.
- **Automation Candidate:** Yes.

### TC-ORD-032 — Empty category / empty menu shows "No items available"
- **Priority:** Low | **Severity:** Minor | **Type:** Empty state
- **Preconditions:** A category with no available items (or BL-1 empty menu).
- **Steps:** 1. Open a bill. 2. Select the empty category (or observe with no menu).
- **Expected Result:** Menu picker shows `No items available`. Pay stays disabled (no items can be added).
- **Automation Candidate:** Yes.

---

## Totals & snapshots

### TC-ORD-040 — Running total equals sum of active line totals
- **Priority:** Critical | **Severity:** Critical | **Type:** Money / Functional
- **Preconditions:** Bill open.
- **Steps:** 1. Add `Nasi Goreng` ×2 (Rp 30.000). 2. Add `Es Teh` ×1 (Rp 5.000). 3. Add `Ayam` Hot (Rp 22.000).
- **Expected Result:** Bottom-bar Total = `Rp 57.000`. Each add triggers `recalculateBillTotals` (subtotal =
  grandTotal, discount 0).
- **Postconditions:** Bill `subtotal=grandTotal=57000`.
- **Automation Candidate:** Yes.

### TC-ORD-041 — Price snapshot is frozen at add time (later menu price edit doesn't change the line)
- **Priority:** High | **Severity:** Critical | **Type:** Data integrity (FR-BILL-6, FR-MENU-6)
- **Preconditions:** Bill with `Nasi Goreng` (snapshot Rp 15.000).
- **Steps:** 1. Leave the bill open. 2. In another nav, More → Menu Management → edit `Nasi Goreng` price to Rp 18.000, save. 3. Return to the bill.
- **Expected Result:** The existing `Nasi Goreng` line still shows `Rp 15.000` (snapshot preserved). A **newly**
  added `Nasi Goreng` line would use `Rp 18.000`.
- **Edge Case Notes:** `nameSnapshot`/`priceSnapshot` are copied into the OrderItem at add time.
- **Automation Candidate:** Yes.

### TC-ORD-042 — Name snapshot frozen (rename menu item doesn't rename existing lines)
- **Priority:** Medium | **Severity:** Major | **Type:** Data integrity
- **Preconditions:** Bill with `Es Teh`.
- **Steps:** 1. Rename menu item `Es Teh` → `Iced Tea` in Menu Management. 2. Return to the bill.
- **Expected Result:** Existing line still reads `Es Teh`. New additions read `Iced Tea`.
- **Automation Candidate:** Yes.

---

## Bill state machine & concurrency

### TC-ORD-050 — Paid bill leaves the open list and its detail is read-only
- **Priority:** High | **Severity:** Critical | **Type:** State
- **Preconditions:** A bill with items, then paid (see `04-payment.md`).
- **Steps:** 1. Pay the bill fully. 2. Observe the Order list. 3. Attempt to reopen the paid bill (if reachable via history/detail).
- **Expected Result:** The bill disappears from `Orders` (only OPEN bills listed). If its detail is shown, there
  is **no** bottom Pay bar, **no** Add-Items interaction path for a non-OPEN bill (menu rows disabled;
  `enabled = status==OPEN`), and no void controls. Status only moves forward (OPEN→PAID).
- **Edge Case Notes:** BillDetail auto-pops when `isBillPaid` becomes true.
- **Automation Candidate:** Yes.

### TC-ORD-051 — Cannot add items to a PAID bill
- **Priority:** High | **Severity:** Major | **Type:** Negative
- **Preconditions:** A PAID bill open in detail (navigate before it pops, or via a stale screen).
- **Steps:** 1. Tap a menu row on a paid bill's detail.
- **Expected Result:** Menu rows are disabled (`enabled=false`), so tapping does nothing; no new OrderItem is
  written. (The screen also auto-navigates back on paid.)
- **Automation Candidate:** No (timing-sensitive).

### TC-ORD-052 — Voided bill leaves the open list
- **Priority:** High | **Severity:** Major | **Type:** State
- **Preconditions:** An OPEN bill with items.
- **Steps:** 1. Void the entire bill (see `05-void.md`, TC-VOID-020).
- **Expected Result:** Bill Detail auto-pops (billVoided → popBackStack); the bill no longer appears in `Orders`
  (status VOID). It remains in the DB for audit.
- **Automation Candidate:** Yes.

### TC-ORD-053 — Two devices add different items to the SAME open bill (append-only, no loss)
- **Priority:** High | **Severity:** Critical | **Type:** Sync / Data integrity
- **Preconditions:** E3 two devices online; an OPEN bill created on Device A and synced to B.
- **Steps:** 1. On A open the bill, add `Nasi Goreng`. 2. On B open the same bill, add `Es Teh` (near-simultaneously). 3. Wait for sync on both.
- **Expected Result:** Both devices eventually show **both** items (`Nasi Goreng` + `Es Teh`). No item is lost;
  no duplicate. Order items are append-only with distinct UUIDs.
- **Edge Case Notes:** Bill total is recomputed per device from active items; after both items sync, both
  devices should converge to the same grandTotal. If one device shows a stale total, log the LWW-on-bill-row
  timing behaviour (Major).
- **Automation Candidate:** No (2 devices).

### TC-ORD-054 — Two devices both create bills offline; both survive after reconnect
- **Priority:** High | **Severity:** Critical | **Type:** Sync / Offline
- **Preconditions:** E3 both devices offline.
- **Steps:** 1. A creates bill + item (offline). 2. B creates a different bill + item (offline). 3. Bring both online. 4. Wait for flush.
- **Expected Result:** After sync, **both** devices list **both** bills (distinct UUIDs, no collision). No data
  loss, no duplication.
- **Automation Candidate:** No.

### TC-ORD-055 — Back button from Bill Detail returns to the Order list preserving the bill
- **Priority:** Medium | **Severity:** Minor | **Type:** Navigation
- **Preconditions:** Bill with items open.
- **Steps:** 1. Tap the top-bar Back arrow.
- **Expected Result:** Returns to Order list; the bill (with its items and total) is listed and re-openable.
- **Automation Candidate:** Yes.

### TC-ORD-056 — Process death while on Bill Detail restores the correct bill
- **Priority:** High | **Severity:** Major | **Type:** Recovery
- **Preconditions:** Bill with 2 items open.
- **Steps:** 1. `adb shell am kill com.wfx.warungpos` (simulate background process death) while on the bill (put app to background first). 2. Reopen from Recents. 3. Unlock (PIN).
- **Expected Result:** After unlock, navigating to the bill (via Order list) shows the same 2 items and total
  from Room. No items lost. (Note: the app re-locks on process death — expected per TC-AUTH-030.)
- **Edge Case Notes:** `billId` is restored from `SavedStateHandle`/route; items read from Room.
- **Automation Candidate:** No.

### TC-ORD-057 — Fast double-tap the Pay button does not create two payment screens / double navigation
- **Priority:** Medium | **Severity:** Major | **Type:** User behaviour
- **Preconditions:** Bill with ≥1 item.
- **Steps:** 1. Double-tap **Pay** rapidly.
- **Expected Result:** Exactly one Payment screen opens; Back returns to the bill once. No crash, no stacked
  duplicate payment screens.
- **Automation Candidate:** Yes.

### TC-ORD-058 — Bill total after voiding all items becomes 0 and Pay disables
- **Priority:** High | **Severity:** Major | **Type:** Boundary / State
- **Preconditions:** Bill with two items.
- **Steps:** 1. Void item 1 (reason Customer Changed Mind). 2. Void item 2.
- **Expected Result:** After each void the total drops; after the last, `Order Items` shows only struck items
  are excluded → active list empty → Total `Rp 0` and **Pay disabled** (`activeItems.isNotEmpty()==false`).
- **Edge Case Notes:** Prevents paying an all-void (zero) bill from the UI.
- **Automation Candidate:** Yes.
