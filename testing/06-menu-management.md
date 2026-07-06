# 06 — Menu Management (Items, Categories, Variants, Recipes, Sold-out, Hide)

**Backing code:** `feature/menu/MenuManagementScreen.kt`, `MenuManagementViewModel.kt`,
`feature/menu/MenuItemEditScreen.kt`, `MenuItemEditViewModel.kt`,
`feature/menu/component/VariantGroupEditor.kt`, `IngredientEditor.kt`,
`domain/usecase/menu/UpsertMenuItemUseCase.kt`, `ToggleSoldOutUseCase.kt`, `HideMenuItemUseCase.kt`,
`data/repository/MenuRepositoryImpl.kt`, `MenuItemDao.kt`.

**Behaviour (verified):** Menu Management (More → Menu Management, owner-only = always visible) lists items
grouped by category (plus an `Uncategorized` group). Each row: name, price, a **sold-out control**
(a **Switch** when available; an **"Sold Out"** AssistChip when sold-out — tap either to toggle), and a **Hide**
(eye-off) icon. Row tap → **Menu Item Edit**. FAB `+` → new item edit.

**Item Edit** lets you set name, category, price (digits only), manage **variant groups + options**, and manage
**recipe ingredients** (links to stock items) — ingredient/variant editing requires the item to be **saved
first** (they act on `itemId`). Save uses `UpsertMenuItemUseCase`.

**Important observed limitations (verify + flag):**
- Management list filters `isAvailable` → **hidden items disappear from Menu Management too**, and there is **no
  "show hidden"/unhide** UI. Hiding appears to be effectively permanent from the app (gap — Major).
- No dedicated **category CRUD** screen is wired in the nav graph; categories are chosen from an existing list
  in item edit. Confirm how categories get created (see TC-MENU-030).

Baseline: BL-1 (empty menu) for creation cases; BL-2 for edits.

---

## Create / edit items

### TC-MENU-001 — Create a new menu item (no variants)
- **Priority:** Critical | **Severity:** Critical | **Type:** Functional / CRUD
- **Preconditions:** ≥1 category exists (see TC-MENU-030 for category creation dependency).
- **Test Data:** name `Nasi Goreng`, category `Makanan`, price `15000`.
- **Steps:** 1. More → Menu Management → tap `+`. 2. Enter name `Nasi Goreng`. 3. Select category `Makanan`. 4. Enter price `15000`. 5. Tap **Save**.
- **Expected Result:** Save succeeds (`isSaved`); the item persists (`menu_items` row: basePrice 15000,
  isAvailable=1, isSoldOut=0, PENDING). Returning to Menu Management shows `Nasi Goreng` under `Makanan`.
- **Postconditions:** Item available in the order picker.
- **Automation Candidate:** Yes.

### TC-MENU-002 — Price field digits-only; blank/zero price is rejected on Save
- **Priority:** High | **Severity:** Major | **Type:** Boundary / Validation
- **Preconditions:** New item edit with a valid name + category.
- **Steps:** 1. In price type `12ab3` → observe value `123`. 2. Clear price and tap **Save**. 3. Type `0` and tap **Save**.
- **Expected Result:** Price field holds only digits. Blank price → `basePrice` becomes 0 →
  `UpsertMenuItemUseCase` rejects with `Price must be greater than 0`; the item is **not** saved and the error is
  set (`state.error`). Entering `0` is likewise rejected. Only a price ≥ 1 saves.
- **Edge Case Notes:** `UpsertMenuItemUseCase` guards `basePrice <= 0`. Verify the error is actually surfaced on
  `MenuItemEditScreen`; if the failure is silent (no visible message, Save appears to do nothing), log a Major
  UX defect.
- **Automation Candidate:** Yes.

