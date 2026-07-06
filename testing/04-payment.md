# 04 — Payment & Change Calculation

**Backing code:** `feature/payment/PaymentScreen.kt`, `PaymentViewModel.kt`,
`domain/usecase/payment/ProcessPaymentUseCase.kt`, `CalculateChangeUseCase.kt`, `PaymentRow.kt`,
`data/repository/PaymentRepositoryImpl.kt` (`processPaymentTransaction`), `domain/usecase/stock/DeductStockForBillUseCase.kt`.

**Behaviour (verified):** Payment screen shows the order summary (active items), the grand total, a **single**
payment-method radio list (enabled methods only), one **Amount Tendered** field (digits only), and a computed
**Change** row (shown only when tender is non-blank; clamped to ≥0). On **Confirm Payment**:
- `ProcessPaymentUseCase` requires an OPEN day, bill OPEN (else `BillAlreadyPaidException`), `tendered ≥ amount`
  (else `InsufficientTenderedAmountException`), and `Σamount ≥ grandTotal` (else `InsufficientPaymentException`).
- The UI always sends **one** row with `amount = bill.grandTotal` and `tenderedAmount = entered value or (blank → grandTotal)`.
- On success: payments inserted + bill set PAID (`paidAt=now`) in one transaction, stock deducted for recipes,
  and the screen shows `Payment Complete!` + `Change: RpX`, then navigates back to the Order tab.

**Split payment is NOT available in the UI** (gap D-2). Money is `Long` (integer Rupiah) end-to-end.

Baseline: BL-2 menu; an OPEN bill with known items.

---

### TC-PAY-001 — Cash exact amount → change 0, bill PAID
- **Priority:** Critical | **Severity:** Blocker | **Type:** Functional / Money
- **Preconditions:** Bill total `Rp 20.000` (e.g. Nasi Goreng + Es Teh).
- **Test Data:** method Tunai, tender `20000`.
- **Steps:** 1. Tap **Pay**. 2. Confirm the Total shows `Rp 20.000`. 3. Select **Tunai**. 4. Enter tender `20000`. 5. Verify the Change row reads `Rp 0`. 6. Tap **Confirm Payment**.
- **Expected Result:** `Payment Complete!` with `Change: Rp 0`, then auto-return to the Order tab where the bill
  is gone. Bill row: `status=PAID`, `paidAt` set. One Payment row `amount=20000`, `change=0`, method `pm_tunai`.
- **Postconditions:** Bill PAID; payment persisted PENDING (syncs when online).
- **Automation Candidate:** Yes.

### TC-PAY-002 — Cash overpaid → correct positive change
- **Priority:** Critical | **Severity:** Critical | **Type:** Money
- **Preconditions:** Bill total `Rp 15.000`.
- **Test Data:** Tunai, tender `20000`.
- **Steps:** 1. Pay. 2. Tunai. 3. Tender `20000`. 4. Read Change. 5. Confirm.
- **Expected Result:** Change row `Rp 5.000` before confirm; success screen `Change: Rp 5.000`. Payment
  `amount=15000`, `change=5000`.
- **Edge Case Notes:** `change = max(0, tendered − amount)`.
- **Automation Candidate:** Yes.

