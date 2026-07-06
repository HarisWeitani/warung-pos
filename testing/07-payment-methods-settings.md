# 07 — Payment Method Settings

**Backing code:** `feature/settings/PaymentMethodSettingsScreen.kt`, `PaymentMethodSettingsViewModel.kt`
(`observeAllPaymentMethods`, `toggleActive`), `data/repository/PaymentRepositoryImpl.kt`,
`data/local/dao/PaymentMethodDao.kt`, seeded in `FirstRunManager.kt`.

**Behaviour (verified):** More → Payment Methods lists **all** methods (active + inactive) with a toggle. The
only editable attribute is **active/inactive** (`toggleActive`). **Rename and reorder are NOT implemented**
(gap D-8). Only **active** methods appear on the Payment screen. Seeded defaults: Tunai(cash), QRIS, GoPay, OVO,
Transfer Bank.

---

### TC-PM-001 — All five seeded methods listed
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** BL-0 seeded.
- **Steps:** 1. More → Payment Methods.
- **Expected Result:** Tunai, QRIS, GoPay, OVO, Transfer Bank listed in sort order, all active.
- **Automation Candidate:** Yes.

### TC-PM-002 — Disable a method removes it from the Payment screen
- **Priority:** High | **Severity:** Major | **Type:** Functional / Config
- **Preconditions:** BL-2, open bill.
- **Steps:** 1. Payment Methods → disable **GoPay**. 2. Open a bill and tap Pay.
- **Expected Result:** GoPay absent from the Payment method radio list; still shown (as inactive) in settings.
- **Postconditions:** `pm_gopay.isActive=0`, PENDING.
- **Automation Candidate:** Yes.

### TC-PM-003 — Re-enable a method restores it on the Payment screen
- **Priority:** Medium | **Severity:** Minor | **Type:** Functional
- **Preconditions:** GoPay disabled.
- **Steps:** 1. Re-enable GoPay. 2. Open Pay.
- **Expected Result:** GoPay reappears in the method list.
- **Automation Candidate:** Yes.

### TC-PM-004 — Disable ALL methods → payment cannot complete
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary / Negative
- **Preconditions:** BL-2.
- **Steps:** 1. Disable all five methods. 2. Open a bill, tap Pay.
- **Expected Result:** Empty method list; **Confirm Payment disabled** (selectedMethodId null). See TC-PAY-008.
- **Automation Candidate:** Yes.

### TC-PM-005 — Toggle state persists across restart
- **Priority:** Medium | **Severity:** Major | **Type:** Persistence
- **Preconditions:** OVO disabled.
- **Steps:** 1. Force-stop + relaunch. 2. Payment Methods.
- **Expected Result:** OVO still disabled (Room-persisted).
- **Automation Candidate:** Yes.

### TC-PM-006 — No rename / reorder controls (gap verification)
- **Priority:** Low | **Severity:** Minor | **Type:** Gap verification
- **Preconditions:** Payment Methods screen.
- **Steps:** 1. Inspect each row and the screen for rename/drag controls.
- **Expected Result:** Only an active/inactive toggle exists. No rename field, no drag handle, no reorder. This
  diverges from FR-PAYMENT-4 (rename + reorder) — log Minor gap.
- **Automation Candidate:** No.

### TC-PM-007 — Toggle syncs across devices
- **Priority:** Low | **Severity:** Minor | **Type:** Sync
- **Preconditions:** E3 two devices.
- **Steps:** 1. Disable QRIS on A. 2. Wait for sync. 3. Check B's Payment screen.
- **Expected Result:** QRIS becomes unavailable on B too (LWW on the payment method row).
- **Automation Candidate:** No.

### TC-PM-008 — Rapid toggling doesn't corrupt state
- **Priority:** Low | **Severity:** Minor | **Type:** User behaviour
- **Preconditions:** Payment Methods.
- **Steps:** 1. Toggle Tunai on/off 10× quickly.
- **Expected Result:** Final visible state matches the last tap; the value persisted equals the displayed state;
  no crash. (If it can be paid with Tunai disabled, that's a defect.)
- **Automation Candidate:** Yes.