### TC-MENU-003 — Blank name is rejected on Save
- **Priority:** Medium | **Severity:** Major | **Type:** Negative / Validation
- **Preconditions:** New item edit.
- **Steps:** 1. Leave name blank. 2. Enter a valid price (e.g. `5000`) and a category. 3. Tap **Save**.
- **Expected Result:** `UpsertMenuItemUseCase` rejects with `Name must not be blank`; no item is created. The
  error is set on state. (Whitespace-only names are trimmed to blank in `save()` and also rejected.)
- **Edge Case Notes:** Confirm the error message is visible to the user on the edit screen; silent failure = Major.
- **Automation Candidate:** Yes.

### TC-MENU-004 — Edit an existing item's price does not affect historical order lines
- **Priority:** High | **Severity:** Critical | **Type:** Data integrity
- **Preconditions:** BL-2; an OPEN bill already contains `Nasi Goreng` at Rp 15.000.
- **Steps:** 1. Edit `Nasi Goreng` price → `18000`, Save. 2. Reopen the existing bill.
- **Expected Result:** Existing order line still Rp 15.000 (snapshot). New additions use Rp 18.000. (Mirror of
  TC-ORD-041.)
- **Automation Candidate:** Yes.

### TC-MENU-005 — Rename an item; new orders reflect the new name
- **Priority:** Medium | **Severity:** Minor | **Type:** CRUD
- **Preconditions:** BL-2.
- **Steps:** 1. Edit `Es Teh` name → `Iced Tea`, Save. 2. Order picker in a new bill.
- **Expected Result:** Picker lists `Iced Tea`; existing snapshot lines keep `Es Teh`.
- **Automation Candidate:** Yes.

### TC-MENU-006 — Re-open an item edit shows persisted variants and ingredients
- **Priority:** Medium | **Severity:** Major | **Type:** State
- **Preconditions:** An item with 1 variant group + 1 ingredient (created below).
- **Steps:** 1. Open the item in edit. 2. Observe variant groups and ingredients load.
- **Expected Result:** `loadVariantGroups()`/`loadIngredients()` populate the previously saved data.
- **Automation Candidate:** Yes.

---

## Variant groups & options

### TC-MENU-010 — Add a variant group to a saved item
- **Priority:** High | **Severity:** Major | **Type:** Functional / CRUD
- **Preconditions:** A saved item (`itemId` exists).
- **Steps:** 1. Open the item edit. 2. Tap **Add variant group** (adds "New Group", SINGLE, not required). 3. Rename it to `Spice`, set selection type SINGLE, toggle **required** on.
- **Expected Result:** A `Spice` group persists (`variant_groups`: selectionType SINGLE, isRequired true).
- **Edge Case Notes:** `addVariantGroup` requires a non-null itemId → the item must be saved first. If the item
  is brand-new and unsaved, adding a group is a no-op (return@launch) — verify the Add control is disabled or
  the item is auto-saved first.
- **Automation Candidate:** Yes.

### TC-MENU-011 — Add options with price deltas (zero, positive, negative)
- **Priority:** High | **Severity:** Major | **Type:** Boundary / Money
- **Preconditions:** Item with a `Spice` group.
- **Steps:** 1. Add option `Mild` delta `0`. 2. Add option `Hot` delta `2000`. 3. Add option `Discount` delta `-1000` (if the editor allows negatives).
- **Expected Result:** Options persist with `priceDelta` 0, 2000, and (if allowed) -1000. Verify whether the
  option editor accepts a negative delta (PRD FR-MENU-5 allows negative). If the input strips the minus sign,
  log a Minor gap.
- **Edge Case Notes:** Negative deltas affect the unit price in the order sheet.
- **Automation Candidate:** Yes.

### TC-MENU-012 — Required group is enforced in the order sheet
- **Priority:** High | **Severity:** Major | **Type:** Cross-feature
- **Preconditions:** Item with a required SINGLE group.
- **Steps:** 1. In a bill, tap the item. 2. Try to add without selecting the required option.
- **Expected Result:** Add is blocked until the required option is chosen (see TC-ORD-015).
- **Automation Candidate:** Yes.

