# 09 — Expense Logging

**Backing code:** `feature/expense/ExpenseLogScreen.kt`, `ExpenseLogViewModel.kt`,
`domain/usecase/expense/SaveExpenseUseCase.kt`, `data/repository/ExpenseRepositoryImpl.kt`,
`core/common/ExpenseCategory.kt`, `feature/settings/ExpenseCategorySettingsScreen.kt`.

**Behaviour (verified):** More → Expense Log lists expenses for the **current open shift** (empty when no shift).
An **Add** action opens a sheet: **category** (enum picker), **amount** (digits only), **note** (optional).
**Save** requires `amount > 0` (blank/0 → silently no-ops and does not close the sheet). Expenses are stamped
with the current shift id, `createdBy=<username>`, and reduce expected cash at day close.

**Categories (verified enum, not editable):** `SUPPLIES, UTILITIES, SALARY, RENT, TRANSPORT, OTHER` — differs
from PRD's `GAS, ELECTRICITY, PACKAGING, WAGES, RENT, OTHER` (gap D-9). The "Expense Categories" settings screen
is a **read-only** bilingual list (Supplies/Perlengkapan, Utilities/Utilitas, Salary/Gaji, Rent/Sewa,
Transport/Transportasi, Other/Lainnya).

---

### TC-EXP-001 — Log an expense (happy path)
- **Priority:** High | **Severity:** Major | **Type:** Functional / CRUD
- **Preconditions:** Open day.
- **Test Data:** category SUPPLIES, amount `25000`, note `beras 5kg`.
- **Steps:** 1. More → Expense Log → Add. 2. Choose **Supplies**. 3. Amount `25000`. 4. Note `beras 5kg`. 5. Save.
- **Expected Result:** Sheet closes; the expense appears in the list (Supplies, Rp 25.000, note). Row persists
  with `shiftId=<open shift>`, `createdBy=<username>`, PENDING.
- **Postconditions:** Counts toward day-close expected-cash reduction.
- **Automation Candidate:** Yes.

### TC-EXP-002 — Amount field digits-only; 0/blank is rejected
- **Priority:** High | **Severity:** Major | **Type:** Validation / Boundary
- **Preconditions:** Add sheet open.
- **Steps:** 1. Type `1a2b` → observe `12`. 2. Clear amount and Save. 3. Type `0` and Save.
- **Expected Result:** Field keeps digits only. Saving with blank amount does nothing (`toLongOrNull() ?: return`).
  Saving `0` does nothing (`amountLong <= 0 → return`). The sheet stays open both times; no expense created.
- **Edge Case Notes:** No error message is shown on the 0/blank case → the Save button appears to "do nothing".
  Flag Minor UX (silent rejection).
- **Automation Candidate:** Yes.

### TC-EXP-003 — Optional note may be blank
- **Priority:** Medium | **Severity:** Minor | **Type:** Boundary
- **Steps:** 1. Add an expense with amount `5000`, no note. 2. Save.
- **Expected Result:** Saved with `note=null` (blank → null). Appears in the list.
- **Automation Candidate:** Yes.

### TC-EXP-004 — All six categories are selectable
- **Priority:** Low | **Severity:** Minor | **Type:** Functional
- **Steps:** 1. In the Add sheet, cycle the category picker.
- **Expected Result:** Options: Supplies, Utilities, Salary, Rent, Transport, Other (enum order). Each is
  selectable and persisted.
- **Automation Candidate:** Yes.

### TC-EXP-005 — Expenses reduce expected cash at day close
- **Priority:** High | **Severity:** Critical | **Type:** Cross-feature / Money
- **Preconditions:** Cash sales `Rp 50.000`; log an expense `Rp 15.000`.
- **Steps:** 1. Log the expense. 2. Close Day, counted `35000`.
- **Expected Result:** `expectedCash = 50000 − 15000 = 35000`, variance 0. Expense appears in the day's totals.
- **Automation Candidate:** Yes.

### TC-EXP-006 — Expense list is scoped to the current open shift
- **Priority:** Medium | **Severity:** Major | **Type:** State
- **Preconditions:** Log an expense today; then close the day and open a new one.
- **Steps:** 1. Log expense on Day A. 2. Close Day A, start Day B. 3. Open Expense Log.
- **Expected Result:** The list now shows **only** Day B expenses (empty until a new one is logged). Day A's
  expense is not shown here (it's in Day A's report). `observeExpensesForShift(currentShift)`.
- **Edge Case Notes:** If no shift is open, the list is empty and Save would stamp `shiftId=null` — verify a day
  is always open when logging.
- **Automation Candidate:** Yes.

### TC-EXP-007 — Cancel the Add sheet discards input
- **Priority:** Low | **Severity:** Minor | **Type:** Cancel-midway
- **Steps:** 1. Open Add, type amount + note. 2. Dismiss the sheet.
- **Expected Result:** No expense created; on reopening, fields are reset (`dismissSheet` resets category to
  SUPPLIES, amount and note to blank).
- **Automation Candidate:** Yes.

### TC-EXP-008 — Large amount handled without overflow
- **Priority:** Low | **Severity:** Minor | **Type:** Boundary
- **Steps:** 1. Log an expense of `1000000000` (1 billion). 2. Verify it appears and is included in day totals.
- **Expected Result:** Stored as `Long` 1,000,000,000; displayed as Rp 1.000.000.000; day-close math handles it.
- **Automation Candidate:** Yes.

### TC-EXP-009 — Expense categories screen is read-only (gap verification)
- **Priority:** Low | **Severity:** Minor | **Type:** Gap verification
- **Steps:** 1. More → Expense Categories.
- **Expected Result:** A read-only bilingual list of the 6 categories; **no add/edit/delete**. Diverges from
  FR-EXPENSE-2 ("editable in Settings") and uses different category names than the PRD (gap D-9).
- **Automation Candidate:** Yes.

### TC-EXP-010 — Expense syncs to RTDB
- **Priority:** Low | **Severity:** Minor | **Type:** Sync
- **Preconditions:** E1 online.
- **Steps:** 1. Log an expense. 2. Check RTDB `/expenses`.
- **Expected Result:** The expense appears with amount/category/shiftId; local row flips to SYNCED.
- **Automation Candidate:** No.

### TC-EXP-011 — Double-tap Save does not create duplicate expenses
- **Priority:** Medium | **Severity:** Major | **Type:** User behaviour
- **Steps:** 1. Fill a valid expense. 2. Double-tap Save.
- **Expected Result:** Exactly one expense row; the sheet closes after the first save (subsequent tap lands on a
  closed sheet). Verify only one row exists in the list/DB.
- **Automation Candidate:** Yes.
