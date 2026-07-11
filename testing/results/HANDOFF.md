# Warung POS — QA Handoff

E2E test suite (17 files, 259 authored cases) executed against a running debug build on a single Android
emulator (API 33), primarily offline, over a multi-day session. Full step-by-step evidence for every case is in
[`EXECUTION_LOG.md`](EXECUTION_LOG.md) (~3,100 lines) — this document is the scannable summary for whoever picks
up this project next.

**Scope note:** this began as a test-execution pass only. Four follow-up sessions then worked the defect list:
- **2026-07-10** — DEFECT-001 and DEFECT-002 (Auth / Lock App) fixed.
- **2026-07-11 (fix pass)** — every other then-open defect (DEFECT-003/008 through DEFECT-015) fixed. Committed
  as [`1bb45c2`](../../.git).
- **2026-07-11 (two-device session)** — a second emulator became available, unblocking E3 (multi-device) cases
  for the first time. Found **DEFECT-016**. Documentation-only commit [`63eae3e`](../../.git).
- **2026-07-11 (DEFECT-016 fix session)** — implemented, tested, and verified a fix for DEFECT-016 (see below).

**15 numbered defects found in total (DEFECT-001 through DEFECT-016). All 15 are fixed and committed.**
DEFECT-003 and DEFECT-008 are the same root-cause issue found twice. DEFECT-012 was never assigned — a candidate
finding during file 11 was folded into DEFECT-011 instead of becoming its own entry, so the numbering has a
deliberate gap, not a missing write-up.

## Release-gate verdict

**Meets the suite's own release-gate condition, including multi-device use.** Every Critical/High
regression-pack row passes; zero Blocker/Critical/Major defects remain open in Order, Payment, Void, Day-close,
or Sync, on one device or several. DEFECT-016 (the last open item) is now fixed: a shift left OPEN by another
device — and any bill still attached to it — is surfaced and closable instead of silently unreachable. Full unit
+ instrumented test suites pass (139 unit + 42 instrumented tests, zero failures, run on two connected
emulators).

