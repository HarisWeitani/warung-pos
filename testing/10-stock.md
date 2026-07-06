# 10 — Stock Items & Batches

**Backing code:** `feature/stock/StockScreen.kt`, `StockViewModel.kt`, `feature/stock/StockBatchScreen.kt`,
`StockBatchViewModel.kt`, `domain/usecase/stock/UpsertStockItemUseCase.kt`, `ReceiveStockBatchUseCase.kt`,
`DeductStockForBillUseCase.kt`, `data/repository/StockRepositoryImpl.kt`, `data/local/dao/StockDao.kt`,
`feature/more/MoreViewModel.kt` (low-stock badge via `observeLowStock`).

**Behaviour (verified):** More → **Stock** lists stock items (name, unit, current qty, reorder point) with an
Add sheet (name, unit, reorder point). More → **Stock Batches** records incoming stock (stock item, qty, cost),
which increases `currentQuantity` and sets `lastCostPrice`. Low-stock items (`currentQty ≤ lowStockThreshold`)
drive a **badge on the More → Stock** item and an indicator on the Stock screen. Selling items that have a
recipe deducts stock on payment (see TC-PAY-014).

> These are Phase-2 features that the PRD lists as "out of MVP" but they are **shipped and reachable** (gap D-15).

Baseline: BL-2, no stock unless a case adds it.

---

### TC-STK-001 — Create a stock item
- **Priority:** High | **Severity:** Major | **Type:** CRUD
- **Test Data:** name `Rice`, unit `kg`, reorder point `5`.
- **Steps:** 1. More → Stock → Add. 2. Name `Rice`. 3. Unit `kg`. 4. Reorder point `5`. 5. Save.
- **Expected Result:** `Rice` appears with currentQty `0` (until a batch is received), reorder point 5. Persists
  PENDING.
- **Edge Case Notes:** New item starts at qty 0, which is ≤ threshold 5 → it should immediately count as low
  stock (verify badge, TC-STK-020).
- **Automation Candidate:** Yes.

### TC-STK-002 — Edit a stock item (name/unit/reorder point)
- **Priority:** Medium | **Severity:** Minor | **Type:** CRUD
- **Steps:** 1. Tap `Rice`. 2. Change reorder point → `3`. 3. Save.
- **Expected Result:** Updated value persists; low-stock evaluation uses the new threshold.
- **Automation Candidate:** Yes.

### TC-STK-003 — Reorder point / qty inputs accept decimals appropriately
- **Priority:** Medium | **Severity:** Minor | **Type:** Boundary / Input
- **Steps:** 1. Add a stock item with reorder point `2.5`.
- **Expected Result:** Accepts a decimal (currentQuantity/threshold are `Double`). Verify the input filter allows
  `.` and a single decimal point; reject malformed like `2.5.1`.
- **Edge Case Notes:** Confirm the field filters non-numeric like the opname counted-qty field.
- **Automation Candidate:** Yes.

### TC-STK-010 — Receive a stock batch increases current quantity and sets last cost
- **Priority:** High | **Severity:** Major | **Type:** Functional / Money
- **Preconditions:** `Rice` exists (qty 0).
- **Test Data:** stock item `Rice`, qty `10`, cost `120000`.
- **Steps:** 1. More → Stock Batches → Add. 2. Choose `Rice`. 3. Qty `10`. 4. Cost `120000`. 5. Save.
- **Expected Result:** `Rice.currentQuantity` becomes `0 + 10 = 10` (`ReceiveStockBatchUseCase` **increments**
  `item.currentQty + batch.qty`). A StockBatch row persists (qty 10, cost 120000, purchaseDate now).
- **Edge Case Notes:** The use case does **not** update `StockItem.lastCostPrice` (it only saves the batch and
  sets currentQty). Verify whether `lastCostPrice` is updated anywhere; if it stays at its old value, the opname
  cost-impact and any COGS basis use a stale cost — log a Major gap. Qty ≤ 0 and negative cost are rejected
  (`Quantity must be greater than 0` / `Cost cannot be negative`).
- **Automation Candidate:** Yes.

### TC-STK-011 — Receive a second batch adds to existing quantity
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary
- **Preconditions:** `Rice` qty 10.
- **Steps:** 1. Receive another `Rice` batch qty `5`, cost `70000`.
- **Expected Result:** `Rice.currentQuantity = 15`; `lastCostPrice` updated to the latest batch's unit basis per
  the use case. Two batch rows exist.
