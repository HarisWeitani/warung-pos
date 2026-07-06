# 12 — Reports, Dashboard & CSV Export

**Backing code:** `feature/reports/ReportsScreen.kt`/`ReportsViewModel.kt`,
`feature/reports/DashboardScreen.kt`/`DashboardViewModel.kt`, `feature/reports/ReportScreen.kt`/`ReportViewModel.kt`,
`feature/reports/BestSellerScreen.kt`, `domain/usecase/report/GetDashboardDataUseCase.kt`,
`GetReportDataUseCase.kt`, `ExportReportUseCase.kt`, `data/local/dao/ReportQueryDao.kt`,
`data/repository/ReportRepositoryImpl.kt`, `PaymentDao` (breakdown), `core/util/DateUtil.kt`.

**Behaviour (verified):**
- **Reports tab** (owner-only = always visible) → Dashboard. **Dashboard** shows **today's** (paidAt-based,
  `todayRangeWib`) total revenue, transaction count, total expenses, payment breakdown, and top-5 best sellers.
- **Full Report** (`FullReportRoute`) supports modes **Day / Week / Month / Custom**:
  - Day = start..end of today; Week = last **7** days (today − 6d .. today); Month = last **30** days
    (today − 29d .. today); Custom = a chosen start..end (inclusive day bounds).
  - Shows revenue, expenses, **grossProfit = revenue − expenses** (NOT COGS-based, gap D-12), payment mix, void
    stats, expenses-by-category, best sellers.
- **Best Sellers** sub-screen reuses the report's data (shared ViewModel scoped to `FullReportRoute`).
- **Export**: CSV only (`ExportReportUseCase` writes a `.csv` to cache and shares via `FileProvider`/Android
  share sheet). No PDF (gap D-11).
- All range revenue/void/breakdown use **paidAt** (date-range), while the Z-report/day-close use **shiftId**
  (gap D-13).

Baseline: BL-3 with known data; use E4 clock control for range boundaries.

---

## Dashboard

### TC-RPT-001 — Dashboard shows today's revenue, tx count, expenses
- **Priority:** High | **Severity:** Major | **Type:** Reporting
- **Preconditions:** Today: 3 PAID bills totalling `Rp 90.000`; 1 expense `Rp 10.000`.
- **Steps:** 1. Tap the **Reports** tab → Dashboard.
- **Expected Result:** Total revenue `Rp 90.000`, transactions `3`, expenses `Rp 10.000` (paidAt in today's WIB
  range). Values match `GetDashboardDataUseCase`.
- **Automation Candidate:** Yes.