### TC-MENU-013 — Delete a variant group / option
- **Priority:** Medium | **Severity:** Minor | **Type:** CRUD
- **Preconditions:** Item with a group + options.
- **Steps:** 1. Delete one option. 2. Delete the whole group.
- **Expected Result:** Deleted option/group disappears and no longer offered in the order sheet. (Hard delete
  via `deleteVariantOption`/`deleteVariantGroup`.)
- **Edge Case Notes:** Deleting a group used by historical orders does not alter their snapshots
  (`selectedVariants` is a JSON snapshot). Verify no crash rendering an old order whose variant was deleted.
- **Automation Candidate:** Yes.

---

## Recipes (ingredients)

### TC-MENU-020 — Link a stock ingredient to a menu item
- **Priority:** High | **Severity:** Major | **Type:** Functional / Stock
- **Preconditions:** A saved item; ≥1 stock item exists (e.g. `Rice`).
- **Steps:** 1. Open item edit. 2. Tap **Add ingredient**. 3. Choose `Rice`, set qty-per-serving `0.2`. 4. Save.
- **Expected Result:** A `MenuItemIngredient(menuItemId, Rice, 0.2)` persists. On payment this deducts stock
  (TC-PAY-014). Items without ingredients never deduct.
- **Edge Case Notes:** `addIngredient` picks the first stock item not already linked; requires a saved item and
  ≥1 stock item, else no-op.
- **Automation Candidate:** Yes.

### TC-MENU-021 — Change an ingredient's stock item reassigns cleanly (no orphan)
- **Priority:** Medium | **Severity:** Minor | **Type:** CRUD
- **Preconditions:** Item with ingredient `Rice`.
- **Steps:** 1. Edit the ingredient to point at `Oil` instead.
- **Expected Result:** The old `Rice` link is deleted and an `Oil` link saved (`updateIngredient` deletes the
  old stockItemId when changed). Exactly one ingredient row remains.
- **Automation Candidate:** Yes.

### TC-MENU-022 — Delete an ingredient
- **Priority:** Low | **Severity:** Minor | **Type:** CRUD
- **Steps:** 1. Delete the ingredient from an item.
- **Expected Result:** Row removed; that item no longer deducts stock on sale.
- **Automation Candidate:** Yes.

---

## Sold-out & hide

### TC-MENU-030 — No in-app category creation (confirmed gap; items land Uncategorized)
- **Priority:** High | **Severity:** Major | **Type:** Negative / Gap verification
- **Preconditions:** BL-0/BL-1 fresh install (no categories seeded).
- **Steps:** 1. Open Menu Management. 2. Tap `+` to add an item. 3. Inspect the **category** selector. 4. Look
  anywhere in the app (More/Settings/Menu Management) for a "New Category" / category-management action.
- **Expected Result:** There is **no UI to create a category** (`MenuRepository.saveCategory` exists but is not
  wired to any screen). The item-edit category selector is **empty** on a fresh install, so a new item is saved
  with `categoryId=null` and appears under the **`Uncategorized`** group in Menu Management (and in one implicit
  group in the order picker). This **blocks FR-MENU-1** (category create/rename/reorder/soft-delete) — log a
  Major gap. To test category-based flows, categories must be seeded via RTDB/DB (see baseline note below).
- **Edge Case Notes:** `MenuManagementViewModel` groups null/unknown categories under `UNCATEGORIZED_ID`. This
  means the `BL-2` baseline (named categories `Makanan`/`Minuman`) is **not reproducible through the app UI** —
  seed categories directly (RTDB `/menuCategories` or Room) before running category-dependent order/menu cases.
- **Automation Candidate:** No (exploratory).

### TC-MENU-031 — Toggle an item Sold Out
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** BL-2, item `Es Teh` available (Switch on).
- **Steps:** 1. In Menu Management, toggle the `Es Teh` Switch off.
- **Expected Result:** The row now shows a **"Sold Out"** AssistChip instead of the Switch; `isSoldOut=1`,
  PENDING. (Reminder: sold-out is NOT enforced in the order picker — TC-ORD-030.)
