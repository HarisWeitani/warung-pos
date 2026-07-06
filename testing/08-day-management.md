# 08 — Day Management (Auto-open, Manual Close, Rollover Auto-close, Z-Report, History)

**Backing code:** `domain/usecase/shift/EnsureDayOpenUseCase.kt`, `CloseShiftUseCase.kt`,
`GenerateZReportUseCase.kt`, `feature/shift/ShiftCloseScreen.kt`, `ShiftCloseViewModel.kt`,
`feature/shift/ShiftHistoryScreen.kt`/`ViewModel`, `feature/shift/ZReportScreen.kt`/`ViewModel`,
`data/repository/ShiftRepositoryImpl.kt`, `data/local/dao/ReportQueryDao.kt`, `PaymentDao` (cash total),
`ExpenseDao` (`totalForShift`), `core/util/DateUtil.kt` (`startOfDay`).

**Model (verified):** "Day" == `Shift` entity. A Day **auto-opens** with `openingFloat=0` and no prompt when
none is open (`EnsureDayOpenUseCase`, called from `AppViewModel.init` and `OrderViewModel.createBill`).
- **Manual close** (More → **Close Day**, owner-only = always visible): shows day summary, a **Closing Cash
  Float** input, and a **Close Day** button. The button is **disabled** when `shift==null` or `openBills` is
  non-empty. Critically, the blocking open-bill list is loaded into state **only after** a close attempt fails
  (the VM does not pre-load open bills), so on first render the button may be enabled even if open bills exist —
  tapping it then surfaces the blocking card and disables the button.
- On successful close: shift `status=CLOSED`, `closedBy=<username>`, `closingFloat=countedCash`, and a **Z-report
  is generated**; the app navigates to the Z-report screen (`popUpTo ShiftClose inclusive`).
- **Auto-close on rollover:** when `EnsureDayOpenUseCase` runs and the open shift's `startOfDay(openedAt)` ≠
  today, and there are **no open bills**, it closes the old shift with `closedBy=null` (system),
  `countedCash=expectedCash`, `variance=0`, generates a Z-report, and opens a new day. If **open bills exist**,
  the old (stale-dated) day **stays open** until manually resolved — no auto-close.
- **Cash math:** `expectedCash = openingFloat(0) + Σ(cash payments in shift) − Σ(expenses in shift)`;
  `variance = countedCash − expectedCash`. Cash payments = payments whose method `isCash=1` on PAID bills of the
  shift. **All** expenses in the shift are subtracted (not only cash-tagged expenses — expenses have no cash flag).
- **Attribution:** revenue/tx/cash for the shift are keyed on `bills.shiftId` (set at bill **creation**), not
  `paidAt` (gap D-13).

Baseline: BL-3 (active trading day with PAID bills + expenses + voids), unless stated.

---

## Auto-open

### TC-DAY-001 — A day auto-opens on first launch (no prompt, zero float)
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional
- **Preconditions:** BL-0 fresh install, first-run PIN done.
- **Steps:** 1. Immediately inspect the open shift (DB or by creating a bill).
- **Expected Result:** Exactly one OPEN shift, `openingFloat=0`, no dialog was shown. See TC-ONB-004.
- **Automation Candidate:** Yes (DB).

### TC-DAY-002 — Only one day is OPEN at a time
- **Priority:** High | **Severity:** Critical | **Type:** Business rule
- **Preconditions:** An OPEN day.
- **Steps:** 1. Cold start the app several times. 2. Create bills. 3. Inspect `shifts` for `status=OPEN` rows.
- **Expected Result:** Never more than one OPEN shift on a single device (`EnsureDayOpenUseCase` opens a new day
  only when `getOpenShift()==null`).
- **Edge Case Notes:** Multi-device split-brain (both offline) is a known risk (R-1) — see TC-SYNC-050.
- **Automation Candidate:** Yes.

---

## Manual close

### TC-DAY-010 — Manual close with zero open bills generates a Z-report
- **Priority:** Critical | **Severity:** Blocker | **Type:** Functional / Money
- **Preconditions:** OPEN day; **no** open bills; 2 PAID cash bills totalling `Rp 40.000`; 1 expense `Rp 10.000`.
- **Test Data:** counted cash `30000`.
- **Steps:** 1. More → **Close Day**. 2. Read the Day Summary (Total Revenue Rp 40.000, Total Expenses Rp 10.000,
  Net Rp 30.000, Transactions 2). 3. Enter Closing Cash Float `30000`. 4. Tap **Close Day**.
