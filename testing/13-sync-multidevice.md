# 13 — Sync, Offline, Reconnection & Multi-Device

**Backing code:** `data/remote/sync/SyncCoordinator.kt`, `SyncWorker.kt`, `RtdbListener.kt`, `ConflictResolver.kt`,
`EntityMapping.kt`, `RtdbPaths.kt`, `feature/sync/SyncStatusBar.kt`, `SyncViewModel.kt`,
`data/remote/firebase/FirebaseAuthDataSource.kt`, `FirebaseRtdbDataSource.kt`, `core/common/NetworkMonitor.kt`.

**Model (verified):** Room is the single source of truth; the UI never reads RTDB. Every write sets
`syncStatus=PENDING` and calls `SyncCoordinator.notifyPendingSync()`, which enqueues a **unique**
`OneTimeWork` SyncWorker (network-constrained, exponential backoff 10s). `SyncWorker` batches all PENDING rows
across every DAO into a single multi-path RTDB write, then re-queries PENDING rows and flips them to SYNCED.
`RtdbListener` pulls remote changes and applies them via `ConflictResolver` (LWW by `updatedAt`, plus a
**status-regression guard**: OPEN→PAID→VOID can never move backward). Anonymous auth (`ensureSignedIn`) gates
RTDB access.

**Sync status bar (verified):** shows **OFFLINE** (red, `Offline — data tersimpan lokal / Offline — saved
locally`) when the network is down; **SYNCING** (amber, `Menyinkronkan data... / Syncing...`) when online and a
sync job is RUNNING/ENQUEUED; otherwise hidden. It does **not** display a pending-record count (minor divergence
from FR-SYNC-4/9).

**Known code risks to probe:** `SyncWorker.markSynced()` re-queries PENDING after the push — a row written
between the push and the flip can be marked SYNCED without being sent (gap F-1). Inbound reflection
deserialization can null-out on schema/JSON mismatch (gap F-2).

Environments: E1 (online), E2 (airplane mode), E3 (two devices, same Firebase project).

---

## Offline capability & status bar

### TC-SYNC-001 — Full POS works offline
- **Priority:** Critical | **Severity:** Blocker | **Type:** Offline
- **Preconditions:** E2 airplane mode; registered; menu seeded (BL-2).
- **Steps:** 1. With no network, create a bill, add items, void an item, pay cash, log an expense, close the day.
- **Expected Result:** Every operation completes instantly against Room. No operation is blocked by the lack of
  network. All new/edited rows are `syncStatus=PENDING`.
- **Automation Candidate:** No (network toggling).

### TC-SYNC-002 — Offline status bar visible while airplane mode is on
- **Priority:** High | **Severity:** Major | **Type:** UI / Indicator
- **Preconditions:** E2.
- **Steps:** 1. Enable airplane mode with the app foregrounded.
- **Expected Result:** A red bar appears at the top: `Offline — data tersimpan lokal / Offline — saved locally`.
  It never blocks interaction.
- **Edge Case Notes:** `NetworkMonitor.isOnline` drives this; verify it flips within a few seconds of toggling.
- **Automation Candidate:** No.

### TC-SYNC-003 — Syncing status bar appears while a sync job runs (online)
- **Priority:** Medium | **Severity:** Minor | **Type:** UI / Indicator
- **Preconditions:** E1 online; make a batch of pending writes offline first, then go online.
- **Steps:** 1. Create several bills/items offline. 2. Turn network on. 3. Watch the top bar.
- **Expected Result:** An amber bar `Menyinkronkan data... / Syncing...` shows while the SyncWorker is
  RUNNING/ENQUEUED, then disappears (HIDDEN) once sync settles and the device is online with no active job.
- **Edge Case Notes:** No pending count is shown (FR-SYNC-9 asked for "Syncing N records…" — log Minor gap).
- **Automation Candidate:** No.

### TC-SYNC-004 — Status bar hidden when online and idle
- **Priority:** Low | **Severity:** Trivial | **Type:** UI
- **Preconditions:** E1 online, everything synced.
- **Steps:** 1. Observe the top of the screen.
- **Expected Result:** No sync bar is shown (SyncBarState.HIDDEN).
- **Automation Candidate:** No.

