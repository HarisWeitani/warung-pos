# 17 — Regression Pack (Release Gate Smoke + Critical Path)

Run this pack on **every release-candidate build** before shipping an APK. It is a curated subset of the full
suite that exercises the money-critical and data-integrity paths end-to-end, plus the highest-value negative and
recovery cases. Each row references the authoritative case in its feature file.

**Pass condition for release:** every **Critical**/**High** row Pass; zero Blocker/Critical defects open in
Order, Payment, Void, Day-close, or Sync.

---

## R1 — Core happy path (single device, online)

| # | Ref | Check | Expected |
|---|-----|-------|----------|
| R1-1 | TC-AUTH-001 | First-run register PIN | Reaches Order tab |
| R1-2 | TC-ONB-001 | 5 payment methods seeded | Tunai/QRIS/GoPay/OVO/Transfer, no dupes |
| R1-3 | TC-ONB-004 | Day auto-opens | One OPEN shift, float 0 |
| R1-4 | TC-MENU-001 | Create a menu item | Item appears in picker |
| R1-5 | TC-ORD-002/010 | Create bill, add item | Line + total correct |
| R1-6 | TC-ORD-016 | Add variant item with deltas | Unit price = base + deltas |
| R1-7 | TC-PAY-001 | Cash exact payment | PAID, change 0, bill leaves list |
| R1-8 | TC-PAY-002 | Cash overpay | Correct positive change |
| R1-9 | TC-EXP-001 | Log an expense | Appears in list |
| R1-10 | TC-DAY-010 | Close day, zero open bills | Z-report, correct variance |
| R1-11 | TC-RPT-001 | Dashboard today | Revenue/tx/expenses match |

## R2 — Money & void integrity

| # | Ref | Check | Expected |
|---|-----|-------|----------|
| R2-1 | TC-PAY-003 | Underpay blocked | Rejected, bill stays OPEN |
| R2-2 | TC-PAY-018 | Double-tap Confirm | Exactly one payment, no double charge |
| R2-3 | TC-VOID-001 | Void item excludes from total | Total drops correctly |
| R2-4 | TC-VOID-002 | Void "Other" needs note | Button disabled until note |
| R2-5 | TC-PAY-015 | Pay after voiding an item | Charges only active items |
| R2-6 | TC-ORD-058 | Void all items | Total 0, Pay disabled |
| R2-7 | TC-DAY-011 | Cash variance shortage | variance = counted − expected (negative kept) |
| R2-8 | TC-DAY-014 | Non-cash excluded from expected cash | Only cash counted |
| R2-9 | TC-DAY-020 | Open bills block close | Blocking list, day stays OPEN |
| R2-10 | TC-RPT-023 | Reports exclude open bills | Only PAID counted |

## R3 — Sold-out / availability (known gaps)

| # | Ref | Check | Expected (current) |
|---|-----|-------|--------------------|
| R3-1 | TC-ORD-030 | Sold-out still orderable | Orderable (gap D-1 — log defect) |
| R3-2 | TC-ORD-031 | Hidden item removed from picker | Absent from picker |
| R3-3 | TC-MENU-033 | Sold-out not auto-reset on new day | Remains sold-out (gap D-5) |

## R4 — Offline & recovery

| # | Ref | Check | Expected |
|---|-----|-------|----------|
| R4-1 | TC-SYNC-001 | Full POS offline | All ops succeed, rows PENDING |
| R4-2 | TC-SYNC-010 | Flush on reconnect | All rows reach RTDB, SYNCED |
| R4-3 | TC-PAY-019 | Offline payment then sync | PAID locally, synced later |
| R4-4 | TC-NAV-023 | Force-close mid-order | Added items survive |
| R4-5 | TC-AUTH-030 | Process death → locked | PIN required on relaunch |
| R4-6 | TC-SYNC-032 | Force-close during pending sync | No loss, no dup |

## R5 — Multi-device (when 2 devices available)

| # | Ref | Check | Expected |
|---|-----|-------|----------|
| R5-1 | TC-SYNC-020 | Bill A → visible on B | Propagates within seconds |
| R5-2 | TC-SYNC-022 | Concurrent item adds | Both survive |
| R5-3 | TC-SYNC-024 | Stale device can't reopen PAID | Stays PAID |
| R5-4 | TC-ONB-007 | Second device no payment-method dupes | Still 5 methods |

## R6 — Guardrails & lifecycle

| # | Ref | Check | Expected |
|---|-----|-------|----------|
| R6-1 | TC-AUTH-011 | Wrong PIN | Error, field cleared, still locked |
| R6-2 | TC-AUTH-052 | Back on PIN screen | No bypass |
| R6-3 | TC-VER-002 | Below min version | Non-dismissable update screen |
| R6-4 | TC-VER-003 | Offline version check | Allowed (fail-open) |
| R6-5 | TC-DAY-033 | Auto-close no double Z-report | Exactly one report |
| R6-6 | TC-NAV-021 | Process death restores screen data | Items intact after unlock |

## R7 — Crash-safety sweep (NFR-RELIABILITY: zero crashes in order/payment)

Perform a fast, adversarial pass and confirm **no crash / ANR** in any step:
- Double/rapid taps on: FAB (new bill), menu items, Pay, Confirm Payment, Void Item, Close Day, Create PIN.
- Back button spammed on every screen (PIN gate, Bill Detail, Payment, variant sheet, dialogs).
- Cancel every dialog/sheet mid-way (variant sheet, void dialog, void-bill dialog, lock dialog, expense sheet).
- Enter boundary inputs: empty amounts, huge tender (`Long.MAX` range), non-digit paste into PIN/tender/price.
- Toggle airplane mode repeatedly during an active order and during a sync.

**Expected:** No crash, no ANR, no data loss or duplication in any of the above (Blocker if any crash occurs in
an Order or Payment path).
