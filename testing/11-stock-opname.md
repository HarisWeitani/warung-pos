# 11 — Stock Opname (Physical Count / Variance Reconciliation)

**Backing code:** `feature/stock/StockOpnameScreen.kt`, `StockOpnameViewModel.kt`,
`domain/usecase/stock/StartStockOpnameUseCase.kt`, `SubmitStockOpnameUseCase.kt`,
`data/repository/StockRepositoryImpl.kt`, `core/common/VarianceReason.kt`, `core/common/OpnameStatus.kt`.

**Behaviour (verified):** More → **Stock Opname**. **Start** snapshots every stock item's current qty as each
line's `systemQty` and creates an `IN_PROGRESS` opname (only one at a time; second start →
`OpnameAlreadyInProgressException`). Each line shows name, unit, system qty, an editable **counted qty** (digits
+ one `.`), and a **reason** picker. `variance = counted − system`. On **Submit**: every line with a **non-zero
variance requires a reason** (else `MissingVarianceReasonException(name)`); then each item's `currentQuantity` is
**set to the counted value**, lines saved, and the opname marked COMPLETED (immutable).

**Reasons (verified enum):** `COUNT_ERROR, DAMAGE, THEFT, EXPIRY, OTHER` (differs from PRD `SPOILAGE, LOSS,
THEFT, COUNTING_ERROR, OTHER` — gap D-10).

**Deferred-deduction (FR-OPNAME-7): NOT implemented** — sales during an open opname deduct immediately and are
then **overwritten** by the counted value on submit (gap D-14).

Baseline: BL-2 + several stock items with known quantities.

---

### TC-OPN-001 — Start an opname snapshots current quantities
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** Stock items `Rice`=10, `Oil`=4, `Sugar`=2.
- **Steps:** 1. More → Stock Opname → **Start**.
- **Expected Result:** A line per stock item appears with `systemQty` = current (Rice 10, Oil 4, Sugar 2) and
  `counted` prefilled to the system qty (variance 0). An `IN_PROGRESS` opname exists.
- **Automation Candidate:** Yes.

### TC-OPN-002 — Only one opname can be in progress
- **Priority:** High | **Severity:** Major | **Type:** Business rule / Negative
- **Preconditions:** An opname already IN_PROGRESS.
- **Steps:** 1. Leave the opname (Back). 2. Reopen Stock Opname and attempt **Start** again.
- **Expected Result:** The screen resumes the existing in-progress session (loads its lines) rather than
  starting a new one; a second explicit Start yields `OpnameAlreadyInProgressException` (error surfaced). No two
  IN_PROGRESS sessions.
- **Edge Case Notes:** The VM observes `observeInProgressOpname()` and loads it on entry.
- **Automation Candidate:** Yes.

### TC-OPN-003 — Enter counted quantities; variance computes live
- **Priority:** High | **Severity:** Major | **Type:** Functional / Boundary
- **Preconditions:** In-progress opname (Rice 10, Oil 4, Sugar 2).
- **Steps:** 1. Set Rice counted `9`. 2. Set Oil counted `4`. 3. Set Sugar counted `2.5`.
- **Expected Result:** Variance shows Rice `-1`, Oil `0`, Sugar `+0.5` live (`counted − system`).
- **Automation Candidate:** Yes.

### TC-OPN-004 — Counted-qty field filters to digits and a single decimal
- **Priority:** Medium | **Severity:** Minor | **Type:** Input validation
- **Steps:** 1. Type `9a.5b` into a counted field.
- **Expected Result:** Field keeps `9.5` (filter allows digit or `.`). Note: the filter allows **multiple** dots
  (e.g. `9.5.1`) since it only checks `isDigit() || == '.'`; `toDoubleOrNull()` of `9.5.1` is null → the line
  falls back to `systemQty` on submit. Test `9.5.1` and confirm the submit treats it as no-change (variance 0)
  rather than crashing; if it silently discards the entry, log Minor.
- **Automation Candidate:** Yes.

### TC-OPN-005 — Submit blocked when a non-zero variance line has no reason
- **Priority:** Critical | **Severity:** Critical | **Type:** Validation / Negative
- **Preconditions:** Rice counted `9` (variance −1), **no reason** chosen; others zero-variance.
- **Steps:** 1. Tap **Submit**.
- **Expected Result:** Submit **rejected** with an error naming the item (`MissingVarianceReasonException("Rice")`
  → error text). Nothing is committed; the session stays IN_PROGRESS; quantities unchanged.
- **Automation Candidate:** Yes.