---

## Outbound sync & reconnection

### TC-SYNC-010 — Pending writes flush to RTDB on reconnect
- **Priority:** Critical | **Severity:** Critical | **Type:** Reconnection / Data integrity
- **Preconditions:** E2 → E1. Create offline: 2 bills, 3 order items, 1 payment, 1 expense.
- **Steps:** 1. Verify local rows are PENDING. 2. Re-enable network. 3. Wait for the sync job to run.
- **Expected Result:** All rows appear at their RTDB paths (`/bills`, `/orderItems`, `/payments`, `/expenses`);
  local rows flip to `syncStatus=SYNCED`. No row remains stuck PENDING once online for a reasonable window.
- **Edge Case Notes:** Verify order-independence (a child order item whose parent bill also just synced must not
  break). If any row is stuck PENDING after settling, investigate the `markSynced` race (gap F-1).
- **Automation Candidate:** No (console + timing).

### TC-SYNC-011 — Rapid writes during an active sync are not lost (markSynced race, gap F-1)
- **Priority:** High | **Severity:** Critical | **Type:** Data integrity / Race
- **Preconditions:** E1 online.
- **Steps:** 1. Trigger a large sync (many pending rows). 2. **While** the amber Syncing bar is visible, quickly
  create a new bill + add an item. 3. Let sync settle. 4. Inspect that new bill/item's `syncStatus` and its
  presence in RTDB.
- **Expected Result (target):** The row created mid-sync ends up in RTDB and SYNCED. **Risk:** if `markSynced`
  flips it to SYNCED without pushing it (because it was queried after `writeMulti`), the row is SYNCED locally
  but **absent** in RTDB → it will not resync until edited again. If observed, log a **Critical** silent
  data-loss defect with repro.
- **Edge Case Notes:** The most important robustness probe in this suite. Verify by cross-checking Room SYNCED
  rows against the RTDB console.
- **Automation Candidate:** No.

### TC-SYNC-012 — Sync retries with backoff after a transient failure
- **Priority:** Medium | **Severity:** Major | **Type:** Reliability
- **Preconditions:** E1; ability to briefly drop connectivity mid-sync.
- **Steps:** 1. Start a sync. 2. Kill connectivity mid-flush, then restore it.
- **Expected Result:** The worker returns `retry()` (up to 3 attempts) and eventually completes on reconnect;
  no data lost; rows end SYNCED. App remains usable throughout (NFR-RELIABILITY).
- **Automation Candidate:** No.

### TC-SYNC-013 — Version gate / sync never blocks offline usage
- **Priority:** High | **Severity:** Critical | **Type:** Offline
- **Preconditions:** E2 fresh cold start offline.
- **Steps:** 1. Cold-start with no network. 2. Use the app.
- **Expected Result:** No blocking screens from sync/version checks; version gate resolves to Allowed on
  offline; anonymous sign-in failure is silent.
- **Automation Candidate:** No.

---

## Inbound sync & propagation (two devices)

### TC-SYNC-020 — New bill on Device A appears on Device B within seconds
- **Priority:** High | **Severity:** Critical | **Type:** Multi-device
- **Preconditions:** E3 both online.
- **Steps:** 1. On A create a bill + item. 2. Watch B's Order list.
- **Expected Result:** Within a short settle window the bill appears on B with the same items and total.
  (`RtdbListener` → ConflictResolver ACCEPT since B has no local copy.)
- **Automation Candidate:** No.

### TC-SYNC-021 — Payment on A reflects as PAID on B (bill leaves B's open list)
- **Priority:** High | **Severity:** Critical | **Type:** Multi-device / State
- **Preconditions:** E3; a shared OPEN bill on both.
- **Steps:** 1. A pays the bill. 2. Observe B.
- **Expected Result:** B's copy becomes PAID and drops off B's Order list. The status-regression guard prevents
  B from ever reverting it to OPEN.
