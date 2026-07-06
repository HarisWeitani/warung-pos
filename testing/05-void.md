# 05 — Void Item & Void Bill (Audit)

**Backing code:** `feature/order/BillDetailScreen.kt` (VoidItemDialog, Void Bill dialog, More menu),
`BillDetailViewModel.kt` (`voidItem`, `voidBill`), `domain/usecase/bill/VoidOrderItemUseCase.kt`,
`domain/usecase/bill/VoidBillUseCase.kt`, `data/local/dao/OrderItemDao.kt` (`voidItem`).

**Behaviour (verified):**
- **Void item:** trash icon on each active order-item row (only when bill is OPEN) → dialog with reason radio
  list {Customer Changed Mind, Kitchen Error, Item Unavailable, Test Order, Other}. **Other requires a note**
  (Void Item button disabled until the note is non-blank). On confirm, the item's status becomes VOID, it is
  excluded from active items/total, and the bill total is recalculated. Voided items are **not hard-deleted**.
- **Void bill:** owner-only (role always OWNER) via the top-bar overflow menu → **Void Bill** (only shown when
  bill status is OPEN) → confirm dialog. Sets bill `status=VOID`, records `voidedBy`. **Only OPEN bills are
  voidable** — a PAID bill returns `BillNotVoidableException` (gap D-3). On success the screen pops back.
- Errors surface in an `Error` alert dialog with an `OK` button.

Baseline: BL-2; an OPEN bill with items.

---

## Void order item

### TC-VOID-001 — Void an item with a standard reason excludes it from the total
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / Money
- **Preconditions:** Bill: `Nasi Goreng` (15.000) + `Es Teh` (5.000), Total `Rp 20.000`.
- **Steps:** 1. Tap the trash icon on the `Es Teh` row. 2. In the dialog select **Kitchen Error**. 3. Tap **Void Item**.
- **Expected Result:** `Es Teh` disappears from the active `Order Items` list; Total becomes `Rp 15.000`;
  **Pay** still enabled. OrderItem `Es Teh`: `status=VOID`, `voidReason=KITCHEN_ERROR`, `voidedBy=<username>`,
  row still present in DB (soft void).
- **Postconditions:** Bill grandTotal recalculated to 15.000.
- **Edge Case Notes:** Default reason preselected is Customer Changed Mind; verify chosen reason is persisted.
- **Automation Candidate:** Yes.

### TC-VOID-002 — Void reason "Other" requires a note (button disabled until entered)
- **Priority:** High | **Severity:** Major | **Type:** Validation
- **Preconditions:** Bill with ≥1 item.
- **Steps:** 1. Trash an item. 2. Select **Other**. 3. Observe the **Void Item** button while the note is empty. 4. Type `spilled` in the note. 5. Tap **Void Item**.
- **Expected Result:** With **Other** selected and note empty, **Void Item is disabled**. After typing a note it
  enables; confirming voids the item with `voidReason=OTHER` and the note persisted.
- **Edge Case Notes:** `VoidOrderItemUseCase` also guards server-side: OTHER + blank note →
  `IllegalArgumentException("Note is required when reason is OTHER")`.
- **Automation Candidate:** Yes.

### TC-VOID-003 — Whitespace-only note for "Other" is rejected
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary / Validation
- **Preconditions:** Void dialog, reason Other.
- **Steps:** 1. Type `"   "` (spaces only) in the note. 2. Observe the button.
- **Expected Result:** **Void Item stays disabled** (`note.isNotBlank()` is false for whitespace). Cannot void
  with a blank note.
- **Automation Candidate:** Yes.

### TC-VOID-004 — Cancel the void dialog leaves the item intact
- **Priority:** Medium | **Severity:** Minor | **Type:** Cancel-midway
- **Preconditions:** Void dialog open on an item.
- **Steps:** 1. Select a reason. 2. Tap **Cancel** (or tap outside).
- **Expected Result:** Dialog closes; the item is unchanged (still active); total unchanged.
- **Automation Candidate:** Yes.

### TC-VOID-005 — Void all reasons are selectable and persisted
- **Priority:** Low | **Severity:** Minor | **Type:** Functional (data)
- **Preconditions:** A bill with 4 items.
- **Steps:** 1. Void item 1 = Customer Changed Mind. 2. Void item 2 = Kitchen Error. 3. Void item 3 = Item Unavailable. 4. Void item 4 = Test Order.
- **Expected Result:** Each void persists its distinct `voidReason` enum
  (CUSTOMER_CHANGE / KITCHEN_ERROR / ITEM_UNAVAILABLE / TEST). All excluded from total.
- **Edge Case Notes:** Used later in Z-report void breakdown (`08-day-management.md`).
- **Automation Candidate:** Yes.

### TC-VOID-006 — Cannot void an item once the bill is PAID (no trash icon)
- **Priority:** High | **Severity:** Major | **Type:** Negative / State
- **Preconditions:** A PAID bill's detail (reached before auto-pop, or via a stale screen).
- **Steps:** 1. Observe order-item rows on a non-OPEN bill.
- **Expected Result:** No trash icon is rendered (`canVoid = status==OPEN` is false). Items cannot be voided
  after payment.
- **Automation Candidate:** No (timing).