- **Automation Candidate:** Yes.

### TC-STK-012 — Batch with zero/blank qty or negative cost is rejected
- **Priority:** Medium | **Severity:** Minor | **Type:** Negative / Validation
- **Steps:** 1. Attempt to save a batch with qty blank/0. 2. Attempt to save a batch with a negative cost (if enterable).
- **Expected Result:** `ReceiveStockBatchUseCase` rejects qty ≤ 0 (`Quantity must be greater than 0`) and cost
  < 0 (`Cost cannot be negative`). No batch recorded; current qty unchanged. Verify the VM surfaces the error
  (silent failure = Minor UX defect).
- **Automation Candidate:** Yes.

### TC-STK-013 — Stock deducts on sale of a recipe item
- **Priority:** High | **Severity:** Major | **Type:** Cross-feature
- **Preconditions:** `Rice` qty 15; `Nasi Goreng` recipe uses 0.2 kg Rice.
- **Steps:** 1. Sell (pay) `Nasi Goreng` ×5. 2. Check `Rice`.
- **Expected Result:** `Rice.currentQuantity = 15 − (0.2×5) = 14`. (See TC-PAY-014.) Items without recipes don't
  affect stock.
- **Automation Candidate:** Yes.

### TC-STK-014 — Selling more than in stock drives quantity negative (probe)
- **Priority:** Medium | **Severity:** Major | **Type:** Boundary / Negative
- **Preconditions:** `Rice` qty 0.3; `Nasi Goreng` uses 0.2/serving.
- **Steps:** 1. Sell `Nasi Goreng` ×3 (needs 0.6). 2. Check `Rice`.
- **Expected Result:** `Rice` becomes `0.3 − 0.6 = -0.3` (deduction is unconditional; no stock-availability
  block on sale). Document that negative stock is allowed — this is expected given there is no block, but flag
  whether the app should warn. No crash.
- **Edge Case Notes:** Confirms there is no "insufficient stock" guard on payment.
- **Automation Candidate:** Yes.

### TC-STK-020 — Low-stock badge on More reflects items at/below threshold
- **Priority:** High | **Severity:** Major | **Type:** Functional / Indicator
- **Preconditions:** `Rice` threshold 5, currentQty 3 (below); `Oil` threshold 2, qty 10 (above).
- **Steps:** 1. Go to the **More** tab. 2. Observe the **Stock** row badge.
- **Expected Result:** The Stock row shows a numeric **badge = 1** (only `Rice` is low). Adjust quantities and
  confirm the badge count updates reactively (`observeLowStock().size`).
- **Edge Case Notes:** Boundary: `currentQty == threshold` counts as low (≤). Test qty exactly 5 vs 5.0001.
- **Automation Candidate:** Yes.

### TC-STK-021 — Badge clears when stock is replenished above threshold
- **Priority:** Medium | **Severity:** Minor | **Type:** Reactive
- **Preconditions:** `Rice` low (badge 1).
- **Steps:** 1. Receive a `Rice` batch to bring qty above 5. 2. Return to More.
- **Expected Result:** Badge count decreases to 0 (badge hidden when count 0).
- **Automation Candidate:** Yes.

### TC-STK-030 — Stock changes sync across devices atomically
- **Priority:** Medium | **Severity:** Major | **Type:** Sync
- **Preconditions:** E3 two devices; `Rice` synced.
- **Steps:** 1. Device A sells a `Rice` recipe item; Device B receives a `Rice` batch (near-simultaneously). 2. Wait for sync.
- **Expected Result:** Final `Rice` quantity reflects **both** the deduction and the addition (per
  `ServerValue.increment`-style atomic updates, FR-SYNC-6). Neither device's change is lost to LWW overwrite.
- **Edge Case Notes:** This validates that stock qty uses atomic increments, not last-write-wins on the whole
  field. If one change is lost, log Critical.
- **Automation Candidate:** No (2 devices).

### TC-STK-031 — Empty stock screen state
- **Priority:** Low | **Severity:** Trivial | **Type:** Empty state
- **Preconditions:** No stock items.
- **Steps:** 1. Open Stock.
- **Expected Result:** An empty-state/empty list; Add sheet works. No crash.
- **Automation Candidate:** Yes.