- **Expected Result:** Shift closes (`status=CLOSED`, `closedBy=<username>`, `closingFloat=30000`). Navigates to
  the **Z-report** screen. Z-report snapshot: revenue 40000, expenses 10000, expectedCash = 0+40000−10000 =
  `30000`, countedCash 30000, **variance 0**, transaction count 2, plus void + payment breakdown. A **new** day
  is NOT auto-opened by manual close alone — the next `ensureDayOpen` (app start / new bill) opens it.
- **Postconditions:** Closed shift + immutable Z-report. New day opens on next `ensureDayOpen`.
- **Edge Case Notes:** Verify whether a new day is open afterward — `CloseShiftUseCase` does not call
  `openNewDay`; the Order tab's `createBill` (and app restart) will. Until then there is **no open day**, so
  payments would be blocked (TC-PAY-013). Confirm the app opens a new day promptly (Order VM observes shift; a
  new bill triggers it). Log if the app is left with no open day and ordering is silently blocked.
- **Automation Candidate:** Yes (DB + report inspection).

### TC-DAY-011 — Cash variance computed correctly (shortage)
- **Priority:** Critical | **Severity:** Critical | **Type:** Money / Boundary
- **Preconditions:** Cash sales `Rp 50.000`, expenses `Rp 5.000` → expected `Rp 45.000`.
- **Test Data:** counted cash `40000`.
- **Steps:** 1. Close Day. 2. Enter `40000`. 3. Close.
- **Expected Result:** Z-report `expectedCash=45000`, `countedCash=40000`, `variance=-5000` (a Rp 5.000
  shortage). Negative variance is preserved (not clamped).
- **Automation Candidate:** Yes.

### TC-DAY-012 — Cash variance computed correctly (overage)
- **Priority:** High | **Severity:** Major | **Type:** Money / Boundary
- **Preconditions:** Expected cash `Rp 45.000`.
- **Test Data:** counted cash `50000`.
- **Expected Result:** `variance=+5000`.
- **Automation Candidate:** Yes.

### TC-DAY-013 — Blank counted cash defaults to 0 → variance = −expected
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary
- **Preconditions:** Expected cash `Rp 30.000`.
- **Steps:** 1. Close Day. 2. Leave the float field blank. 3. Close.
- **Expected Result:** `countedCash=0` (`closingFloat.toLongOrNull() ?: 0`), `variance=-30000`. Confirm whether
  the UI should require an explicit count; a blank close producing a large negative variance is a real risk —
  flag Medium if no confirmation.
- **Automation Candidate:** Yes.

### TC-DAY-014 — Non-cash payments are excluded from expected cash
- **Priority:** High | **Severity:** Critical | **Type:** Money
- **Preconditions:** Day with `Rp 30.000` cash (Tunai) + `Rp 100.000` QRIS sales; no expenses.
- **Steps:** 1. Close Day. 2. Enter counted `30000`. 3. Close.
- **Expected Result:** `expectedCash = 30000` (QRIS excluded — only `isCash=1` methods counted). Total Revenue
  in the summary is `Rp 130.000` (all methods), but expected **cash** is `Rp 30.000`, variance 0.
- **Edge Case Notes:** Confirms cash reconciliation ignores digital methods.
- **Automation Candidate:** Yes.

