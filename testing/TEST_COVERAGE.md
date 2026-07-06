# TEST COVERAGE & Requirement Traceability Matrix — Warung POS

Companion to [`TEST_PLAN.md`](TEST_PLAN.md) and [`TEST_STRATEGY.md`](TEST_STRATEGY.md). Maps every documented
requirement (PRD FR-*/NFR-*) and every confirmed implementation gap to Test Case IDs, so no requirement is left
unverified — including requirements that were **not implemented** (those map to a case that verifies their
**absence**).

Legend for **Status**:
- **Implemented** — behaviour present; tests assert it works.
- **Deviates** — present but differs from the PRD; tests assert actual behaviour + flag the gap.
- **Not implemented** — absent; tests verify the absence (documented gap).

---

## 1. Feature file index

| File | Area | Case ID prefix | # cases |
|------|------|----------------|---------|
| `00-assumptions-and-gaps.md` | Assumptions, PRD-vs-code gaps | — | — |
| `01-authentication-pin.md` | Local PIN auth | TC-AUTH | 25 |
| `02-onboarding-firstrun.md` | First-run seeding | TC-ONB | 8 |
| `03-order-and-bills.md` | Order & bill lifecycle | TC-ORD | 31 |
| `04-payment.md` | Payment & change | TC-PAY | 20 |
| `05-void.md` | Void item/bill | TC-VOID | 15 |
| `06-menu-management.md` | Menu/variants/recipes | TC-MENU | 22 |
| `07-payment-methods-settings.md` | Payment methods | TC-PM | 8 |
| `08-day-management.md` | Day open/close/rollover/Z-report | TC-DAY | 21 |
| `09-expenses.md` | Expenses | TC-EXP | 11 |
| `10-stock.md` | Stock items/batches | TC-STK | 12 |
| `11-stock-opname.md` | Stock opname | TC-OPN | 12 |
| `12-reports-dashboard.md` | Reports/dashboard/export | TC-RPT | 18 |
| `13-sync-multidevice.md` | Sync/offline/multi-device | TC-SYNC | 22 |
| `14-settings-language-i18n.md` | Settings/i18n | TC-SET | 10 |
| `15-version-gate.md` | Version gate | TC-VER | 7 |
| `16-navigation-state-lifecycle.md` | Nav/lifecycle/recovery | TC-NAV | 17 |
| `17-regression.md` | Release-gate regression pack | R-tables | — |
| **Total** | | | **259** |

---

## 2. Requirement Traceability Matrix (PRD → tests)

### FR-AUTH — Authentication & Roles
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-AUTH-1/2/5 (Firebase email/pw, roles, owner-only gates) | **Not implemented** (replaced by local PIN; role always OWNER) | TC-AUTH-090, TC-AUTH-091 |
| FR-AUTH-3 (auth persists offline) | Deviates → PIN persists locally; sync uses anon auth | TC-AUTH-030, TC-ONB-005, TC-SYNC-051 |
| FR-AUTH-4 (new-device first login needs internet) | Deviates → only sync needs internet; PIN is local | TC-SYNC-030, TC-SYNC-051 |
| Local PIN model (shipped) | Implemented | TC-AUTH-001..052, TC-AUTH-092 |

### FR-I18N — Internationalisation
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-I18N-1/3/3a (fully bilingual, all strings externalised) | **Deviates** (most text hardcoded English) | TC-SET-003, TC-SET-010 |
| FR-I18N-2 (language selectable, per-device, not synced) | Implemented | TC-SET-001, TC-SET-004, TC-SET-005 |
| FR-I18N-4 (default Indonesian, fallback English) | Implemented | TC-SET-002, TC-SET-010 |
| FR-I18N-5 (Rp, no decimals) | Implemented | TC-SET-006 |
| NFR-I18N (switch without restart) | Implemented (limited scope) | TC-SET-002 |