- **Automation Candidate:** Yes.

### TC-MENU-032 — Revert Sold Out via the chip
- **Priority:** Medium | **Severity:** Minor | **Type:** Functional
- **Preconditions:** `Es Teh` sold out.
- **Steps:** 1. Tap the **"Sold Out"** chip on `Es Teh`.
- **Expected Result:** Reverts to available (Switch shown, `isSoldOut=0`).
- **Automation Candidate:** Yes.

### TC-MENU-033 — Sold-out does NOT auto-reset (manual only)
- **Priority:** Medium | **Severity:** Major | **Type:** Business rule
- **Preconditions:** `Es Teh` sold out during an OPEN day.
- **Steps:** 1. Close the day (manual) or trigger a day rollover. 2. Reopen Menu Management.
- **Expected Result:** `Es Teh` is **still sold out** after the new day starts — there is **no** "Reset all
  sold-out?" prompt (gap D-5; `ResetSoldOutItemsUseCase` is unwired). Operator must revert manually.
- **Edge Case Notes:** Contradicts FR-DAY-1a. Log Medium gap.
- **Automation Candidate:** Yes.

### TC-MENU-034 — Hide an item removes it from the order picker
- **Priority:** High | **Severity:** Major | **Type:** Functional
- **Preconditions:** BL-2, item `Nasi Goreng` not in any open bill.
- **Steps:** 1. In Menu Management tap the eye-off icon on `Nasi Goreng`. 2. Dialog `Hide "Nasi Goreng"?` with text `This item will no longer appear on the order screen.` → tap **Hide**.
- **Expected Result:** `isAvailable=0`. `Nasi Goreng` disappears from the order picker (TC-ORD-031).
- **Automation Candidate:** Yes.

### TC-MENU-035 — Hide warns when the item is in an open bill
- **Priority:** Medium | **Severity:** Major | **Type:** Functional / Edge
- **Preconditions:** An OPEN bill contains `Es Teh`.
- **Steps:** 1. Menu Management → eye-off on `Es Teh`. 2. Read the dialog.
- **Expected Result:** Dialog text warns: `This item is in one or more open bills. Hiding it will remove it from
  the order screen but won't affect those bills.` Confirming hides it from the picker but the existing bill's
  line is untouched.
- **Edge Case Notes:** `requestHide` checks open bills for the item to pick the warning text.
- **Automation Candidate:** Yes.

### TC-MENU-036 — Hidden item cannot be seen/un-hidden from Menu Management (gap verification)
- **Priority:** High | **Severity:** Major | **Type:** Negative / Gap
- **Preconditions:** `Nasi Goreng` hidden (TC-MENU-034).
- **Steps:** 1. Reopen Menu Management. 2. Look for `Nasi Goreng`.
- **Expected Result (current):** `Nasi Goreng` is **absent** from Menu Management (list filters `isAvailable`).
  There is **no visible way to unhide it** in the app. Confirm whether any "show hidden" toggle or edit path
  exists; if not, log a Major usability/data gap (hidden items are effectively lost from the UI, and a
  soft-deleted item cannot be restored without DB access).
- **Automation Candidate:** No (exploratory).

### TC-MENU-037 — Cancel the hide dialog
- **Priority:** Low | **Severity:** Trivial | **Type:** Cancel-midway
- **Steps:** 1. Open the hide dialog. 2. Tap **Cancel**.
- **Expected Result:** Item stays available; dialog closes.
- **Automation Candidate:** Yes.

### TC-MENU-038 — Menu edits sync across devices
- **Priority:** Medium | **Severity:** Major | **Type:** Sync
- **Preconditions:** E3 two devices online.
- **Steps:** 1. On A create item `Soto` Rp 12.000. 2. Wait for sync. 3. On B open the order picker.
- **Expected Result:** `Soto` appears on B; price and availability match. A later price edit on A propagates to
  B by LWW.
- **Automation Candidate:** No (2 devices).