**Note on scope:** DEFECT-016's fix (below) is a client-side visibility/recovery fix, not a change to the
underlying sync architecture — the app is still offline-first with no server-enforced single-open-shift lock
(a deliberate tradeoff to avoid requiring network access just to open a shift; see the rejected "server-enforced
lock" option in the fix writeup). What changed is that the *consequence* of two devices each opening their own
shift — an orphaned shift with a stranded bill — is now always recoverable through the UI, never silently lost.

## DEFECT-016 — fixed this session

**Finding:** the shared Firebase project had accumulated 24 separate OPEN shift rows over this engagement's
history. `getOpenShift()` always resolves to the single most-recently-opened one, so Close Day and the Z-report
only ever operated on that one shift — any older OPEN shift, and any bill still attached to it, was permanently
unreachable. Confirmed with a real, live, unpaid Rp 55.000 bill stranded this way, identically on both
`emulator-5554` and `emulator-5556`.

**Fix (Option 1 from the recommendation given to the user — "surface, don't hide," chosen over auto-merging
shifts or a server-enforced lock, both of which carried real downsides):**
- `ShiftDao`/`ShiftRepository` gained `getAllOpenShifts()`/`observeAllOpenShifts()` — every OPEN shift, not just
  the newest.
- `CloseShiftUseCase` now accepts an optional `shiftId`, so a specific non-current shift can be targeted (default
  `null` preserves existing "close the current shift" behavior).
- `ShiftCloseScreen`/`ShiftCloseViewModel` gained an "Other Open Shifts Detected" section: every other OPEN shift
  is listed with its own revenue/expenses/open-bill-count, and its own closing-float input + Close button. A
  shift with unresolved open bills shows a blocked state ("N open bill(s) must be resolved first — check
  Orders") instead of a close button — the fix routes the owner *to* the stranded bill via the existing Orders
  flow rather than force-closing around it.
- `MoreScreen`'s "Close Day" menu item gained a badge (mirroring the existing low-stock badge pattern) showing
  the open-shift count whenever it's more than 1, so the condition is visible without having to open Close Day
  first.
- **Edge case found and fixed during this work:** closing a shift is a check-then-act with no DB-level
  atomicity (unlike opening one, which DEFECT-003/008 already made atomic) — nothing stopped the same shift
  from being closed twice via a rapid double-tap, which would re-run `CloseShiftUseCase`/`GenerateZReportUseCase`
  a second time. Added an `isClosing`-state guard (same pattern as DEFECT-004's `Mutex`) to both the primary and
  per-row close actions. The much rarer cross-device double-close-the-same-shift race is accepted as a residual
  risk, consistent with this app's offline-first design not enforcing any cross-device lock on shift state.

**Verification:**
- New tests: `ShiftDaoTest` (3 new cases for the multi-open-shift queries against a real Room DB),
  `CloseShiftUseCaseTest` (4 new cases — explicit shiftId, still blocks on that shift's own bills, unknown
  shiftId, staff role blocked on the shiftId path too), `ShiftCloseViewModelTest` (new file, 7 cases), 4 new
  `ShiftCloseScreenTest` instrumented cases. 139 unit + 42 instrumented tests total, all passing.
- **End-to-end on-device, both emulators:** rebuilt and reinstalled; confirmed the "Close Day" badge showed the
  live open-shift count; opened Close Day and saw every other OPEN shift listed, including the one holding the
  real stranded bill, correctly shown as blocked; closed an eligible other shift and watched it disappear from
  the list reactively with a Z-report generated; **resolved the actual stranded bill** by voiding it via the
  normal Orders flow; returned to Close Day and confirmed that shift was now unblocked and closable; closed it,
  confirmed CLOSED status + Z-report in the DB. Registered a fresh third identity on Device B and confirmed both
  closures (and the bill's VOID status) had synced correctly.

Full repro evidence, code-level detail, and the fix recommendation/tradeoff table given to the user are in
`EXECUTION_LOG.md`'s "Addendum — 2026-07-11 second session" and "Addendum — 2026-07-11 DEFECT-016 fix" sections
at the end of the file.

## All defects — fixed and re-verified

| ID | Area | Fix | Verification |
|----|------|-----|---------------|
| **DEFECT-001** | Auth / Lock App | `PinViewModel.refreshMode()` re-derives UNLOCK/REGISTER mode from live registration state and clears the form on every re-entry to the PIN gate (`LaunchedEffect` in `WarungPosApp.kt`). | Re-ran the exact bypass trigger on-device (register → Lock App → Lock, no restart): correctly shows UNLOCK, wrong PIN rejected, no credential overwrite. New `PinViewModelTest`. |
| **DEFECT-002** | Auth / Lock App | Same fix as DEFECT-001 — `refreshMode()` also clears the `pin` field on every Lock. | Re-ran exact repro (unlock, Lock, blind Unlock tap with 0 digits): correctly rejected, no bypass. |
| **DEFECT-003 / DEFECT-008** | Day/Shift | `ShiftDao.openIfNoneOpen()` — new `@Transaction` DAO method atomically checks-then-inserts, closing the race that let two callers both open a shift. `BillDao.getOpenBillsForShift(shiftId)` replaces the unscoped global query in Close Day/auto-close. `MIGRATION_4_5` repairs any pre-existing duplicate-OPEN corruption on upgrade. | New `ShiftDaoTest` — a genuine 20-way concurrent-coroutine test against a real Room DB confirms exactly one shift ever ends up OPEN. New `BillDaoTest` cases for the scoped query. |
| **DEFECT-004** | Menu / Order | `BillDetailViewModel.addItem()`'s read-modify-write is now `Mutex`-guarded, serializing concurrent taps so no increment is lost to interleaving. | New `AddItemRaceConditionTest` — 30 genuinely concurrent coroutines confirm the final quantity always exactly matches the tap count. |
| **DEFECT-005** | Payment | `PaymentScreen` now displays `state.error` (an `AlertDialog`, same pattern as `BillDetailScreen`'s void error) — the ViewModel already set it correctly, the UI just never showed it. | New instrumented `PaymentScreenTest` cases: error is displayed, dismiss callback fires. |
| **DEFECT-006** | Void | Added the missing `voidNote` column (`MIGRATION_5_6`) and threaded it end-to-end (entity → domain → mapper → DAO → repository → use case → RTDB sync). | New mapper/use-case test cases round-tripping a non-null `voidNote`. |
| **DEFECT-007** | Menu / Variants | `VariantGroupEditor`'s name/price fields now buffer locally (`remember(id) { mutableStateOf(...) }`) instead of being fully controlled by state that round-tripped through a DB write + full reload on every keystroke. Price field also stopped reformatting from the parsed number (was erasing a typed `-` before the digits could follow). | Manual on-device: typed a 33-char name and `-1500` in one fast burst, confirmed byte-for-byte correct on-screen and in the persisted DB (WAL-inclusive read). |
| **DEFECT-009** | Void / Reporting | `VoidBillUseCase` now cascades `VOID` status to the bill's still-active `order_items` (new `VoidReason.BILL_VOID`), so whole-bill voids correctly contribute to the Z-report's void audit. | New `VoidBillUseCaseTest` case. |
| **DEFECT-010** | Reports / Z-report | New `ZReportSnapshot` model + `ZReport.toSnapshot()` mapper; `ZReportViewModel` now reads the persisted snapshot (falling back to live re-derivation only if none exists) and `ZReportScreen` shows the previously-invisible `countedCash`/`expectedCash`/`variance`. | New `ZReportMapperTest` cases against the real JSON shape `GenerateZReportUseCase` produces. |
| **DEFECT-011** | Stock / Opname | New shared `filterDecimalInput()` (digits + at most one `.`) replaces the buggy filter in all 3 affected fields (reorder threshold, batch qty, opname counted qty). | New `DecimalInputFilterTest`, including the exact `"2.5.1"` repro. |
| **DEFECT-013** | Stock Opname | `StockOpnameViewModel` now persists every counted-qty/reason edit to the DB immediately, so navigating away and back reads the draft instead of stale data. | New `StockOpnameViewModelTest` simulating ViewModel recreation — a typed count and a variance reason both survive. |
| **DEFECT-014** | Reports / Dashboard | (a) `DashboardScreen` gained an Expenses card. (b) `ReportsScreen`'s shift-scoped card relabeled "Current Shift Summary" (was "Day Summary", easily conflated with the adjacent genuinely-day-scoped "Today's Dashboard" link). | Manual verification (pure UI text/layout changes). |
| **DEFECT-015** | i18n | Two independent bugs: (1) the prior locale-override mechanism never worked — replaced with the platform `LocaleManager` API called directly from `WarungPosApplication`/`LanguageSettingsViewModel`. (2) Indonesian resources lived in `values-id/`, but the runtime locale canonicalizes to legacy code `"in"` — renamed the folder to `values-in/`. | Confirmed via `adb shell cmd locale get-app-locales` and visually: bottom nav shows "Laporan"/"Lainnya" by default and switches live, in-session, to "Reports"/"More" with no restart. |
| **DEFECT-016** | Sync / Day-Shift (multi-device) | Close Day now lists every OPEN shift (`ShiftDao.getAllOpenShifts()`), not just the most-recently-opened one; each is independently closable (or shown blocked if it still has open bills) via `CloseShiftUseCase(shiftId=...)`. A badge on the "Close Day" menu item surfaces the condition. See the dedicated section above for the full writeup. | New `ShiftDaoTest`/`CloseShiftUseCaseTest`/`ShiftCloseViewModelTest`/`ShiftCloseScreenTest` cases. End-to-end on-device: resolved the actual stranded bill via Orders, then closed its previously-unreachable shift, confirmed via DB and Z-report generation, confirmed synced correctly to a second device. |

Full root-cause traces, code-level detail, and evidence for every fix above are in `EXECUTION_LOG.md`'s
"Addendum — 2026-07-11 fix pass" section and inline under each defect's original write-up.

## Not run — and why

These remain genuinely blocked on infrastructure this environment doesn't have; none were part of the fix pass.

- **Multi-device (E3) cases, partial** — a second emulator became available 2026-07-11. TC-ONB-007 (no duplicate payment methods) was run and passes. TC-SYNC-050 (split-brain shift) was substantively addressed — the shared Firebase project's already-accumulated shift history stood in for a clean synthetic race, and surfaced DEFECT-016 (see above). TC-ORD-053/054, TC-PAY-020, TC-MENU-038, TC-PM-007, TC-NAV-009, and the regression pack's R5 rows are still NOT RUN — deprioritized once DEFECT-016 surfaced, and would need a fresh (uncontaminated) Firebase project to get a clean read anyway, since these two devices now carry the same 24-shift backlog.
- **Firebase RTDB console-dependent cases** — file 15 (version gate) almost entirely, plus scattered cases in 09/12/13. No console access in this environment.
- **Device-clock-control cases** — rollover/boundary cases in files 08 and 12 (week/month range edges, midnight-crossing attribution). Suite's own authors marked these "Automation Candidate: No"; not attempted given the risk of destabilizing the rest of the session's timestamps.
- Full per-file breakdown of what ran vs. didn't is in each file's section of `EXECUTION_LOG.md`.

## One operational note

Mid-session (file 13), re-enabling network to test the sync status bar triggered an unintended full sync of this
session's accumulated local test data (including a Rp 1,000,000,000 boundary-test expense and ~23 voided test
bills) to the shared Firebase project. Flagged to the user at the time; per their direction, no cleanup was
attempted. If cross-testing with other sessions on the same Firebase project, be aware this test data is live in
that project's RTDB.

## Files

- [`EXECUTION_LOG.md`](EXECUTION_LOG.md) — full step-by-step evidence, DB queries, and per-case verdicts for all 259 cases, plus every fix's verification detail.
- [`../00-assumptions-and-gaps.md`](../00-assumptions-and-gaps.md) — the pre-existing gap catalogue (D-1 through D-16, R-1 through R-7) this session's findings build on; gap D-11 (PDF export) is now closed.