### TC-VOID-007 — Voided item quantity is fully excluded (partial-void not supported)
- **Priority:** Medium | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** Bill with `Nasi Goreng 3 × Rp 15.000` = 45.000.
- **Steps:** 1. Trash the `Nasi Goreng` line. 2. Void it.
- **Expected Result:** The **entire** line (all 3) is voided; there is no way to void just 1 of 3. Total drops by
  45.000. (Document: no partial-quantity void.)
- **Automation Candidate:** Yes.

### TC-VOID-008 — Double-tap Void Item does not error or double-void
- **Priority:** Low | **Severity:** Minor | **Type:** User behaviour
- **Preconditions:** Void dialog on an item, valid reason.
- **Steps:** 1. Double-tap **Void Item**.
- **Expected Result:** Item voided once; dialog closes; no crash. (Dialog dismisses after first tap.)
- **Automation Candidate:** Yes.

---

## Void entire bill

### TC-VOID-020 — Void an entire OPEN bill (owner)
- **Priority:** High | **Severity:** Critical | **Type:** Functional / Audit
- **Preconditions:** OPEN bill with ≥1 item.
- **Steps:** 1. On Bill Detail, tap the top-bar overflow (⋮). 2. Tap **Void Bill**. 3. In the confirm dialog
  (`This will void the entire bill and all its items. This cannot be undone.`) tap **Void Bill**.
- **Expected Result:** Bill Detail closes (auto-pop on `billVoided`). The bill is absent from `Orders`. Bill
  row: `status=VOID`, `voidedBy=<username>`. Items remain in DB.
- **Postconditions:** Bill VOID; excluded from revenue.
- **Edge Case Notes:** The ⋮ menu with **Void Bill** appears only when `isOwner && status==OPEN` — always the
  case for the single OWNER user on an open bill.
- **Automation Candidate:** Yes.

### TC-VOID-021 — Cancel void-bill confirmation
- **Priority:** Medium | **Severity:** Minor | **Type:** Cancel-midway
- **Preconditions:** OPEN bill, ⋮ → Void Bill dialog open.
- **Steps:** 1. Tap **Cancel**.
- **Expected Result:** Dialog closes; bill remains OPEN and editable.
- **Automation Candidate:** Yes.

### TC-VOID-022 — Void Bill option is hidden on a non-OPEN bill
- **Priority:** Medium | **Severity:** Major | **Type:** State
- **Preconditions:** A PAID or VOID bill detail (if reachable).
- **Steps:** 1. Look for the top-bar overflow (⋮) menu.
- **Expected Result:** The ⋮ action is **not** shown (condition requires status OPEN). There is no path to void
  a paid bill from the UI.
- **Automation Candidate:** No.

### TC-VOID-023 — Voiding a PAID bill via the use case is rejected (gap D-3 verification)
- **Priority:** Medium | **Severity:** Major | **Type:** Negative / Gap verification
- **Preconditions:** A PAID bill.
- **Steps:** 1. Attempt to void the PAID bill (only reachable via a stale/back-stacked detail screen or a
  direct use-case-level test).
- **Expected Result:** `VoidBillUseCase` returns `BillNotVoidableException` → an `Error` dialog shows the
  message; the bill stays PAID. This **contradicts FR-VOID-3** (which allows voiding paid bills). Log a Major
  gap: there is no supported way to void/refund a paid bill in the app.
- **Edge Case Notes:** Important for real ops: a mistaken cash payment cannot be reversed in-app.
- **Automation Candidate:** No.

### TC-VOID-024 — Void error dialog is dismissible
- **Priority:** Low | **Severity:** Trivial | **Type:** UI
- **Preconditions:** Trigger a void error (e.g. TC-VOID-023).
- **Steps:** 1. On the `Error` dialog tap **OK**.
- **Expected Result:** Dialog dismisses (`dismissVoidError`); the bill screen remains usable.
- **Automation Candidate:** Yes.

### TC-VOID-025 — Voided bill and items appear in the Z-report void summary
- **Priority:** High | **Severity:** Major | **Type:** Reporting / Audit
- **Preconditions:** During an OPEN day, void ≥2 order items (across bills) with different reasons and void one
  whole open bill's items.
- **Steps:** 1. Perform the voids. 2. Close the day (`08-day-management.md`). 3. View the Z-report.
- **Expected Result:** Z-report snapshot includes `voidCount` and `voidValue` reflecting all VOID order items in
  the shift (`totalVoidsForShift` counts `order_items.status='VOID'` for bills in the shift, summing lineTotal).
- **Edge Case Notes:** Void **bill** does not itself change item statuses; only individually-voided **items** are
  counted by the void query. A whole-bill void with un-voided items contributes to voidCount **only** if those
  items are VOID. Verify how a whole-bill void is represented and whether its items are counted — log if the
  audit under-counts whole-bill voids (Major).
- **Automation Candidate:** Yes (DB/report inspection).

### TC-VOID-026 — Voided item never re-enters totals after further edits
- **Priority:** Medium | **Severity:** Major | **Type:** Data integrity
- **Preconditions:** Bill with items A, B; A voided.
- **Steps:** 1. Void A. 2. Add a new item C. 3. Increment B.
- **Expected Result:** Total always = active(B)+active(C); A stays excluded through every recalculation. A's row
  is never resurrected.
- **Automation Candidate:** Yes.
