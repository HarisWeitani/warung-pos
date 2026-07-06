# 14 — Settings, Language & Internationalisation

**Backing code:** `feature/settings/SettingsScreen.kt`, `LanguageSettingsScreen.kt`/`LanguageSettingsViewModel.kt`,
`AboutScreen.kt`, `feature/more/MoreScreen.kt`, `core/common/AppPreferences.kt` (language persisted in
EncryptedSharedPreferences `app_prefs`), `WarungPosApp.kt` (applies locale via `ContextThemeWrapper`),
`res/values/strings.xml`, `res/values-id/strings.xml`.

**Behaviour (verified):** Language is chosen in **More → Language** (Bahasa Indonesia / English), stored **per
device** (not synced), default **`id`**. `WarungPosApp` re-wraps the context with the chosen locale so
string-resource lookups switch **without restart**. **However, almost all screen text is hardcoded English**
(only bottom-nav labels, the version-gate screen, and a handful of keys use `strings.xml`) — so switching the
language changes very little on screen (gap D-4). The sync bar shows both languages inline. About shows the app
version.

---

### TC-SET-001 — Open Language settings and see both options
- **Priority:** Medium | **Severity:** Minor | **Type:** Functional
- **Steps:** 1. More → **Language**.
- **Expected Result:** Two selectable options (Bahasa Indonesia, English); the current selection is indicated
  (default Indonesian on a fresh install).
- **Automation Candidate:** Yes.

### TC-SET-002 — Switching language updates string-resource UI immediately (no restart)
- **Priority:** Medium | **Severity:** Major | **Type:** i18n
- **Preconditions:** Currently English (or Indonesian).
- **Steps:** 1. In Language, select the other language. 2. Return to the app; observe the **bottom nav labels**.
- **Expected Result:** Bottom-nav labels switch language immediately (Order/Reports/More ↔ Indonesian
  equivalents from `values-id`). No app restart needed (`ContextThemeWrapper` re-applied on the `language` flow).
- **Edge Case Notes:** This is the *only* clearly observable change because most screens are hardcoded English.
- **Automation Candidate:** Yes.

### TC-SET-003 — Most screen text stays English regardless of language (gap D-4)
- **Priority:** Medium | **Severity:** Major | **Type:** Gap verification / i18n
- **Preconditions:** Set language to Bahasa Indonesia.
- **Steps:** 1. Visit Order ("Orders"), Bill Detail ("Order Items", "Add Items", "Pay"), Payment ("Payment",
  "Confirm Payment"), More ("Close Day", "Lock App"), Menu Management ("Menu Management").
- **Expected Result:** These strings remain **English** even with Indonesian selected, because they are
  hardcoded (not externalised). Log a Major i18n gap: the app is effectively English-only despite the toggle.
- **Automation Candidate:** Yes.

### TC-SET-004 — Language preference persists across restart
- **Priority:** Medium | **Severity:** Major | **Type:** Persistence
- **Steps:** 1. Set English. 2. Force-stop + relaunch.
- **Expected Result:** After relaunch the app is still English (persisted in `app_prefs`).
- **Automation Candidate:** Yes.

### TC-SET-005 — Language is per-device, not synced
- **Priority:** Low | **Severity:** Minor | **Type:** Multi-device
- **Preconditions:** E3 two devices.
- **Steps:** 1. Set English on A, Indonesian on B. 2. Trigger data sync.
- **Expected Result:** Each device keeps its own language; the setting is not written to RTDB (FR-I18N-2).
- **Automation Candidate:** No.

### TC-SET-006 — Monetary formatting uses Rp with no decimals
- **Priority:** Medium | **Severity:** Major | **Type:** i18n / Money
- **Preconditions:** Any amount displayed.
- **Steps:** 1. Observe amounts on Order/Payment/Reports.
- **Expected Result:** Amounts are prefixed `Rp` and show whole Rupiah (no decimal places), per
  `CurrencyFormatter` (FR-I18N-5). Confirm the exact grouping character used and whether it stays constant
  across language (it should — formatter is not language-bound in the UI text).
- **Automation Candidate:** Yes.

### TC-SET-007 — About screen shows version
- **Priority:** Low | **Severity:** Trivial | **Type:** Functional
- **Steps:** 1. More → **About**.
- **Expected Result:** Displays app name and version (e.g. `v1.0 (1)` per `BuildConfig`). Back returns to More.
- **Automation Candidate:** Yes.

### TC-SET-008 — Settings sub-screens are reachable and Back-navigable
- **Priority:** Low | **Severity:** Minor | **Type:** Navigation
- **Steps:** 1. From More open each: Payment Methods, Expense Categories, Language, About, Menu Management. 2. Back from each.
- **Expected Result:** Each opens and Back returns to More without crashing or losing the tab.
- **Automation Candidate:** Yes.

### TC-SET-009 — Rapid language toggling does not corrupt the UI
- **Priority:** Low | **Severity:** Minor | **Type:** User behaviour
- **Steps:** 1. Toggle language back and forth 6× quickly.
- **Expected Result:** No crash; final selection is applied and persisted; bottom-nav labels reflect the last
  choice.
- **Automation Candidate:** Yes.

### TC-SET-010 — Untranslated key falls back to English
- **Priority:** Low | **Severity:** Trivial | **Type:** i18n / Boundary
- **Preconditions:** Indonesian selected.
- **Steps:** 1. Inspect any string that exists in `values` but is missing/placeholder in `values-id`.
- **Expected Result:** Falls back to the English default (Android resource fallback). No `[TODO-ID:...]`
  placeholder or missing-resource crash is shown to the user.
- **Automation Candidate:** No.