- **Edge Case Notes:** During A's local-commit-to-RTDB window (up to the 15-min periodic backup if the immediate
  job is delayed), B may still show OPEN (gap R-5). Record the propagation latency.
- **Automation Candidate:** No.

### TC-SYNC-022 — Concurrent same-bill item adds (append-only, both survive)
- **Priority:** High | **Severity:** Critical | **Type:** Conflict
- **Preconditions:** E3; shared OPEN bill.
- **Steps:** 1. A adds item X, B adds item Y at the same time. 2. Wait for sync.
- **Expected Result:** Both devices end with items X and Y (distinct UUIDs, no collision). The bill total on both
  converges. See TC-ORD-053.
- **Automation Candidate:** No.

### TC-SYNC-023 — Same-field conflict resolves by last-write-wins (updatedAt)
- **Priority:** Medium | **Severity:** Major | **Type:** Conflict
- **Preconditions:** E3; a shared menu item.
- **Steps:** 1. A sets `Es Teh` price 5000 at T1; B sets it 6000 at T2 (>T1). 2. Sync both.
- **Expected Result:** Both converge to the later write (6000). The earlier write is discarded by
  `ConflictResolver` (incoming.updatedAt must exceed existing to ACCEPT).
- **Edge Case Notes:** Millisecond ties go to REJECT (local wins) — see gap R-7; extremely unlikely at scale.
- **Automation Candidate:** No.

### TC-SYNC-024 — Stale device cannot reopen a PAID bill (status regression guard)
- **Priority:** High | **Severity:** Critical | **Type:** Conflict / Data integrity
- **Preconditions:** E3; A pays a bill (PAID synced). B was offline holding the bill OPEN with a newer local
  `updatedAt` (e.g. B added an item to it just before A paid).
- **Steps:** 1. Bring B online so B pushes its OPEN-with-newer-updatedAt state.
- **Expected Result:** The bill stays **PAID** on both devices — `ConflictResolver.isStatusRegression` REJECTs
  any incoming OPEN when the existing is PAID, regardless of `updatedAt`. No reopen; payment preserved.
- **Edge Case Notes:** Also confirm B's late-added order item still appends (items are separate rows and not
  status-guarded). Verify the paid bill's total does/doesn't include B's late item — document.
- **Automation Candidate:** No.

### TC-SYNC-025 — Deleted variant/option inbound does not crash the order sheet
- **Priority:** Medium | **Severity:** Major | **Type:** Robustness / Inbound
- **Preconditions:** E3; A deletes a variant group that B currently has open in a variant sheet.
- **Steps:** 1. On B, open the variant sheet for the item. 2. On A, delete that group. 3. Let it sync to B.
- **Expected Result:** B does not crash; the sheet either updates or the stale selection is handled gracefully
  on confirm. Historical order items keep their JSON snapshot.
- **Automation Candidate:** No.

---

## Multi-device lifecycle & data integrity

### TC-SYNC-030 — New device restores data from RTDB after login
- **Priority:** High | **Severity:** Critical | **Type:** Multi-device / Recovery
- **Preconditions:** E3; Device A has a populated, synced dataset. Device B is a fresh install.
- **Steps:** 1. On B, register a PIN online. 2. Wait for inbound sync.
- **Expected Result:** B pulls menu, bills, payments, stock, etc. from RTDB into its Room. No duplication (fixed
  payment-method IDs prevent method dupes). B shows the same data as A.
- **Edge Case Notes:** Confirm the Day/shift state: B may open its **own** local day; verify there are not two
  conflicting OPEN days across devices (see TC-SYNC-050).
- **Automation Candidate:** No.

### TC-SYNC-031 — Reinstall on the same device recovers data from RTDB (no local backup)
- **Priority:** High | **Severity:** Critical | **Type:** Recovery
- **Preconditions:** Device synced online. Then uninstall + reinstall.
- **Steps:** 1. Uninstall. 2. Reinstall. 3. Register PIN online. 4. Wait for sync.
- **Expected Result:** Historical bills/menu/etc. reappear from RTDB. **Caveat:** Z-reports have no sync-retry
  metadata (R-2) — verify whether past Z-reports are restored (they may only be recoverable via the one-time
  `/zReports` write). If a closed day's Z-report is missing after reinstall, log Major.