### TC-OPN-006 — Submit succeeds; counted values become the new current quantities
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / Data integrity
- **Preconditions:** Rice counted `9` reason DAMAGE; Oil `4` (no reason needed, variance 0); Sugar `2.5` reason COUNT_ERROR.
- **Steps:** 1. Set reasons for the non-zero lines. 2. Tap **Submit**.
- **Expected Result:** Opname → COMPLETED (immutable). `Rice.currentQuantity=9`, `Oil=4`, `Sugar=2.5`. Lines
  persist their variance + reason. The screen resets to no in-progress session.
- **Postconditions:** Stock reflects the physical count.
- **Automation Candidate:** Yes.

### TC-OPN-007 — Zero-variance lines need no reason
- **Priority:** Medium | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** All counted == system.
- **Steps:** 1. Submit without setting any reason.
- **Expected Result:** Succeeds; no reason required; quantities unchanged (set to same values).
- **Automation Candidate:** Yes.

### TC-OPN-008 — Pause/resume: leaving and returning keeps counts
- **Priority:** Medium | **Severity:** Major | **Type:** State / Recovery
- **Preconditions:** In-progress opname with some counted values entered.
- **Steps:** 1. Enter counts for 2 lines (do not submit). 2. Navigate away (Back to More). 3. Reopen Stock Opname.
- **Expected Result:** The session resumes. **Caveat:** counted values are held in ViewModel state and lines are
  reloaded from persisted `countedQty`. Verify whether uncommitted counts survive the round-trip — if the VM
  reloads from DB and the DB only has the initial snapshot, in-progress edits may be **lost**. Record the actual
  behaviour; if edits are lost on navigation, log a Major defect (opname "pause" loses work).
- **Edge Case Notes:** `loadLines` only runs when `inProgress.id` changes; returning to the same session should
  keep VM state if the VM survived, but a fresh VM reloads from DB.
- **Automation Candidate:** No (lifecycle-sensitive).

### TC-OPN-009 — Process death mid-opname does not lose the session
- **Priority:** Medium | **Severity:** Major | **Type:** Recovery
- **Preconditions:** In-progress opname.
- **Steps:** 1. Background the app, `am kill`. 2. Relaunch, unlock, reopen Stock Opname.
- **Expected Result:** The IN_PROGRESS opname is still present (persisted); lines reload from `systemQty`
  snapshot. Uncommitted counted values entered before the kill may be gone (same caveat as TC-OPN-008).
- **Automation Candidate:** No.

### TC-OPN-010 — Sales during an open opname are overwritten on submit (gap D-14)
- **Priority:** Medium | **Severity:** Major | **Type:** Data integrity / Gap
- **Preconditions:** `Rice`=10; `Nasi Goreng` recipe 0.2 Rice; opname started (Rice systemQty snapshot 10).
- **Steps:** 1. Start the opname (do not submit). 2. In another flow, sell `Nasi Goreng` ×5 (deducts 1.0 →
  `Rice.currentQuantity` becomes 9 immediately). 3. Return to the opname, set Rice counted `10` reason
  COUNT_ERROR (the counter physically saw 10 because they haven't used the rice yet, say). 4. Submit.
- **Expected Result (current):** On submit, `Rice.currentQuantity` is **set to 10**, **discarding** the −1.0
  deduction from the concurrent sale. The sale's stock impact is lost. This violates FR-OPNAME-7 (deferred
  replay). Document precisely; the *shipped* behaviour is "counted value wins, concurrent deductions lost".
- **Edge Case Notes:** Real risk of overstating inventory if sales happen mid-count. High-value gap to raise.
- **Automation Candidate:** No (multi-flow).

### TC-OPN-011 — Pre-submit shows a variance COUNT but no cost-impact summary (gap)
- **Priority:** Low | **Severity:** Minor | **Type:** Reporting / Gap verification
- **Preconditions:** In-progress opname with some variances.
- **Steps:** 1. Read the header/summary area on the opname screen.
- **Expected Result:** The screen shows a variance **count** like `N of M items have a variance` and a per-line
  signed variance (`+0.5 kg`), but it does **not** show the FR-OPNAME-4 **cost impact** (variance × lastCostPrice)
  or **total stock value**. Log a Minor gap (cost-impact summary missing). Per-line variance and reason entry
  work.
- **Automation Candidate:** Yes.

### TC-OPN-012 — Empty stock → opname has no lines
- **Priority:** Low | **Severity:** Trivial | **Type:** Empty state
- **Preconditions:** No stock items.
- **Steps:** 1. Start an opname.
- **Expected Result:** A session with **zero** lines; Submit completes it immediately with no changes. No crash.
- **Automation Candidate:** Yes.