### FR-ORDER — Order Taking
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-ORDER-1 (order = launch destination) | Implemented | TC-ORD-001, TC-NAV-001 |
| FR-ORDER-2 (grid grouped by category, chip filter) | Deviates (list + chips in Bill Detail) | TC-ORD-014, TC-ORD-032 |
| FR-ORDER-3 (sold-out greyed & non-tappable) | **Not implemented** | TC-ORD-030 |
| FR-ORDER-4 (tap adds/variant sheet, required enforced) | Implemented | TC-ORD-010, TC-ORD-015, TC-ORD-016 |
| FR-ORDER-5/6 (cart panel, +/- quantity, − removes) | **Deviates** (no cart, no +/-, only void) | TC-ORD-011, TC-ORD-018, TC-VOID-007 |
| FR-ORDER-7/8 (confirm → new bill → payment, no destination) | Deviates (bill-first flow) | TC-ORD-002, TC-ORD-005 |
| FR-ORDER-9 (writes to Room immediately, optimistic) | Implemented | TC-ORD-010, TC-NAV-023 |

### FR-BILL — Bill Management
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-BILL-1 (all UPFRONT) | Implemented | TC-ORD-002 |
| FR-BILL-2 (OPEN→PAID/VOID, forward-only) | Implemented | TC-ORD-050, TC-ORD-052, TC-SYNC-024 |
| FR-BILL-4 (labels "Bill #N") | **Deviates** ("Counter - HH:mm") | TC-ORD-002 (note) |
| FR-BILL-5 (append items to OPEN bill) | Implemented | TC-ORD-010..013, TC-ORD-053 |
| FR-BILL-6 (name/price snapshots) | Implemented | TC-ORD-041, TC-ORD-042, TC-MENU-004 |
| FR-BILL-7 (12h open-bill warning) | **Not implemented** | TC-ORD (gap D-7, see 00 §D) |
| FR-BILL-8 (daily bills; leftover blocks next day) | Implemented | TC-DAY-020, TC-DAY-031 |

### FR-PAYMENT — Payment
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-PAYMENT-1 (method selector, tender, change) | Implemented | TC-PAY-001, TC-PAY-007 |
| FR-PAYMENT-2 (change = tender − total; block deficit) | Implemented | TC-PAY-002, TC-PAY-003 |
| FR-PAYMENT-3 (split payment) | **Not implemented** | TC-PAY (gap D-2, see 00 §D) |
| FR-PAYMENT-4 (methods toggle/rename/reorder) | **Deviates** (toggle only) | TC-PM-002, TC-PM-006 |
| FR-PAYMENT-5 (money as Long) | Implemented | TC-PAY-011 |
| FR-PAYMENT-6 (QRIS label only, no MDR) | Implemented | TC-PAY-005 |

### FR-VOID — Void & Cancel
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-VOID-1 (void item, reason required, OTHER note) | Implemented | TC-VOID-001, TC-VOID-002, TC-VOID-003 |
| FR-VOID-2 (voided items excluded, not deleted) | Implemented | TC-VOID-001, TC-VOID-026, TC-PAY-015 |
| FR-VOID-3 (void entire paid or open bill, owner) | **Deviates** (OPEN only; role always owner) | TC-VOID-020, TC-VOID-023 |
| FR-VOID-4 (Z-report void breakdown) | Implemented (verify whole-bill count) | TC-VOID-025, TC-DAY-040 |