### TC-RPT-002 — Dashboard payment breakdown by method
- **Priority:** Medium | **Severity:** Major | **Type:** Reporting
- **Preconditions:** Today: `Rp 30.000` Tunai + `Rp 60.000` QRIS.
- **Steps:** 1. Open Dashboard. 2. Read the payment breakdown.
- **Expected Result:** Breakdown lists Tunai `Rp 30.000` and QRIS `Rp 60.000` (sum by `paymentMethodId` in
  today's range). Verify method names vs raw IDs.
- **Automation Candidate:** Yes.

### TC-RPT-003 — Dashboard top-5 best sellers by quantity
- **Priority:** Medium | **Severity:** Minor | **Type:** Sorting
- **Preconditions:** Today: sold Nasi Goreng ×5, Es Teh ×3, Ayam ×1 (all PAID).
- **Steps:** 1. Open Dashboard best-sellers.
- **Expected Result:** Ranked by total quantity DESC: Nasi Goreng (5), Es Teh (3), Ayam (1); only PAID,
  non-void order items counted; capped at 5.
- **Edge Case Notes:** Voided items excluded (`status != VOID`). Ties: order is by qty then DB order.
- **Automation Candidate:** Yes.

### TC-RPT-004 — Dashboard excludes non-today, voided, and open-bill data
- **Priority:** High | **Severity:** Major | **Type:** Data integrity
- **Preconditions:** Yesterday: a PAID bill; today: an OPEN (unpaid) bill and a voided item.
- **Steps:** 1. Open Dashboard.
- **Expected Result:** Yesterday's revenue is excluded (paidAt not in today), the OPEN bill contributes nothing
  (not PAID), and the voided item is excluded from best-sellers. Only today's PAID activity is counted.
- **Automation Candidate:** Yes.

### TC-RPT-005 — Empty day dashboard shows zeros
- **Priority:** Low | **Severity:** Minor | **Type:** Empty state
- **Preconditions:** No paid bills today.
- **Steps:** 1. Open Dashboard.
- **Expected Result:** Revenue `Rp 0`, transactions `0`, expenses `Rp 0`, empty breakdown/best-sellers. No crash.
- **Automation Candidate:** Yes.

---

## Full report & date ranges

### TC-RPT-010 — Day mode equals today's figures
- **Priority:** Medium | **Severity:** Major | **Type:** Reporting
- **Steps:** 1. Dashboard → Full Report. 2. Select **Day**.
- **Expected Result:** Revenue/expenses/etc. match today's paidAt range (same window as Dashboard revenue).
- **Automation Candidate:** Yes.

### TC-RPT-011 — Week mode covers the last 7 days inclusive
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary / Time
- **Preconditions:** E4. Paid bills on: today, 6 days ago (in range), 7 days ago (out of range).
- **Steps:** 1. Full Report → **Week**.
- **Expected Result:** Includes today and the bill 6 days ago; **excludes** the bill 7 days ago
  (`startOfDay(now − 6d) .. endOfDay(now)`).
- **Edge Case Notes:** Confirms the inclusive 7-day window boundary.
- **Automation Candidate:** No (clock).

### TC-RPT-012 — Month mode covers the last 30 days inclusive
- **Priority:** Low | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** E4. Bills at 29 days ago (in) and 30 days ago (out).
- **Steps:** 1. Full Report → **Month**.
- **Expected Result:** Includes the 29-days-ago bill, excludes the 30-days-ago bill.
- **Automation Candidate:** No.

### TC-RPT-013 — Custom range (inclusive day bounds)
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary / Filter
- **Preconditions:** Bills spread over a month.
- **Steps:** 1. Full Report → **Custom**. 2. Pick start and end dates spanning a known subset.
- **Expected Result:** Report totals equal the sum of PAID bills with `paidAt` within
  `startOfDay(start)..endOfDay(end)` inclusive. Verify a bill paid at 23:59 on the end date is included.
- **Automation Candidate:** No.

### TC-RPT-014 — Custom range start after end (invalid range)
- **Priority:** Low | **Severity:** Minor | **Type:** Negative
- **Steps:** 1. Custom mode, choose start = today, end = yesterday (if the picker allows).
- **Expected Result:** Verify the picker prevents start > end, or the report returns empty/zero without crashing.
  Record actual behaviour; if it produces a nonsensical negative range, log Minor.
- **Automation Candidate:** No.

### TC-RPT-015 — "Gross Profit" is revenue − expenses, not COGS-based (gap D-12)
- **Priority:** Medium | **Severity:** Major | **Type:** Gap verification / Reporting
- **Preconditions:** Revenue `Rp 100.000`, expenses `Rp 30.000`, and recipe items sold (so a COGS exists).
- **Steps:** 1. Full Report → Day. 2. Read the gross-profit figure.
- **Expected Result:** Gross profit shows `Rp 70.000` (= revenue − **expenses**), ignoring ingredient COGS. This
  mislabels net-of-expenses as "gross profit" and there is **no per-item margin/COGS report** (FR-REPORTS-3).
  Log Major gap.
- **Automation Candidate:** Yes.

### TC-RPT-016 — Expenses-by-category breakdown
- **Priority:** Low | **Severity:** Minor | **Type:** Reporting
- **Preconditions:** Expenses: Supplies 20.000, Utilities 10.000.
- **Steps:** 1. Full Report over the range containing them.
- **Expected Result:** Category breakdown lists SUPPLIES 20.000, UTILITIES 10.000 (grouped by enum).
- **Automation Candidate:** Yes.

### TC-RPT-017 — Best Sellers sub-screen shares the report's range
- **Priority:** Low | **Severity:** Minor | **Type:** Navigation / State
- **Steps:** 1. Full Report → set Week. 2. Open **Best Sellers**.
- **Expected Result:** The best-seller list reflects the **Week** data (shared ViewModel scoped to
  `FullReportRoute`, up to 20 items). Changing the mode and reopening updates it.
- **Edge Case Notes:** `BestSellerRoute` uses `hiltViewModel(parentEntry=FullReportRoute)` — verify the shared
  instance; if it re-creates a fresh VM, the range resets to Day (log Minor).
- **Automation Candidate:** No.

---

## Export

### TC-RPT-020 — Export produces a CSV and opens the Android share sheet
- **Priority:** Medium | **Severity:** Major | **Type:** Functional / Export
- **Preconditions:** A report with data.
- **Steps:** 1. Full Report → **Share/Export**. 2. Observe the share sheet.
- **Expected Result:** A `.csv` file (`warungpos_report_<ts>.csv`) is generated in cache and offered via the
  system share sheet (WhatsApp/Drive/email/etc.). No PDF option (gap D-11).
- **Automation Candidate:** No (share sheet is system UI).

### TC-RPT-021 — CSV content matches on-screen figures
- **Priority:** Medium | **Severity:** Major | **Type:** Data integrity
- **Preconditions:** Known report data.
- **Steps:** 1. Export to a location you can open (e.g. Drive). 2. Open the CSV.
- **Expected Result:** CSV contains rows: `Revenue`, `Expenses`, `Gross Profit`, a `Payment Method,Total`
  section, `Expense Category,Total`, `Void Count`/`Void Value`, and `Item,Qty,Revenue` best-sellers, matching
  the screen. Item names containing commas/quotes are CSV-escaped.
- **Edge Case Notes:** Payment breakdown rows use `paymentMethodId` (raw id), not the display name — verify and
  log Minor if IDs are unfriendly in the export.
- **Automation Candidate:** No.

### TC-RPT-022 — Export an empty report (no crash)
- **Priority:** Low | **Severity:** Minor | **Type:** Boundary
- **Preconditions:** A range with no data.
- **Steps:** 1. Export.
- **Expected Result:** A CSV with zeroed totals and empty sections is produced and shared; no crash.
- **Automation Candidate:** No.

### TC-RPT-023 — Reports reflect only PAID bills (open bills excluded)
- **Priority:** High | **Severity:** Critical | **Type:** Data integrity
- **Preconditions:** An OPEN bill worth `Rp 50.000` and a PAID bill worth `Rp 20.000` in range.
- **Steps:** 1. Full Report → Day.
- **Expected Result:** Revenue = `Rp 20.000` only; the open bill is excluded (`status='PAID'` filter). Voided
  bills also excluded.
- **Automation Candidate:** Yes.

### TC-RPT-024 — Large dataset performance-scenario (identify, not benchmark)
- **Priority:** Low | **Severity:** Minor | **Type:** Performance-scenario
- **Preconditions:** ~12 months of data (hundreds–thousands of paid bills) seeded.
- **Steps:** 1. Open Full Report → Month/Custom over the full range.
- **Expected Result:** The report loads without ANR/crash. (NFR-PERF target: <3s for 12 months — measure
  separately.) This case only *identifies* the scenario for a perf pass.
- **Automation Candidate:** No.