### TC-PAY-003 — Cash underpaid → blocked with insufficient-tender error
- **Priority:** Critical | **Severity:** Critical | **Type:** Negative / Boundary
- **Preconditions:** Bill total `Rp 20.000`.
- **Test Data:** Tunai, tender `15000`.
- **Steps:** 1. Pay. 2. Tunai. 3. Tender `15000` (Change row shows `Rp 0` because it's clamped). 4. Confirm Payment.
- **Expected Result:** Payment is **rejected** — an error surfaces (from `InsufficientTenderedAmountException`;
  the ViewModel puts `e.message` into `state.error`). Bill stays **OPEN**; no Payment row written; no stock
  deducted; the screen does not show success.
- **Edge Case Notes:** The clamped Change=`Rp 0` display can mislead the cashier into thinking it's fine — the
  guard is enforced only at Confirm. Note the error is not rendered in `PaymentScreen` (there is no error UI in
  the composable) → verify whether the user sees *any* feedback; if not, log a Major UX defect (silent failure:
  button re-enables, nothing happens).
- **Automation Candidate:** Yes.

### TC-PAY-004 — Blank tender defaults to exact grand total (change 0) and succeeds
- **Priority:** High | **Severity:** Major | **Type:** Boundary
- **Preconditions:** Bill total `Rp 12.000`.
- **Steps:** 1. Pay. 2. Select a method. 3. Leave **Amount Tendered** empty. 4. Tap **Confirm Payment**.
- **Expected Result:** Succeeds with `tenderLong = grandTotal` → `amount=12000`, `change=0`. Success screen
  `Change: Rp 0`.
- **Edge Case Notes:** `confirmPayment` uses `tenderAmount.toLongOrNull() ?: bill.grandTotal`. The Change row is
  hidden (tender blank) but the success screen shows Rp 0.
- **Automation Candidate:** Yes.

### TC-PAY-005 — Non-cash method (QRIS) exact, no surcharge
- **Priority:** High | **Severity:** Major | **Type:** Functional / Business rule
- **Preconditions:** Bill total `Rp 30.000`.
- **Steps:** 1. Pay. 2. Select **QRIS**. 3. Leave tender blank (or enter `30000`). 4. Confirm.
- **Expected Result:** Bill PAID; Payment `amount=30000`, method `pm_qris`, `change=0`. **No MDR/surcharge**
  is added (amount equals grandTotal). Change row (if tender entered exact) = `Rp 0`.
- **Edge Case Notes:** FR-PAYMENT-6 — QRIS is label-only. Confirms cash-vs-noncash both pay exactly grandTotal.
- **Automation Candidate:** Yes.

### TC-PAY-006 — Non-cash with tender less than total is still rejected
- **Priority:** Medium | **Severity:** Major | **Type:** Negative
- **Preconditions:** Bill total `Rp 30.000`.
- **Steps:** 1. Pay. 2. Select QRIS. 3. Enter tender `10000`. 4. Confirm.
- **Expected Result:** Rejected (tendered `10000` < amount `30000` → InsufficientTenderedAmountException). Bill
  stays OPEN. (The guard is method-agnostic.)
- **Edge Case Notes:** For non-cash, entering any tender < total blocks; safer to leave blank (TC-PAY-004).
- **Automation Candidate:** Yes.

### TC-PAY-007 — Only enabled payment methods are selectable
- **Priority:** High | **Severity:** Major | **Type:** Functional / Config
- **Preconditions:** Disable **GoPay** and **OVO** (More → Payment Methods).
- **Steps:** 1. Open a bill and tap **Pay**. 2. Inspect the method list.
- **Expected Result:** Only **Tunai, QRIS, Transfer Bank** are listed (`observeActivePaymentMethods`). GoPay/OVO
  absent. Default selection is the first active method.
- **Automation Candidate:** Yes.

### TC-PAY-008 — All payment methods disabled → cannot confirm
- **Priority:** Medium | **Severity:** Major | **Type:** Negative / Boundary
- **Preconditions:** Disable **all** payment methods in settings.
- **Steps:** 1. Open a bill, tap **Pay**. 2. Observe the method list and the Confirm button.
- **Expected Result:** No methods listed; `selectedMethodId` is null → **Confirm Payment** is **disabled**
  (`enabled = … && selectedMethodId != null`). Bill cannot be paid until a method is re-enabled.
- **Edge Case Notes:** Also verify no crash and Back works.
- **Automation Candidate:** Yes.

### TC-PAY-009 — Change updates live as tender is edited
- **Priority:** Medium | **Severity:** Minor | **Type:** UI / Reactive
- **Preconditions:** Bill total `Rp 18.000`.
- **Steps:** 1. Pay. 2. Type `2` → `20` → `200` → `2000` → `20000` in Amount Tendered, observing the Change row.
- **Expected Result:** Change row appears once tender is non-blank; shows `Rp 0` while tender ≤ total, then
  `Rp 2.000` at `20000`. Recomputed on every keystroke via `onTenderChange`.
- **Automation Candidate:** Yes.

### TC-PAY-010 — Tender field strips non-digits
- **Priority:** Medium | **Severity:** Minor | **Type:** Input validation
- **Preconditions:** Payment screen.
- **Steps:** 1. Type `1a2b.3,4` in Amount Tendered.
- **Expected Result:** Field holds `1234` (digits only; `value.filter { isDigit() }`).
- **Automation Candidate:** Yes.

### TC-PAY-011 — Very large tender (overflow probe)
- **Priority:** Low | **Severity:** Major | **Type:** Boundary
- **Preconditions:** Bill total `Rp 10.000`.
- **Steps:** 1. Pay. 2. Enter a 15+ digit tender (e.g. `999999999999999`). 3. Observe Change; Confirm.
- **Expected Result:** Change computes as `tender − 10000` without crash if it fits in `Long`
  (≤ 9,223,372,036,854,775,807). If a longer string is entered, `toLongOrNull()` returns null → tender treated
  as `0` on the live-change path (Change `Rp 0`); on confirm, blank/overflow falls back to grandTotal. No crash
  either way. Record actual behaviour at the `Long.MAX` boundary.
- **Edge Case Notes:** Probes integer overflow and `toLongOrNull` null handling. Money must never wrap negative.
- **Automation Candidate:** Yes.

### TC-PAY-012 — Paying an already-paid bill is prevented (idempotency)
- **Priority:** High | **Severity:** Critical | **Type:** Negative / Data integrity
- **Preconditions:** A bill already PAID; open its Payment screen from a stale back stack if possible, OR use
  two devices (see TC-PAY-020).
- **Steps:** 1. With the bill already PAID, invoke Confirm Payment again (stale screen).
- **Expected Result:** `ProcessPaymentUseCase` returns `BillAlreadyPaidException`; no second Payment row is
  written; total is not double-counted. Bill stays PAID once.
- **Edge Case Notes:** On a single device the screen auto-navigates away on success, so reaching this needs a
  stale/back-stacked screen or the multi-device case.
- **Automation Candidate:** No.

### TC-PAY-013 — Payment blocked when no day is open (defensive)
- **Priority:** Medium | **Severity:** Critical | **Type:** Negative / Business rule
- **Preconditions:** Force a state with **no OPEN shift** (e.g. day was closed while a bill remained — normally
  blocked; construct via DB edit or by closing after emptying then reopening a stale bill screen).
- **Steps:** 1. On a bill's Payment screen while no shift is OPEN, tap Confirm Payment.
- **Expected Result:** `ShiftNotOpenException` → payment rejected; bill stays OPEN.
- **Edge Case Notes:** In normal use a day is always auto-open, so this is a defensive/edge path. Document how it
  was forced.
- **Automation Candidate:** No.

### TC-PAY-014 — Stock is deducted on payment for items with a recipe
- **Priority:** High | **Severity:** Major | **Type:** Functional / Stock
- **Preconditions:** `Nasi Goreng` has a recipe: 1 serving uses `Rice` 0.2 kg (StockItem `Rice` currentQty 10).
- **Steps:** 1. Create a bill, add `Nasi Goreng` ×3. 2. Pay in full. 3. More → Stock, check `Rice`.
- **Expected Result:** After payment, `Rice.currentQty = 10 − (0.2 × 3) = 9.4`. Items **without** a recipe do
  not change any stock.
- **Edge Case Notes:** `DeductStockForBillUseCase` runs after the payment transaction; deduction uses active
  (non-void) items × qty × qtyPerServing. See gap D-14 for opname interaction.
- **Automation Candidate:** Yes (with DB/stock inspection).

### TC-PAY-015 — Voided items are excluded from the amount charged
- **Priority:** High | **Severity:** Critical | **Type:** Money / Void interaction
- **Preconditions:** Bill: `Nasi Goreng` (15.000) + `Es Teh` (5.000) = 20.000.
- **Steps:** 1. Void `Es Teh` (reason Customer Changed Mind). 2. Verify Total `Rp 15.000`. 3. Pay Tunai tender `15000`.
- **Expected Result:** Payment amount `Rp 15.000` (voided line excluded); change 0; bill PAID. The Payment
  screen Order Summary lists only the active `Nasi Goreng`.
- **Edge Case Notes:** PaymentScreen filters `status != VOID`; grandTotal already recalculated on void.
- **Automation Candidate:** Yes.

### TC-PAY-016 — Success screen flashes then returns to Order list
- **Priority:** Low | **Severity:** Minor | **Type:** UI / Navigation
- **Preconditions:** Bill ready to pay.
- **Steps:** 1. Complete a payment.
- **Expected Result:** Brief `Payment Complete!` state, then automatic navigation to the Order tab
  (`popUpTo OrderRoute`, inclusive=false). The paid bill is absent from the list.
- **Edge Case Notes:** Because navigation is triggered by `isSuccess`, the success screen may be visible only
  momentarily. If the auto-nav feels abrupt, note as UX.
- **Automation Candidate:** No (timing).

### TC-PAY-017 — Back from Payment screen returns to the still-OPEN bill (no charge)
- **Priority:** High | **Severity:** Major | **Type:** Cancel-midway
- **Preconditions:** Bill open, on Payment screen, nothing confirmed.
- **Steps:** 1. Enter a tender but do **not** confirm. 2. Tap the top-bar Back arrow.
- **Expected Result:** Returns to Bill Detail; bill still **OPEN**; no Payment row; total unchanged. The bill can
  be edited/paid again later.
- **Automation Candidate:** Yes.

### TC-PAY-018 — Double-tap Confirm Payment does not double-charge
- **Priority:** High | **Severity:** Critical | **Type:** User behaviour / Data integrity
- **Preconditions:** Bill total `Rp 25.000`, ready to pay.
- **Steps:** 1. Select Tunai, tender `25000`. 2. Rapidly double-tap **Confirm Payment**.
- **Expected Result:** Exactly **one** Payment row is written; bill PAID once. The second tap either lands while
  `isLoading` (button shows a spinner and is `enabled=!isLoading`) or after the bill is already PAID
  (`BillAlreadyPaidException`). No double payment, no crash.
- **Edge Case Notes:** Verify the button truly disables during `isLoading`. If a second Payment row appears, log
  a Blocker (double-charge).
- **Automation Candidate:** Yes.

### TC-PAY-019 — Offline payment persists locally and syncs later
- **Priority:** High | **Severity:** Critical | **Type:** Offline / Data integrity
- **Preconditions:** E2 airplane mode; bill ready.
- **Steps:** 1. Complete a cash payment offline. 2. Confirm success + return to Order list. 3. Re-enable network. 4. Wait for sync.
- **Expected Result:** Bill immediately PAID locally (optimistic); Payment row `syncStatus=PENDING`. After
  reconnect, `/bills/{id}/status=PAID` and `/payments/{id}` appear in RTDB; local rows flip to SYNCED. No loss.
- **Automation Candidate:** No (network + console).

### TC-PAY-020 — Two devices attempt to pay the SAME open bill (no reopen / no double pay)
- **Priority:** High | **Severity:** Critical | **Type:** Sync / Conflict
- **Preconditions:** E3; one OPEN bill visible on both devices.
- **Steps:** 1. Device A pays the bill (PAID). 2. Before B syncs, B opens the same bill's Payment screen and taps Confirm.
- **Expected Result:** After sync, the bill is PAID exactly once. `ConflictResolver` rejects any inbound
  OPEN→(back to)OPEN regression, and `ProcessPaymentUseCase` on B fails with `BillAlreadyPaidException` once B
  sees PAID. If B committed a local PAID before syncing A's, the two PAID writes converge by LWW but **must not**
  produce two divergent totals or reopen the bill. Log any duplicate Payment or reopened bill as Blocker.
- **Edge Case Notes:** This is the core stale-device guard (FR-SYNC-7). During the pre-sync window B's UI may
  still show the bill OPEN (gap R-5) — note the UX.
- **Automation Candidate:** No (2 devices).