### FR-MENU — Menu Management
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-MENU-1 (category CRUD, reorder, soft-delete) | **Not implemented** (no category-creation UI; items land Uncategorized) | TC-MENU-030 |
| FR-MENU-2 (item fields, availability, sold-out) | Implemented | TC-MENU-001, TC-MENU-031 |
| FR-MENU-3 (sold-out manual, no auto-reset) | Implemented | TC-MENU-033 |
| FR-MENU-4/5 (variant groups/options, deltas incl. negative) | Implemented (verify negative) | TC-MENU-010, TC-MENU-011 |
| FR-MENU-6 (price/name edit doesn't alter history) | Implemented | TC-MENU-004, TC-MENU-005 |
| FR-MENU-7 (soft-delete/hide, blocked if in bills) | Deviates (hide warns but allows; can't unhide) | TC-MENU-034, TC-MENU-035, TC-MENU-036 |

### FR-STOCK — Stock (Phase 2, shipped)
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-STOCK-1/2 (stock items, batches) | Implemented | TC-STK-001, TC-STK-010 |
| FR-STOCK-3/4 (recipe link, deduct on payment) | Implemented | TC-MENU-020, TC-PAY-014, TC-STK-013 |
| FR-STOCK-5 (low-stock indicator + More badge) | Implemented | TC-STK-020, TC-STK-021 |
| FR-STOCK-6 (no-recipe items don't deduct) | Implemented | TC-STK-013, TC-PAY-014 |
| FR-SYNC-6 (atomic stock increment) | Implemented (verify) | TC-STK-030 |

### FR-OPNAME — Stock Opname (Phase 2, shipped)
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-OPNAME-1/6 (session start, one at a time) | Implemented | TC-OPN-001, TC-OPN-002 |
| FR-OPNAME-2 (count entry, live variance) | Implemented | TC-OPN-003, TC-OPN-004 |
| FR-OPNAME-3 (non-zero variance needs reason) | Implemented (different reason enum) | TC-OPN-005 |
| FR-OPNAME-4 (pre-submit cost summary) | **Investigate/likely gap** | TC-OPN-011 |
| FR-OPNAME-5 (submit sets qty, immutable) | Implemented | TC-OPN-006 |
| FR-OPNAME-7 (defer deductions during opname) | **Not implemented** | TC-OPN-010 |

### FR-EXPENSE — Expenses
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-EXPENSE-1 (log category/amount/date/note) | Implemented | TC-EXP-001, TC-EXP-003 |
| FR-EXPENSE-2 (default categories, editable) | **Deviates** (different names; read-only) | TC-EXP-004, TC-EXP-009 |
| FR-EXPENSE-3 (expenses in reports) | Implemented | TC-EXP-005, TC-RPT-016 |

### FR-DAY — Day Management
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-DAY-1 (auto-open, zero float, no prompt) | Implemented | TC-DAY-001, TC-ONB-004 |
| FR-DAY-1a (reset sold-out prompt on new day) | **Not implemented** | TC-MENU-033 |
| FR-DAY-2 (one open day) | Implemented (single device); risk multi-device | TC-DAY-002, TC-SYNC-050 |
| FR-DAY-3 (manual close owner-only, block on open bills) | Implemented (role moot) | TC-DAY-010, TC-DAY-020 |
| FR-DAY-4 (counted cash, expected, variance) | Implemented | TC-DAY-011, TC-DAY-012, TC-DAY-014, TC-DAY-015 |
| FR-DAY-4a (auto-close on rollover, no count step) | Implemented (trigger-based, no timer) | TC-DAY-030, TC-DAY-031, TC-DAY-032 |
| FR-DAY-5 (immutable Z-report contents) | Implemented | TC-DAY-040, TC-DAY-043 |
| FR-DAY-6 (closed days immutable, no reopen) | Implemented | TC-DAY-041 |
| FR-DAY-7 (bill attributed to Day it was PAID) | **Deviates** (attributed to creation day) | TC-DAY-044, TC-RPT (paidAt vs shiftId) |

### FR-REPORTS — Reports
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-REPORTS-1 (daily dashboard) | Implemented | TC-RPT-001..005 |
| FR-REPORTS-2 (date-range report) | Implemented | TC-RPT-010..016 |
| FR-REPORTS-3 (per-item COGS margin) | **Not implemented**; "gross profit" mislabeled | TC-RPT-015 |
| FR-REPORTS-4 (best-seller ranking) | Implemented | TC-RPT-003, TC-RPT-017 |
| FR-REPORTS-5 (CSV + PDF export, share sheet) | **Deviates** (CSV only, no PDF) | TC-RPT-020, TC-RPT-021 |

### FR-SYNC — Offline-First Sync
| Req | Status | Test cases |
|-----|--------|-----------|
| FR-SYNC-1 (Room SoT) | Implemented | TC-SYNC-001, all UI reads |
| FR-SYNC-2 (optimistic writes + sync metadata) | Implemented | TC-SYNC-010, TC-PAY-019 |
| FR-SYNC-3 (WorkManager push, RTDB pull) | Implemented | TC-SYNC-010, TC-SYNC-020 |
| FR-SYNC-4 (persistent status indicator) | Deviates (no pending count) | TC-SYNC-002, TC-SYNC-003 |
| FR-SYNC-5 (field-level, append-only items) | Implemented | TC-SYNC-022, TC-ORD-053 |
| FR-SYNC-6 (atomic stock increment) | Implemented (verify) | TC-STK-030 |
| FR-SYNC-7 (bill status guarded) | Implemented | TC-SYNC-024, TC-PAY-020 |
| FR-SYNC-8 (LWW by updatedAt) | Implemented | TC-SYNC-023 |
| FR-SYNC-9 (reconnect flush with progress) | Deviates (no "N records" text) | TC-SYNC-010, TC-SYNC-040 |

### Non-Functional
| Req | Status | Test cases |
|-----|--------|-----------|
| NFR-PERF (responsiveness, no main-thread block) | Perf-scenario identified | TC-NAV-027, TC-RPT-024, TC-ORD-012 |
| NFR-OFFLINE (100% core ops offline) | Implemented | TC-SYNC-001, TC-ONB-005, TC-SYNC-013 |
| NFR-DATA (Long money, immutable finals, soft-delete, UUID PKs) | Implemented | TC-PAY-011, TC-DAY-041, TC-VOID-002 |
| NFR-SECURITY (RTDB rules, encrypted prefs) | Implemented (no per-role paths) | TC-SYNC-052, TC-AUTH-092 |
| NFR-RELIABILITY (zero crashes order/payment; retry backoff) | Verified by | R7 sweep, TC-SYNC-012 |
| NFR-DISTRIBUTION (version gate) | Implemented | TC-VER-001..007 |
| NFR-COST (free tier) | Out of test scope (monitor console) | — |

---

## 3. Gap → verification mapping (from `00-assumptions-and-gaps.md`)

| Gap | Description | Verifying case(s) |
|-----|-------------|-------------------|
| B | No Firebase login / no roles (local PIN, always OWNER) | TC-AUTH-090, TC-AUTH-091 |
| C | Bill-first order flow; `ConfirmOrderUseCase` dead | TC-ORD-002, TC-ORD-011 |
| D-1 | Sold-out items orderable | TC-ORD-030 |
| D-2 | No split payment | TC-PAY-001 (single row), 00 §D |
| D-3 | PAID bill not voidable | TC-VOID-023 |
| D-4 | Hardcoded English strings | TC-SET-003 |
| D-5 | No sold-out reset prompt | TC-MENU-033 |
| D-6 | Labels "Counter - HH:mm" | TC-ORD-002 |
| D-7 | No 12h open-bill warning | (assert absence) |
| D-8 | Payment methods toggle-only | TC-PM-006 |
| D-9 | Different expense categories, read-only | TC-EXP-004, TC-EXP-009 |
| D-10 | Different opname variance reasons | TC-OPN-005 |
| D-11 | CSV only, no PDF | TC-RPT-020 |
| D-12 | "Gross profit" = revenue − expenses | TC-RPT-015 |
| D-13 | Revenue attributed to creation day, not paid day | TC-DAY-044 |
| D-14 | Opname overwrites concurrent deductions | TC-OPN-010 |
| D-15 | Stock/opname/reports shipped despite "out of MVP" | 10/11/12 suites |
| D-16 | Rollover only on trigger, no timer | TC-DAY-032 |
| D-17 | Version gate fails open | TC-VER-003, TC-VER-005 |
| D-18 | No in-app category creation; items Uncategorized | TC-MENU-030 |
| D-19 | Stock `lastCostPrice` not updated on batch receive | TC-STK-010 |
| D-20 | Menu item blank-name / price≤0 rejected | TC-MENU-002, TC-MENU-003 |
| F-1 | SyncWorker markSynced race → possible loss | TC-SYNC-011 |
| F-2 | Inbound reflection null-out | TC-SYNC-025 |
| R-1 | Multi-device split-brain open day | TC-SYNC-050 |
| R-2 | Z-report not sync-retried (loss risk) | TC-DAY-043, TC-SYNC-031 |

---

## 4. Coverage by test-type (self-review checklist)

| Dimension (from brief) | Covered by |
|------------------------|-----------|
| Happy path | R1, TC-ORD/PAY/DAY happy cases |
| Alternate flows | variant sheet, non-cash, custom range |
| Negative | TC-PAY-003/006, TC-VOID-023, TC-OPN-005, TC-AUTH-002..005 |
| Boundary values | TC-AUTH-004/007, TC-PAY-011, TC-DAY-013, TC-STK-020, TC-RPT-011/012 |
| Input validation | TC-AUTH-006, TC-PAY-010, TC-EXP-002, TC-OPN-004, TC-MENU-002 |
| Business-rule validation | Day close blocking, void reasons, single opname, forward-only status |
| CRUD | Menu items/variants/ingredients, stock, expenses, payment-method toggle |
| Search/Filter/Sort/Pagination | category chips (filter), best-seller sort, open-bills sort; **pagination N/A** (small datasets, no Paging) |
| Navigation | file 16 (TC-NAV-*) |
| Error handling | void error dialog, payment error path, sync retry, version gate fail-open |
| Fast/double tap | TC-AUTH-050/051, TC-ORD-012/057, TC-PAY-018, TC-EXP-011, TC-DAY-022, TC-NAV-008, R7 |
| Back / cancel-midway / partial completion | TC-ORD-019/020/055, TC-VOID-004/021, TC-PAY-017, TC-AUTH-021, R7 |
| Long idle / background→foreground | TC-AUTH-031, TC-NAV-020/024/025 |
| Session timeout / lock | TC-AUTH-020/030/031 (no timeout by design) |
| Force close / app restart / device reboot | TC-AUTH-030, TC-NAV-021/023, TC-SYNC-032/033 |
| Slow/Offline/Reconnect/Timeout/Retry | file 13, TC-VER-005 |
| Partial/Server error/Invalid/Empty response | TC-SYNC-012/025, TC-VER-004/005 |
| Login/Logout/Session/Token/Invalid creds/Multiple attempts | file 01 (adapted to PIN); FR-AUTH absence TC-AUTH-090/091 |
| Multi-device: new phone/reinstall/same account/logout one/replace/restore | TC-SYNC-030/031, TC-AUTH-041, TC-ONB-007, TC-SYNC-050 |
| Data integrity after logout-lock/update/migration/interruption/crash/retry | TC-NAV-021/023, TC-SYNC-031/032/033, TC-DAY-033 |
| State: loading/empty/error/success/disabled/refresh/cache | TC-NAV-026, per-screen empty states |
| UI: validation msgs, button enable/disable, keyboard, transitions, orientation, deep links | file 01/03/04/16; **deep links N/A** (no deep links defined) |
| Performance scenarios (identify only) | TC-ORD-012, TC-RPT-024, TC-SYNC-040, TC-NAV-027 |

---

## 5. Self-review — residual gaps in the test suite itself

Items intentionally light or pending environment/asset confirmation (call out when executing):
- **App-update/DB migration** (schema v1→v3 upgrade path): needs an older-versioned APK to install-over; not
  fully scripted here — add a case when a prior signed APK is available. Covered conceptually by NFR-DISTRIBUTION.
- **Exact currency grouping string**: assert numerically until `CurrencyFormatter` output is confirmed (A8).
- **Whole-bill void vs Z-report void count** (TC-VOID-025): verify whether a whole-bill void increments the
  void breakdown or only per-item voids do.
- **Opname pause persistence** (TC-OPN-008/009): behaviour of uncommitted counts across VM re-creation must be
  observed on-device.
- **Orientation**: PRD says portrait-only; confirm the manifest lock. If unlocked, add rotation cases to file 16.
- **Category creation mechanism** (TC-MENU-030): exploratory — determine and then formalise category CRUD cases.