- **Automation Candidate:** No.

### TC-SYNC-032 — No data loss after force-close during a pending sync
- **Priority:** High | **Severity:** Critical | **Type:** Recovery / Data integrity
- **Preconditions:** Pending writes queued (offline), app foregrounded.
- **Steps:** 1. Create offline data. 2. Force-stop the app before reconnecting. 3. Relaunch, unlock, go online.
- **Expected Result:** The PENDING rows persisted in Room survive the force-stop; on reconnect the SyncWorker
  flushes them. Nothing lost, nothing duplicated.
- **Automation Candidate:** No.

### TC-SYNC-033 — Device reboot preserves local data and resumes sync
- **Priority:** Medium | **Severity:** Major | **Type:** Recovery
- **Preconditions:** Data present, some PENDING.
- **Steps:** 1. Reboot the device. 2. Launch, unlock, connect.
- **Expected Result:** All Room data intact; PENDING rows flush after launch (WorkManager persists across
  reboot with the network constraint). No loss/dup.
- **Automation Candidate:** No.

### TC-SYNC-040 — Long offline period then bulk reconnect flushes in order
- **Priority:** Medium | **Severity:** Major | **Type:** Reconnection / Performance-scenario
- **Preconditions:** E2 for an extended session generating many records (e.g. a full simulated day of 100+ bills).
- **Steps:** 1. Operate offline for the whole "day". 2. Reconnect. 3. Watch sync complete.
- **Expected Result:** All records flush without ANR/crash; the app stays responsive; final RTDB state equals
  local state; local rows all SYNCED. (Identifies the bulk-flush scenario; not a benchmark.)
- **Automation Candidate:** No.

### TC-SYNC-050 — Two devices both open a day offline (split-brain, gap R-1)
- **Priority:** Medium | **Severity:** Critical | **Type:** Conflict / Edge
- **Preconditions:** E3 both offline, **no** open day on either (e.g. both fresh, or both just closed).
- **Steps:** 1. On A (offline) create a bill → A auto-opens Day-A. 2. On B (offline) create a bill → B
  auto-opens Day-B. 3. Bring both online. 4. Observe.
- **Expected Result:** Two different OPEN shift rows now exist in RTDB (Day-A and Day-B). There is **no**
  server-side single-open-day enforcement (`/appConfig/openDayId` guard is not implemented in this build).
  Document the resulting state: each device keeps its own OPEN day; bills attach to whichever shift was open on
  the creating device. Confirm that closing one day does not orphan the other's bills, and that reports don't
  double-count. This is a known high-risk edge — capture behaviour precisely for the product owner.
- **Edge Case Notes:** The arch decision assumed "only the owner opens shifts from one device," so this is
  outside the intended operating envelope but realistic with 2 devices.
- **Automation Candidate:** No.

### TC-SYNC-051 — Anonymous auth disabled in Firebase → local-only, no sync
- **Priority:** Medium | **Severity:** Major | **Type:** Config / Negative
- **Preconditions:** Firebase project with Anonymous sign-in **disabled**.
- **Steps:** 1. Launch online. 2. Create data. 3. Check RTDB.
- **Expected Result:** App works fully locally; `ensureSignedIn` fails; writes stay PENDING and never reach
  RTDB; version gate falls back to Allowed. No crash, no user-facing auth error. (Documented in
  `docs/firebase-setup.md`.)
- **Automation Candidate:** No.

### TC-SYNC-052 — RTDB security rules deny unauthenticated access
- **Priority:** Low | **Severity:** Major | **Type:** Security
- **Preconditions:** Rules deployed (`firebase/database.rules.json`).
- **Steps:** 1. `curl "https://<project>-default-rtdb.<region>.firebasedatabase.app/bills.json"` (unauthenticated).
- **Expected Result:** `{"error":"Permission denied"}`. Authenticated (anonymous) app access still works.
- **Automation Candidate:** No.
