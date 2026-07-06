# 02 — Onboarding & First-Run Seeding

**Backing code:** `data/seeding/FirstRunManager.kt`, `AppViewModel.kt` (init: `ensureSeeded()` then
`ensureDayOpenUseCase()`, then anonymous sign-in + version gate + `syncCoordinator.start()`),
`domain/usecase/shift/EnsureDayOpenUseCase.kt`.

**Behaviour (verified):** On first app start, `FirstRunManager.ensureSeeded()` seeds **5 payment methods with
fixed IDs** exactly once (guarded by `warung_first_run/seeded_v1`). Then a **Day auto-opens** (zero float, no
prompt). No menu items/categories are seeded — the menu starts empty. Anonymous Firebase sign-in and sync start
in the background.

Seeded payment methods (id, name, isCash, sortOrder):
`pm_tunai`/Tunai/cash/1, `pm_qris`/QRIS/2, `pm_gopay`/GoPay/3, `pm_ovo`/OVO/4, `pm_transfer`/Transfer Bank/5.

---

### TC-ONB-001 — Fresh install seeds exactly five payment methods, once
- **Feature / User Story:** As an operator, payment methods are ready to use on day one.
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / Data
- **Preconditions / Baseline:** BL-0 fresh install (Clear storage).
- **Test Data:** none.
- **Steps:**
  1. Launch and complete first-run PIN registration.
  2. Go to **More → Payment Methods**.
- **Expected Result:** Exactly **5** methods listed: **Tunai, QRIS, GoPay, OVO, Transfer Bank**, all enabled/
  active, in that sort order. No duplicates.
- **Postconditions:** `payment_methods` table has 5 rows with the fixed IDs above; `seeded_v1=true`.
- **Edge Case Notes:** Fixed IDs + `OnConflictStrategy.IGNORE`-style insert make seeding idempotent across
  reinstalls and multi-device (gap R-8 mitigation). Verify no "Tunai ×2".
- **Automation Candidate:** Yes.

### TC-ONB-002 — Seeding does not repeat on subsequent launches
- **Priority:** High | **Severity:** Major | **Type:** Data / Idempotency
- **Preconditions:** App already launched once (seeded).
- **Steps:** 1. In Payment Methods, disable **OVO**. 2. Cold start the app 3 times. 3. Reopen Payment Methods.
- **Expected Result:** Still exactly 5 methods; **OVO remains disabled** (seeding did not re-run and did not
  re-enable it). `ensureSeeded()` returns early because `seeded_v1` is true.
- **Edge Case Notes:** Confirms seeding never overwrites user edits.
- **Automation Candidate:** Yes.

### TC-ONB-003 — Menu starts empty on fresh install
- **Priority:** High | **Severity:** Major | **Type:** Empty state
- **Preconditions:** BL-0 fresh install, first-run done.
- **Steps:** 1. Go to **More → Menu Management**.
- **Expected Result:** No categories and no items (empty list / empty-state). Ordering is possible only after
  the owner adds items (see `06-menu-management.md`).
- **Edge Case Notes:** Nothing seeds a starter menu; a first bill created now has an empty menu picker
  ("No items available").
- **Automation Candidate:** Yes.

### TC-ONB-004 — A Day auto-opens on first run with zero float and no prompt
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional
- **Preconditions:** BL-0 fresh install, first-run PIN done.
- **Steps:** 1. Immediately after unlocking, create a bill (Order → `+`) OR inspect the DB.
- **Expected Result:** A `shifts` row exists with `status=OPEN`, `openingFloat=0`, `closedAt=null`,
  `openedBy=<username>`. No opening-float dialog was ever shown. New bills attach to this shift's id.
- **Edge Case Notes:** `EnsureDayOpenUseCase` runs in `AppViewModel.init` (before the user does anything).
- **Automation Candidate:** Yes (DB assertion) / No (dialog-absence is manual).

### TC-ONB-005 — First launch with Firebase unreachable still completes onboarding (offline)
- **Priority:** High | **Severity:** Critical | **Type:** Offline / Recovery
- **Preconditions:** E2 airplane mode, BL-0 fresh install.
- **Steps:** 1. With no network, launch and register a PIN. 2. Open Payment Methods and Order.
- **Expected Result:** Registration, seeding, and day-open all succeed locally. Anonymous sign-in fails
  silently; version gate resolves to **Allowed** (network offline → skip). App is fully usable. Writes are
  `PENDING` (see Sync suite).
- **Edge Case Notes:** NFR-OFFLINE — app must function from first launch even if internet is never available.
- **Automation Candidate:** No (network toggling).

### TC-ONB-006 — First launch online performs anonymous sign-in and starts sync
- **Priority:** Medium | **Severity:** Major | **Type:** Sync
- **Preconditions:** E1 online, Firebase configured (Anonymous enabled), BL-0.
- **Steps:** 1. Launch, register PIN. 2. Create a bill and add an item. 3. Watch the Firebase RTDB console `/bills`, `/orderItems`.
- **Expected Result:** Within a short settle window the bill and order item appear in RTDB (proving anonymous
  auth + sync started). The seeded payment methods may also appear under `/paymentMethods` once any write flushes.
- **Edge Case Notes:** `syncCoordinator.start()` runs after `ensureSignedIn()`.
- **Automation Candidate:** No (external console check).

### TC-ONB-007 — Second device first-run does not duplicate payment methods after sync
- **Priority:** High | **Severity:** Critical | **Type:** Multi-device / Data integrity
- **Preconditions:** E3 two devices, same Firebase project; Device A already seeded and synced.
- **Steps:** 1. Fresh-install on Device B, register a PIN online. 2. Let it sync. 3. Open Payment Methods on B.
- **Expected Result:** Device B shows exactly **5** methods (the fixed IDs collide with A's, so inbound sync
  merges rather than duplicates). No `Tunai ×2` etc.
- **Edge Case Notes:** Directly validates the fixed-UUID seeding decision (arch_decisions Q7 / gap R-8).
- **Automation Candidate:** No (2 devices).

### TC-ONB-008 — App restart mid-onboarding (kill during PIN entry) leaves a clean state
- **Priority:** Medium | **Severity:** Major | **Type:** Recovery
- **Preconditions:** BL-0.
- **Steps:** 1. Launch to the setup screen. 2. Type a username but do **not** submit. 3. Force-stop the app. 4. Relaunch.
- **Expected Result:** Setup screen again (still unregistered); no partial credential stored; seeding already
  ran once so payment methods exist. No crash.
- **Edge Case Notes:** Seeding runs in `AppViewModel.init` regardless of registration, so 5 methods can exist
  before a PIN is set — acceptable.
- **Automation Candidate:** No.