### TC-DAY-015 — Expenses reduce expected cash
- **Priority:** High | **Severity:** Major | **Type:** Money
- **Preconditions:** Cash sales `Rp 60.000`; expenses `Rp 20.000`.
- **Steps:** 1. Close, counted `40000`.
- **Expected Result:** `expectedCash = 0 + 60000 − 20000 = 40000`, variance 0.
- **Edge Case Notes:** **All** shift expenses are subtracted from cash regardless of how they were "paid"
  (there's no cash-vs-noncash expense flag). If a real expense was paid by transfer, this over-subtracts cash —
  document as a modelling limitation.
- **Automation Candidate:** Yes.

### TC-DAY-020 — Close blocked by open bills (blocking list appears after attempt)
- **Priority:** Critical | **Severity:** Critical | **Type:** Business rule / Negative
- **Preconditions:** OPEN day with **2 open bills** (Rp 15.000 and Rp 8.000).
- **Steps:** 1. More → Close Day. 2. Observe the button initially. 3. Tap **Close Day**.
- **Expected Result:** After the tap, a red card appears: `Cannot close: 2 open bill(s)` listing
  `• Counter - HH:mm — Rp 15.000` and `• Counter - HH:mm — Rp 8.000`. The shift stays OPEN; the **Close Day**
  button is now **disabled** (openBills non-empty in state). No Z-report generated.
- **Edge Case Notes:** The blocking list is populated by `OpenBillsBlockShiftCloseException` on the failed
  attempt (not pre-loaded), so step 2 may show the button enabled even though bills exist. Confirm the day is
  **not** closed at any point.
- **Automation Candidate:** Yes.

### TC-DAY-021 — Resolve blocking bills then close succeeds
- **Priority:** High | **Severity:** Critical | **Type:** Recovery
- **Preconditions:** Continue from TC-DAY-020.
- **Steps:** 1. Go to Order, pay one blocking bill, void the other. 2. Return to Close Day. 3. Enter counted cash. 4. Close.
- **Expected Result:** With zero open bills the close succeeds and a Z-report is generated including the newly
  paid bill's revenue.
- **Edge Case Notes:** Because the VM only refreshes `openBills` on a close attempt, verify the button re-enables
  after returning (the VM re-observes shift, but `openBills` state may still hold the stale list until the next
  failed attempt). If the button stays disabled with zero open bills, log a Major UX defect and note the
  workaround (re-enter the screen).
- **Automation Candidate:** No (multi-step, timing).

### TC-DAY-022 — Close Day button disabled while submitting (no double close)
- **Priority:** Medium | **Severity:** Major | **Type:** User behaviour
- **Preconditions:** Ready to close.
- **Steps:** 1. Double-tap **Close Day**.
- **Expected Result:** One close; button shows spinner and is `enabled=!isLoading`; exactly one Z-report; no
  duplicate closed shift.
- **Automation Candidate:** Yes.

### TC-DAY-023 — Back out of Close Day without closing
- **Priority:** Low | **Severity:** Minor | **Type:** Cancel-midway
- **Steps:** 1. Enter a float. 2. Tap Back.
- **Expected Result:** Day stays OPEN; nothing persisted; returns to More.
- **Automation Candidate:** Yes.

---

## Rollover auto-close

### TC-DAY-030 — Calendar rollover auto-closes the previous day (no open bills)
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / Time
- **Preconditions:** E4 clock control. OPEN day dated "yesterday" with some PAID bills and no open bills. App
  closed.
- **Steps:** 1. Advance the device clock to the next calendar day. 2. Cold-start the app (triggers
  `EnsureDayOpenUseCase`).
- **Expected Result:** The previous day is **auto-closed**: `status=CLOSED`, `closedBy=null` (system),
  `closingFloat=expectedCash`, a Z-report with `countedCash=expectedCash`, `variance=0`. A **new** day opens for
  today (zero float). No counted-cash prompt is shown.
- **Edge Case Notes:** `sameDay` compares `startOfDay(openedAt)` vs `startOfDay(now)` using `DateUtil` timezone
  — verify the day boundary matches the store's timezone (WIB). A mismatch could roll over at the wrong hour.
- **Automation Candidate:** No (clock control).

### TC-DAY-031 — Rollover blocked by open bills keeps the old day open
- **Priority:** High | **Severity:** Critical | **Type:** Business rule / Time
- **Preconditions:** OPEN day dated "yesterday" with **≥1 open bill**.
- **Steps:** 1. Advance clock to next day. 2. Cold-start.
- **Expected Result:** The old (yesterday-dated) day **stays OPEN** (no auto-close, no new day). The operator
  must resolve the open bills and manually close it. No duplicate/second open day is created.
- **Edge Case Notes:** `EnsureDayOpenUseCase` returns early when `getOpenBills().isNotEmpty()`. Confirm a new bill
  created "today" attaches to the still-open yesterday shift (attribution oddity) — document.
- **Automation Candidate:** No.

### TC-DAY-032 — Rollover while app is open but idle does NOT auto-close (no timer)
- **Priority:** Medium | **Severity:** Major | **Type:** Time / Gap
- **Preconditions:** App open on Order tab just before local midnight, no open bills.
- **Steps:** 1. Leave the app foregrounded and idle. 2. Let the clock cross midnight. 3. Wait 5 minutes without interacting.
- **Expected Result (current):** The day does **not** auto-close while idle (no background timer). It only rolls
  over on the next `ensureDayOpen` trigger — creating a bill or cold-starting. Document gap D-16.
- **Steps (cont.):** 4. Tap `+` to create a bill.
- **Expected Result:** At step 4, `createBill`→`ensureDayOpen` fires the rollover (auto-close yesterday, open
  today), then creates the bill under **today's** day.
- **Automation Candidate:** No.

### TC-DAY-033 — Auto-close does not double-generate a Z-report
- **Priority:** High | **Severity:** Critical | **Type:** Data integrity / Time
- **Preconditions:** A day just auto-closed by rollover.
- **Steps:** 1. Cold-start the app again the same day.
- **Expected Result:** Exactly **one** Z-report exists for the auto-closed shift; the new open day is not
  re-closed. `getOpenShift()` returns today's open day so no rollover repeats.
- **Automation Candidate:** Yes (DB).

---

## Z-report & history

### TC-DAY-040 — Z-report contents match the day's activity
- **Priority:** High | **Severity:** Critical | **Type:** Reporting
- **Preconditions:** A closed day with known revenue, expenses, voids, payment mix.
- **Steps:** 1. Open the Z-report (right after close, or via Day History).
- **Expected Result:** The report shows revenue, expenses, transaction count, void count/value, and payment
  breakdown by method matching the day's data. (Snapshot JSON persists these; `openingFloat` is always 0.)
- **Edge Case Notes:** Payment breakdown lists `methodId` values — verify the screen maps IDs to names or shows
  raw IDs (Minor if raw).
- **Automation Candidate:** Yes.

### TC-DAY-041 — Z-report is immutable (no reopen)
- **Priority:** High | **Severity:** Critical | **Type:** Data integrity
- **Preconditions:** A closed day.
- **Steps:** 1. From Day History open the closed day's Z-report. 2. Look for any edit/reopen control.
- **Expected Result:** No reopen/edit control. A closed shift cannot be reopened; the Z-report is read-only
  (FR-DAY-6).
- **Automation Candidate:** Yes.

### TC-DAY-042 — Day History lists closed days newest-first and opens each Z-report
- **Priority:** Medium | **Severity:** Major | **Type:** Functional / Sorting
- **Preconditions:** ≥2 closed days.
- **Steps:** 1. More → **Day History**. 2. Tap a row.
- **Expected Result:** Closed days listed (verify order per `ShiftHistoryViewModel`); tapping opens the matching
  Z-report (`ZReportRoute(shiftId)`).
- **Automation Candidate:** Yes.

### TC-DAY-043 — Z-report backup written to RTDB on close (online)
- **Priority:** Medium | **Severity:** Major | **Type:** Sync / Data integrity
- **Preconditions:** E1 online.
- **Steps:** 1. Close a day. 2. Check RTDB console for the Z-report path (per `ShiftRepositoryImpl.closeShift` /
  `FirebaseRtdbDataSource`).
- **Expected Result:** A one-time Z-report write appears in RTDB (arch decision Q5). If offline at close, verify
  it is not lost — note the R-2 risk (Z-report has no sync-retry metadata; if the device dies offline before
  connectivity, the Z-report may never reach RTDB).
- **Automation Candidate:** No (console).

### TC-DAY-044 — Revenue attribution mismatch across midnight (gap D-13)
- **Priority:** Medium | **Severity:** Major | **Type:** Data integrity / Edge
- **Preconditions:** A bill **created** just before midnight under Day 1 but **paid** just after midnight.
- **Steps:** 1. Create a bill at 23:59 (Day 1 open, no rollover yet since no ensureDayOpen ran). 2. Cross midnight. 3. Cold-start (Day 1 auto-closes if no other open bills — but this bill is open, so Day 1 stays open). 4. Pay the bill. 5. Close Day 1 manually.
- **Expected Result:** Because `shiftId` is fixed at creation, this bill's revenue is attributed to **Day 1**
  (its creation day) in the Z-report — even though it was paid after midnight. The Dashboard "today" (paidAt-based)
  would place it on Day 2. Document the discrepancy; confirm no double counting and no lost revenue.
- **Edge Case Notes:** This is the concrete failure of FR-DAY-7. Capture both the Z-report and Dashboard numbers.
- **Automation Candidate:** No.
